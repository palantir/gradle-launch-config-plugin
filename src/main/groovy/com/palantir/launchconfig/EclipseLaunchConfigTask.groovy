/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.launchconfig

import groovy.xml.MarkupBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

class EclipseLaunchConfigTask extends DefaultTask {

    public static final String TASK_NAME = "eclipseLaunchConfig"
    public static final String CLEAN_TASK_NAME = "cleanEclipseLaunchConfig"

    void setup() {
        this.project.tasks.withType(JavaExec) { javaExec ->
            if (shouldGenerate(javaExec.name)) {
                Path launchFile = Paths.get("${project.projectDir}/${project.name}-${javaExec.name}.launch")
                this.outputs.file(launchFile.toString())
            }
        }
    }

    @TaskAction
    void generate() {
        this.project.tasks.withType(JavaExec) { javaExec ->
            if (shouldGenerate(javaExec.name)) {
                logger.debug("Generating launch file for {}.", javaExec.name)
                writeLaunchFile(javaExec)
            } else {
                logger.debug("Skipping generation of {} since it is excluded in the launchConfig.", javaExec.name)
            }
        }
    }

    void writeLaunchFile(JavaExec javaExec) {
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)

        xml.launchConfiguration(type: "org.eclipse.jdt.launching.localJavaApplication") {

            // default option in eclipse
            booleanAttribute(
                    key: "org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD",
                    value: "true")

            stringAttribute(
                    key: "org.eclipse.jdt.launching.MAIN_TYPE",
                    value: javaExec.main)

            stringAttribute(
                    key: "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS",
                    value: javaExec.args.join(" "))

            stringAttribute(
                    key: "org.eclipse.jdt.launching.VM_ARGUMENTS",
                    value: buildVmParams(javaExec).join(" "))

            stringAttribute(
                    key: "org.eclipse.jdt.launching.PROJECT_ATTR",
                    value: project.eclipse.project.name)

            if (javaExec.workingDir != project.projectDir) {
                stringAttribute(
                        key: "org.eclipse.jdt.launching.WORKING_DIRECTORY",
                        value: javaExec.workingDir)
            }
        }

        Path launchFile = Paths.get("${project.projectDir}/${project.name}-${javaExec.name}.launch")
        String xmlString = writer.toString() + "\n"
        Files.write(launchFile, xmlString.getBytes("UTF-8")).toFile()
    }

    /**
     * Build the VM Params from the JVM Args and the System Properties.
     *
     * Note: this needs to be left as "protected" to avoid scoping issues above
     */
    protected List<String> buildVmParams(JavaExec javaExec) {
        List<String> vmParams = new ArrayList<String>()
        vmParams.addAll(javaExec.jvmArgs)
        javaExec.systemProperties.each() { k, v -> vmParams.add("-D${k}${v ? '=' + v : ''}") }
        if (javaExec.enableAssertions) vmParams.add("-ea")
        if (javaExec.minHeapSize != null) vmParams.add("-Xms${javaExec.minHeapSize}")
        if (javaExec.maxHeapSize != null) vmParams.add("-Xmx${javaExec.maxHeapSize}")
        return vmParams
    }

    boolean shouldGenerate(String taskName) {
        LaunchConfigExtension extension = this.project.extensions.getByType(LaunchConfigExtension)
        Set<List> includedTasks = extension.getIncludedTasks()
        Set<List> excludedTasks = extension.getExcludedTasks()

        // 1. if includeTasks is empty, all tasks are included
        // 2. if includeTasks is not empty, only the specified tasks are included
        // 3. any tasks specified by excludedTasks are excluded from the generation
        return ((includedTasks.isEmpty() || includedTasks.contains(taskName)) && !excludedTasks.contains(taskName));
    }
}
