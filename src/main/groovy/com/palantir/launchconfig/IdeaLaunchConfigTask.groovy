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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.IdeaModule

class IdeaLaunchConfigTask extends DefaultTask {

    public static final String TASK_NAME = "ideaLaunchConfig"

    @TaskAction
    void generate() {
        IdeaModel ideaRootModel = project.rootProject.extensions.findByType(IdeaModel)
        if (ideaRootModel == null) {
            logger.debug("Could not find the root project's IdeaModel.")
            return
        }

        IdeaModel ideaModel = project.extensions.findByType(IdeaModel)
        if (ideaModel == null) {
            logger.debug("Could not find the project's idea module.")
            return
        }
        IdeaModule ideaModule = ideaModel.module

        ideaRootModel.workspace.iws.withXml { xmlProvider ->
            Node runManager = xmlProvider.asNode().component.find { it.@name == "RunManager" }
            if (!runManager) {
                logger.debug("Could not find the root project's RunManager.")
                return
            }

            project.tasks.withType(JavaExec) { javaExec ->
                if (shouldGenerate(javaExec.name)) {
                    String runConfigName = determineRunConfigName(ideaModule, javaExec)

                    if (runManager.find { it.@name == runConfigName }) {
                        logger.debug("Skipping generation of {} since it already exists.", runConfigName)
                    } else {
                        logger.debug("Generating run configuration for {}.", runConfigName)
                        appendRunConfig(ideaModule, runManager, javaExec)
                    }
                } else {
                    logger.debug("Skipping generation of {} since it is excluded in the launchConfig.", javaExec.name)
                }
            }
        }
    }

    void appendRunConfig(IdeaModule module, Node runManager, JavaExec javaExec) {
        runManager.appendNode('configuration', [default: 'false', name: determineRunConfigName(module, javaExec), type: 'Application', factoryName: 'Application'], [
                new Node(null, 'extension', [name: 'coverage', enabled: 'false', merge: 'false', runner: 'idea']),
                new Node(null, 'option', [name: 'MAIN_CLASS_NAME', value: javaExec.main]),
                new Node(null, 'option', [name: 'VM_PARAMETERS', value: buildVmParams(javaExec).join(' ')]),
                new Node(null, 'option', [name: 'PROGRAM_PARAMETERS', value: javaExec.args.join(' ')]),
                new Node(null, 'option', [name: 'WORKING_DIRECTORY', value: javaExec.workingDir]),
                new Node(null, 'option', [name: 'ALTERNATIVE_JRE_PATH_ENABLED', value: 'false']),
                new Node(null, 'option', [name: 'ALTERNATIVE_JRE_PATH', value: '']),
                new Node(null, 'option', [name: 'ENABLE_SWING_INSPECTOR', value: 'false']),
                new Node(null, 'option', [name: 'ENV_VARIABLES']),
                new Node(null, 'option', [name: 'PASS_PARENT_ENVS', value: 'true']),
                new Node(null, 'module', [name: module.name]),
                buildEnvVars(javaExec),
                new Node(null, 'method')
        ])
    }

    private static Node buildEnvVars(JavaExec javaExec) {
        def envsNode = new Node(null, 'envs')
        LaunchConfigPlugin.filterEnvVars(javaExec.environment).each {
            envsNode.append(new Node(null, 'env', [name: it.key, value: it.value]))
        }
        return envsNode
    }

    /**
     * Build the VM Params from the JVM Args and the System Properties.
     */
    private List<String> buildVmParams(JavaExec javaExec) {
        List<String> vmParams = new ArrayList<String>()
        vmParams.addAll(javaExec.jvmArgs)
        javaExec.systemProperties.each() { k, v -> vmParams.add("-D${k}${v ? '=' + v : ''}") }
        return vmParams
    }

    String determineRunConfigName(IdeaModule module, JavaExec javaExec) {
        return "${module.name}-${javaExec.name}"
    }

    boolean shouldGenerate(String taskName) {
        LaunchConfigExtension extension = this.project.extensions.getByType(LaunchConfigExtension)
        Set<String> includedTasks = extension.getIncludedTasks()
        Set<String> excludedTasks = extension.getExcludedTasks()

        // 1. if includeTasks is empty, all tasks are included
        // 2. if includeTasks is not empty, only the specified tasks are included
        // 3. any tasks specified by excludedTasks are excluded from the generation
        return ((includedTasks.isEmpty() || includedTasks.contains(taskName)) && !excludedTasks.contains(taskName));
    }
}
