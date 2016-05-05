package baseline.launchconfig

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
