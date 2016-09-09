package com.palantir.launchconfig

class LaunchConfigExtension {
    private Set<String> includedTasks = [] as Set
    private Set<String> excludedTasks = [] as Set

    public void includedTasks(String... includedTasks) {
        this.includedTasks = new HashSet<>();
        Collections.addAll(this.includedTasks, includedTasks)
    }

    public Set<String> getIncludedTasks() {
        return includedTasks;
    }

    public void excludedTasks(String... excludedTasks) {
        this.excludedTasks = new HashSet<>();
        Collections.addAll(this.excludedTasks, excludedTasks)
    }

    public Set<String> getExcludedTasks() {
        return excludedTasks;
    }
}
