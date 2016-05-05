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

class EclipseLaunchConfigTaskIntegrationSpec extends IntegrationSpec {

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
            apply plugin: 'eclipse'
            apply plugin: 'com.palantir.launch-config'

            task runDev(type: JavaExec) {
                classpath project.sourceSets.main.runtimeClasspath
                main '${main}'
                args('server', 'dev/conf/server.yml')
                jvmArgs('-server', '-client')
                workingDir '/'
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully("eclipse")

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
            it.@key == "org.eclipse.jdt.launching.VM_ARGUMENTS" && it.@value == "-server -client"
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
        ExecutionResult result = runTasksSuccessfully("eclipse")

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
        ExecutionResult result = runTasksSuccessfully("eclipse")

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
        ExecutionResult result = runTasksSuccessfully("eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        fileExists("${projectName}-otherRun.launch")
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
        ExecutionResult result = runTasksSuccessfully("eclipse")

        then:
        result.success

        fileExists("${projectName}-runDev.launch")
        file("${projectName}-runDev.launch").text != "original content"
    }

    def "cleanEclipse should delete the file"() {
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
        ExecutionResult result = runTasksSuccessfully("eclipse", "cleanEclipse")

        then:
        result.success

        !fileExists("${projectName}-runDev.launch")
        !fileExists("${projectName}-otherRun.launch")
    }
}
