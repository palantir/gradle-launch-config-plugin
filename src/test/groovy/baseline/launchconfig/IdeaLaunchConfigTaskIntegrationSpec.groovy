package baseline.launchconfig

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import org.junit.Rule
import org.junit.rules.TestName

class IdeaLaunchConfigTaskIntegrationSpec extends IntegrationSpec {

    @Rule
    TestName testName = new TestName()
    String projectName

    def setup() {
        this.projectName = testName.methodName
                .replaceAll("'", "")
                .replaceAll(" ", "-")
    }

    def "generates launch files from a fully customized JavaExec"() {
        setup:
        writeHelloWorld("com.testing")
        String main = "com.testing.HelloWorld"
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'baseline.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main '${main}'
                args('server', 'dev/conf/server.yml')
                jvmArgs('-server', '-client')
                workingDir '/'
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("idea")

        then:
        result.success

        String launchFilename = "${projectName}.iws"
        fileExists(launchFilename)

        def xml = new XmlSlurper().parseText(file(launchFilename).text)
        def runManager = xml.component.findResult { it.@name == "RunManager" ? it : null }
        runManager != null

        def runConfig = runManager.configuration.findResult { it.@name == "${projectName}-runDev" ? it : null }
        runConfig != null

        runConfig.option.any { it.@name == "MAIN_CLASS_NAME" && it.@value == main }
        runConfig.option.any { it.@name == "VM_PARAMETERS" && it.@value == "-server -client" }
        runConfig.option.any { it.@name == "PROGRAM_PARAMETERS" && it.@value == "server dev/conf/server.yml" }
        runConfig.option.any { it.@name == "WORKING_DIRECTORY" && it.@value == "/" }
        runConfig.module.any { it.@name == this.projectName }
    }


    def "generates multiple run configs"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'baseline.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            task otherRun(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("idea")

        then:
        result.success

        String launchFilename = "${projectName}.iws"
        fileExists(launchFilename)

        def xml = new XmlSlurper().parseText(file(launchFilename).text)
        def runManager = xml.component.findResult { it.@name == "RunManager" ? it : null }
        runManager != null

        runManager.configuration.any { it.@name == "${projectName}-runDev" }
        runManager.configuration.any { it.@name == "${projectName}-otherRun" }
    }

    def "idea works with sub projects"() {
        setup:
        String subProjectName = "test-sub-project"
        writeHelloWorld("com.testing")

        addSubproject(subProjectName, """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'baseline.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            task otherRun(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }
        """.stripIndent())

        buildFile << """
            apply plugin: 'idea'
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("idea")

        then:
        result.success

        String launchFilename = "${projectName}.iws"
        fileExists(launchFilename)

        def xml = new XmlSlurper().parseText(file(launchFilename).text)
        def runManager = xml.component.findResult { it.@name == "RunManager" ? it : null }
        runManager != null

        runManager.configuration.any { it.@name == "${subProjectName}-runDev" }
        runManager.configuration.any { it.@name == "${subProjectName}-otherRun" }
    }
}
