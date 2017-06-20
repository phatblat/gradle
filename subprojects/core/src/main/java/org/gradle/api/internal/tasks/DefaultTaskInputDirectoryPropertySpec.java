/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks;

import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.FileResolver;

import java.io.File;
import java.util.Collection;

public class DefaultTaskInputDirectoryPropertySpec extends AbstractTaskInputPropertySpec implements TaskInputPropertySpecAndBuilder {
    private final FileCollection dirs;

    public DefaultTaskInputDirectoryPropertySpec(String taskName, FileResolver resolver, FileCollection dirs) {
        super(taskName, resolver, dirs.getAsFileTree());
        this.dirs = dirs;
    }

    @Override
    public void validate(Collection<String> messages) {
        if (!isStrict()) {
            return;
        }
        for (File file : dirs) {
            if (!file.exists()) {
                messages.add(String.format("Directory '%s' specified for property '%s' does not exist.", file, getPropertyName()));
            } else if (!file.isDirectory()) {
                messages.add(String.format("Directory '%s' specified for property '%s' is not a directory.", file, getPropertyName()));
            }
        }
    }
}
