gradle-launch-config-plugin
===========================
[![Circle CI](https://circleci.com/gh/palantir/gradle-launch-config-plugin.svg?style=shield&circle-token=c307f1a3f908dc210c9034f710a61715dc1f2250)](https://circleci.com/gh/palantir/gradle-launch-config-plugin)

A Gradle Plugin that creates `.launch` files for Ecipse and Run Configurations for IntelliJ for your project's
`JavaExec` tasks.

Usage
-----
1. Apply the plugin.
2. Call the respective IDE commands (i.e. `./gradlew idea` or `./gradlew eclipse`).


Tasks
-----
The tasks are only added if their matching IDE plugin is applied.

- `eclipseLaunchConfig` - creates `.launch` files. Triggered when `:eclipseProject` is called.
- `cleanEclipseLaunchConfig` - deletes the generated `.launch` files. Triggered when `:cleanEclipseProject` is called.
- `ideaLaunchConfig` - generates the XML in the IDEA workspace. Triggered when `:ideaWorkspace` is called.


Contributing
------------
Before working on the code, if you plan to contribute changes, please read the [CONTRIBUTING](CONTRIBUTING.md) document.


License
-------
This project is made available under the [Apache 2.0 License][license].


[license]: http://www.apache.org/licenses/LICENSE-2.0
