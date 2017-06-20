/*
 * Copyright 2016 the original author or authors.
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

import org.gradle.api.internal.file.FileResolver;

import java.io.File;
import java.util.Collection;

public class DefaultTaskInputFilesPropertySpec extends AbstractTaskInputPropertySpec implements TaskInputPropertySpecAndBuilder {
    public DefaultTaskInputFilesPropertySpec(String taskName, FileResolver resolver, Object paths) {
        super(taskName, resolver, paths);
    }
    @Override
    public void validate(Collection<String> messages) {
        if (!isStrict()) {
            return;
        }
        for (File file : getPropertyFiles()) {
            if (!file.exists()) {
                messages.add(String.format("File '%s' specified for property '%s' does not exist.", file, getPropertyName()));
            } else if (!file.isFile()) {
                messages.add(String.format("File '%s' specified for property '%s' is not a file.", file, getPropertyName()));
            }
        }
    }
}
