/*
 * Copyright 2016 Palantir Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.launchconfig

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
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main '${main}'
                args('server', 'dev/conf/server.yml')
                jvmArgs('-server', '-client')
                workingDir '/'
                maxHeapSize '4g'
                systemProperties['dw.assets'] = null
                systemProperties['dw.abc'] = 123
                environment['dw.env'] = 'defg'
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
        runConfig.option.any { it.@name == "VM_PARAMETERS" && it.@value.toString().contains("-server -client") }
        runConfig.option.any { it.@name == "VM_PARAMETERS" && it.@value.toString().contains("-Ddw.assets") }
        runConfig.option.any { it.@name == "VM_PARAMETERS" && it.@value.toString().contains("-Ddw.abc=123") }
        runConfig.option.any { it.@name == "VM_PARAMETERS" && !it.@value.toString().contains("com.palantir.launchconfig") }
        runConfig.option.any { it.@name == "VM_PARAMETERS" && it.@value.toString().contains("-Xmx4g") }
        runConfig.envs.any { it.env.any { it.@name == "dw.env" && it.@value == "defg" } }
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
            apply plugin: 'com.palantir.launch-config'

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

    def "generates launch file using 'includedTasks' config"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            task otherRun(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            launchConfig {
                includedTasks 'runDev'
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
        !runManager.configuration.any { it.@name == "${projectName}-otherRun" }
    }

    def "generates launch file using 'excludedTasks' config"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            task otherRun(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            launchConfig {
                excludedTasks 'runDev'
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

        !runManager.configuration.any { it.@name == "${projectName}-runDev" }
        runManager.configuration.any { it.@name == "${projectName}-otherRun" }
    }

    def "generates launch file using both includedTasks and excludedTasks config"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            task otherRun(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            task ignoredRun(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            task otherIgnoredRun(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }

            launchConfig {
                includedTasks 'runDev', 'otherRun'
                excludedTasks 'otherRun'
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
        !runManager.configuration.any { it.@name == "${projectName}-otherRun" }
        !runManager.configuration.any { it.@name == "${projectName}-ignoredRun" }
        !runManager.configuration.any { it.@name == "${projectName}-otherIgnoredRun" }
    }

    def "idea works with sub projects"() {
        setup:
        String subProjectName = "test-sub-project"
        writeHelloWorld("com.testing")

        addSubproject(subProjectName, """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'com.palantir.launch-config'

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

    def "idea works with sub projects with the same name"() {
        setup:
        writeHelloWorld("com.testing")

        addSubproject(projectName, """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'com.palantir.launch-config'

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

        def subprojectIdeaModelName = "${projectName}-${projectName}"

        runManager.configuration.any { it.@name == "${subprojectIdeaModelName}-runDev" }
        runManager.configuration.any { it.@name == "${subprojectIdeaModelName}-otherRun" }
    }

    def "generates launch files including 'recognized' values"() {
        setup:
        writeHelloWorld("com.testing")
        String main = "com.testing.HelloWorld"
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'idea'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main '${main}'
                jvmArgs('-server', '-client', '-ea', '-Xmx4g', '-Xms2g')
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
        runConfig.option.any { it.@name == "VM_PARAMETERS" && it.@value.toString().contains("-server -client -ea -Xms2g -Xmx4g") }
        runConfig.module.any { it.@name == this.projectName }
    }
}
