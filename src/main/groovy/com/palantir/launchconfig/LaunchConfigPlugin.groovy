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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin

class LaunchConfigPlugin implements Plugin<Project> {

    public static final String GROUP_NAME = "Launch Config"

    @Override
    void apply(Project project) {
        project.plugins.withType(EclipsePlugin) {
            EclipseLaunchConfigTask eclipseTask = project.tasks.create(
                    EclipseLaunchConfigTask.TASK_NAME,
                    EclipseLaunchConfigTask, {
                group = GROUP_NAME
                description = "Generates '.launch' files for all the JavaExec tasks in your project."
            })

            project.tasks.getByName("eclipseProject").dependsOn.add(EclipseLaunchConfigTask.TASK_NAME)
            project.tasks.getByName("cleanEclipseProject").dependsOn.add(EclipseLaunchConfigTask.CLEAN_TASK_NAME)

            project.afterEvaluate {
                eclipseTask.setup()
            }
        }

        project.plugins.withType(IdeaPlugin) {
            IdeaLaunchConfigTask ideaTask = project.tasks.create(
                    IdeaLaunchConfigTask.TASK_NAME,
                    IdeaLaunchConfigTask, {
                group = GROUP_NAME
                description = "Generates IDEA run configurations for all the JavaExec tasks in your project."
            })

            project.rootProject.tasks.getByName("ideaWorkspace").dependsOn.add(ideaTask)
        }
    }
}
