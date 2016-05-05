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

class IdeaLaunchConfigTask extends DefaultTask {

    public static final String TASK_NAME = "ideaLaunchConfig"

    @TaskAction
    void generate() {
        IdeaModel ideaRootModel = project.rootProject.extensions.findByType(IdeaModel)
        if (!ideaRootModel) {
            logger.debug("Could not find the root project's IdeaModel.")
            return
        }

        ideaRootModel.workspace.iws.withXml { xmlProvider ->
            Node runManager = xmlProvider.asNode().component.find { it.@name == "RunManager" }
            if (!runManager) {
                logger.debug("Could not find the root project's RunManager.")
                return
            }

            project.tasks.withType(JavaExec) { javaExec ->
                String runConfigName = determineRunConfigName(javaExec)

                if (runManager.find { it.@name == runConfigName }) {
                    logger.debug("Skipping generation of '%s' since it already exists.", runConfigName)

                } else {
                    logger.debug("Generating run configuration for '%s'.", runConfigName)
                    appendRunConfig(runManager, javaExec)
                }
            }
        }
    }

    void appendRunConfig(Node runManager, JavaExec javaExec) {
        runManager.appendNode('configuration', [default: 'false', name: determineRunConfigName(javaExec), type: 'Application', factoryName: 'Application'], [
                new Node(null, 'extension', [name: 'coverage', enabled: 'false', merge: 'false', runner: 'idea']),
                new Node(null, 'option', [name: 'MAIN_CLASS_NAME', value: javaExec.main]),
                new Node(null, 'option', [name: 'VM_PARAMETERS', value: javaExec.jvmArgs.join(" ")]),
                new Node(null, 'option', [name: 'PROGRAM_PARAMETERS', value: javaExec.args.join(" ")]),
                new Node(null, 'option', [name: 'WORKING_DIRECTORY', value: javaExec.workingDir]),
                new Node(null, 'option', [name: 'ALTERNATIVE_JRE_PATH_ENABLED', value: 'false']),
                new Node(null, 'option', [name: 'ALTERNATIVE_JRE_PATH', value: '']),
                new Node(null, 'option', [name: 'ENABLE_SWING_INSPECTOR', value: 'false']),
                new Node(null, 'option', [name: 'ENV_VARIABLES']),
                new Node(null, 'option', [name: 'PASS_PARENT_ENVS', value: 'true']),
                new Node(null, 'module', [name: project.name]),
                new Node(null, 'envs'),
                new Node(null, 'method')
        ])
    }

    String determineRunConfigName(JavaExec javaExec) {
        return "${project.name}-${javaExec.name}"
    }
}
