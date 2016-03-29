package com.palantir.launchconfig

import nebula.test.PluginProjectSpec

class LaunchConfigPluginProjectSpec extends PluginProjectSpec {

    @Override
    String getPluginName() {
        return "com.palantir.launch-config"
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
        project.tasks.getByName("ideaWorkspace").dependsOn.contains(IdeaLaunchConfigTask.TASK_NAME)
    }
}
