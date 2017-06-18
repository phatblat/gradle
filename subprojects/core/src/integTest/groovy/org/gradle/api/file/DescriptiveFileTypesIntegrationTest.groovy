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

package org.gradle.api.file

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

import static org.gradle.integtests.fixtures.executer.TaskOrderSpecs.any

class DescriptiveFileTypesIntegrationTest extends AbstractIntegrationSpec {
    def "can wire the output of a task as input to another task"() {
        buildFile << """
            class DirOutputTask extends DefaultTask {
                @InputFile
                final RegularFileVar inputFile = newInputFile()
                @OutputDirectory
                final DirectoryVar outputDir = newOutputDirectory()
                
                @TaskAction
                void go() {
                    def dir = outputDir.asFile.get()
                    new File(dir, "file.txt").text = inputFile.asFile.get().text
                }
            }
            
            class FileOutputTask extends DefaultTask {
                @InputFile
                final RegularFileVar inputFile = newInputFile()
                @OutputFile
                final RegularFileVar outputFile = newOutputFile()
                
                @TaskAction
                void go() {
                    def file = outputFile.asFile.get()
                    file.text = inputFile.asFile.get().text
                }
            }

            class MergeTask extends DefaultTask {
                @InputFile
                final RegularFileVar inputFile = newInputFile()
                @InputFiles
                final ConfigurableFileCollection inputFiles = project.files()
                @OutputFile
                final RegularFileVar outputFile = newOutputFile()
                
                @TaskAction
                void go() {
                    def file = outputFile.asFile.get()
                    file.text = ""
                    file << inputFile.asFile.get().text
                    inputFiles.each { file << ',' + it.text }
                }
            }
            
            task createDir(type: DirOutputTask)
            task createFile1(type: FileOutputTask)
            task createFile2(type: FileOutputTask)
            task merge(type: MergeTask) {
                outputFile.set(layout.buildDirectory.file("merged.txt"))
                inputFile.set(createFile1.outputFile)
                inputFiles.from(createFile2.outputFile)
                inputFiles.from(createDir.outputDir.asFileTree)
            }
            
            // Set values lazily
            createDir.inputFile.set(layout.projectDirectory.file("dir1-source.txt"))
            createDir.outputDir.set(layout.buildDirectory.dir("dir1"))
            createFile1.inputFile.set(layout.projectDirectory.file("file1-source.txt"))
            createFile1.outputFile.set(layout.buildDirectory.file("file1.txt"))
            createFile2.inputFile.set(layout.projectDirectory.file("file2-source.txt"))
            createFile2.outputFile.set(layout.buildDirectory.file("file2.txt"))
            
            buildDir = "output"
"""
        file("dir1-source.txt").text = "dir1"
        file("file1-source.txt").text = "file1"
        file("file2-source.txt").text = "file2"

        when:
        run("merge")

        then:
        result.assertTasksExecuted(":createDir", ":createFile1", ":createFile2", ":merge")
        file("output/merged.txt").text == 'file1,file2,dir1'

        when:
        run("merge")

        then:
        result.assertTasksNotSkipped()

        when:
        file("file1-source.txt").text = "new-file1"
        run("merge")

        then:
        result.assertTasksNotSkipped(":createFile1", ":merge")
        file("output/merged.txt").text == 'new-file1,file2,dir1'
    }

    def "task dependencies are inferred from contents of input FileCollection"() {
        // Include a configuration with transitive dep on a Jar and an unmanaged Jar.
        file('settings.gradle') << 'include "a", "b"'
        file('a/build.gradle') << '''
            configurations { compile }
            dependencies { compile project(path: ':b', configuration: 'archives') }
            
            task doStuff(type: InputTask) {
                src = configurations.compile + fileTree('src/java')
            }
            
            class InputTask extends DefaultTask {
                @InputFiles
                def FileCollection src
            }
        '''
        file('b/build.gradle') << '''
            apply plugin: 'base'
            task jar {
                doLast {
                    file('b.jar').text = 'some jar'
                }
            }
            
            task otherJar(type: Jar) {
                destinationDir = buildDir
            }
            
            configurations { archives }
            dependencies { archives files('b.jar') { builtBy jar } }
            artifacts { archives otherJar }
        '''

        when:
        run("doStuff")

        then:
        result.assertTasksExecutedInOrder(any(':b:jar', ':b:otherJar'), ':a:doStuff')
    }
}
