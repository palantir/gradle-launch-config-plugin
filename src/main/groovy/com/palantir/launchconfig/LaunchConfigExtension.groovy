/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
