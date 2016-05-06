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

package baseline.launchconfig

import nebula.test.PluginProjectSpec

class LaunchConfigPluginProjectSpec extends PluginProjectSpec {

    @Override
    String getPluginName() {
        return "baseline.launch-config"
    }

    def "'eclipse' task depends on 'EclipseLaunchConfigTask' if 'eclipse' plugin applied"() {
        when:
        project.apply plugin: pluginName
        project.apply plugin: 'eclipse'
        project.evaluate()

        then:
        project.tasks.getByName("eclipseProject").dependsOn.contains(EclipseLaunchConfigTask.TASK_NAME)
    }

    def "'idea' task depends on 'IdeaLaunchConfigTask' if 'idea' plugin applied"() {
        when:
        project.apply plugin: pluginName
        project.apply plugin: 'idea'
        project.evaluate()

        then:
        project.tasks.getByName("ideaWorkspace").dependsOn.any { it instanceof IdeaLaunchConfigTask }
    }
}
