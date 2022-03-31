/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.launchconfig

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import spock.lang.Ignore

class EclipseLaunchConfigTaskIntegrationSpec extends IntegrationSpec {

    String projectName

    def setup() {
        this.projectName = getModuleName() 
    }

    def "generates launch files from a fully customized JavaExec"() {
        setup:
        writeHelloWorld("com.testing")
        String main = "com.testing.HelloWorld"
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main '${main}'
                args('server', 'dev/conf/server.yml')
                jvmArgs('-server', '-client', '-Ddw.assets')
                workingDir '/'
                maxHeapSize '4g'
                systemProperties['dw.abc'] = 123
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        String launchFilename = "${projectName}-runDev.launch"
        fileExists(launchFilename)

        def xml = new XmlSlurper().parseText(file(launchFilename).text)

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.MAIN_TYPE" && it.@value == main
        }
        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS" && it.@value == "server dev/conf/server.yml"
        }

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.VM_ARGUMENTS" && it.@value.toString().indexOf("-server -client") > -1
        }

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.VM_ARGUMENTS" && it.@value.toString().indexOf("-Ddw.assets") > -1
        }

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.VM_ARGUMENTS" && it.@value.toString().indexOf("-Ddw.abc=123") > -1
        }

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.VM_ARGUMENTS" && it.@value.toString().indexOf("-Xmx4g") > -1
        }

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.PROJECT_ATTR" && it.@value == this.projectName
        }

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.WORKING_DIRECTORY" && it.@value == "/"
        }
    }

    def "do not set WORKING_DIRECTORY if not defined in JavaExec"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        def xml = new XmlSlurper().parseText(file("${projectName}-runDev.launch").text)
        !xml.stringAttribute.any { it.@key == "org.eclipse.jdt.launching.WORKING_DIRECTORY" }
    }

    def "do not set WORKING_DIRECTORY if set to project directory"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main 'com.testing.HelloWorld'
                workingDir '${projectDir}'
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        def xml = new XmlSlurper().parseText(file("${projectName}-runDev.launch").text)
        !xml.stringAttribute.any { it.@key == "org.eclipse.jdt.launching.WORKING_DIRECTORY" }
    }

    def "generates multiple launch files"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
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
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        fileExists("${projectName}-otherRun.launch")
    }

    def "generates launch file using 'includedTasks' config"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
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
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        !fileExists("${projectName}-otherRun.launch")
    }

    def "generates launch file using 'excludedTasks' config"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
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
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        !fileExists("${projectName}-runDev.launch")
        fileExists("${projectName}-otherRun.launch")
    }

    def "generates launch file using both includedTasks and excludedTasks config"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
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
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        !fileExists("${projectName}-otherRun.launch")
        !fileExists("${projectName}-ignoredRun.launch")
        !fileExists("${projectName}-otherIgnoredRun.launch")
    }

    def "override existing launch files"() {
        setup:
        writeHelloWorld("com.testing")
        createFile('runDev.launch') << "original content"

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
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
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        file("${projectName}-runDev.launch").text != "original content"
    }

    @Ignore
    def "cleanEclipse should delete the file"() {
        setup:
        writeHelloWorld("com.testing")

        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
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
        ExecutionResult result = runTasksSuccessfully("eclipse", "cleanEclipse")

        then:
        result.success

        !fileExists("${projectName}-runDev.launch")
        !fileExists("${projectName}-otherRun.launch")
    }

    def "generates launch files including 'recognized' values"() {
        setup:
        writeHelloWorld("com.testing")
        String main = "com.testing.HelloWorld"
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'eclipse'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main '${main}'
                jvmArgs('-server', '-client', '-ea', '-Xmx4g', '-Xms2g')
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("-DignoreDeprecations=true", "eclipse")

        then:
        result.success

        String launchFilename = "${projectName}-runDev.launch"
        fileExists(launchFilename)

        def xml = new XmlSlurper().parseText(file(launchFilename).text)

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.MAIN_TYPE" && it.@value == main
        }

        xml.stringAttribute.any {
            it.@key == "org.eclipse.jdt.launching.VM_ARGUMENTS" && it.@value.toString().indexOf("-server -client -ea -Xms2g -Xmx4g") > -1
        }
    }
}
