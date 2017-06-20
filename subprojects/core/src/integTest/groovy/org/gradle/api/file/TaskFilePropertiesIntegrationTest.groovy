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
import spock.lang.Unroll

class TaskFilePropertiesIntegrationTest extends AbstractIntegrationSpec {
    def "creates task output file and directory locations specified using annotated properties"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @OutputFile
                File outputFile
                @OutputDirectory
                File outputDir
                @OutputFiles
                List<File> outputFiles = []
                @OutputFiles
                Map<String, File> outputFilesMap = [:]
                @OutputDirectories
                List<File> outputDirs = []
                @OutputDirectories
                Map<String, File> outputDirsMap = [:]
                
                @TaskAction
                def go() {
                    assert outputFile.parentFile.directory 
                    assert outputDir.directory 
                    outputFiles.each { assert it.parentFile.directory }
                    outputFilesMap.values().each { assert it.parentFile.directory }
                    outputDirs.each { assert it.directory }
                    outputDirsMap.values().each { assert it.directory }
                }
            }
            
            task someTask(type: TransformTask) {
                outputFile = file("build/files1/file1.txt")
                outputDir = file("build/dir1")
                outputFiles = [file("build/files2/file2.txt"), file("build/files3/file3.txt")]
                outputFilesMap = [a: file("build/files4/file4.txt"), b: file("build/files5/file5.txt")]
                outputDirs = [file("build/dir2"), file("build/dir3")]
                outputDirsMap = [a: file("build/dir4"), b: file("build/dir5")]
            }
"""

        when:
        run("someTask")

        then:
        file("build/files1").directory
        file("build/files2").directory
        file("build/files3").directory
        file("build/files4").directory
        file("build/files5").directory
        file("build/dir1").directory
        file("build/dir2").directory
        file("build/dir3").directory
        file("build/dir4").directory
        file("build/dir5").directory
    }

    def "creates task output file and directory locations specified using ad hoc properties"() {
        buildFile << """
            task someTask {
                outputs.file("build/files1/file1.txt")
                outputs.dir("build/dir1")
                outputs.files("build/files2/file2.txt", "build/files3/file3.txt")
                outputs.dirs("build/dir2", "build/dir3")
                outputs.files(a: "build/files4/file4.txt", b: "build/files5/file5.txt")
                outputs.dirs(a: "build/dir4", b: "build/dir5")
                doLast { }
            }
"""

        when:
        run("someTask")

        then:
        file("build/files1").directory
        file("build/files2").directory
        file("build/files3").directory
        file("build/files4").directory
        file("build/files5").directory
        file("build/dir1").directory
        file("build/dir2").directory
        file("build/dir3").directory
        file("build/dir4").directory
        file("build/dir5").directory
    }

    def "does not create output locations for task with no action"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @OutputFile
                File outputFile
                @OutputDirectory
                File outputDir
                @OutputFiles
                List<File> outputFiles = []
                @OutputFiles
                Map<String, File> outputFilesMap = [:]
                @OutputDirectories
                List<File> outputDirs = []
                @OutputDirectories
                Map<String, File> outputDirsMap = [:]
            }
            
            task someTask(type: TransformTask) {
                outputFile = file("build/files1/file1.txt")
                outputDir = file("build/dir1")
                outputFiles = [file("build/files2/file2.txt"), file("build/files3/file3.txt")]
                outputFilesMap = [a: file("build/files4/file4.txt"), b: file("build/files5/file5.txt")]
                outputDirs = [file("build/dir2"), file("build/dir3")]
                outputDirsMap = [a: file("build/dir4"), b: file("build/dir5")]
                outputs.file("build/files6/file6.txt")
                outputs.files("build/files7/file6.txt")
                outputs.dir("build/dir6")
                outputs.dirs("build/dir7")
            }
"""

        when:
        run("someTask")

        then:
        !file("build").exists()
    }

    def "fails when no value specified for annotated property"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @${annotation}
                File someProp
                
                @TaskAction
                def go() {
                }
            }
            
            task someTask(type: TransformTask) {
            }
"""

        when:
        fails("someTask")

        then:
        failure.assertHasDescription("A problem was found with the configuration of task ':someTask'.")
        failure.assertHasCause("No value has been specified for property 'someProp'.")

        where:
        annotation          | _
        "InputFile"         | _
        "InputFiles"        | _
        "InputDirectory"    | _
        "OutputFile"        | _
        "OutputFiles"       | _
        "OutputDirectory"   | _
        "OutputDirectories" | _
    }

    def "fails when input location does not exist"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @InputFile
                File inputFile
                @InputDirectory
                File inputDir
                
                @TaskAction
                def go() {
                }
            }
            
            task someTask(type: TransformTask) {
                inputDir = file("dir1")
                inputFile = file("file1.txt")
            }
"""

        when:
        fails("someTask")

        then:
        failure.assertHasDescription("Some problems were found with the configuration of task ':someTask'.")
        failure.assertHasCause("Directory '${file("dir1")}' specified for property 'inputDir' does not exist.")
        failure.assertHasCause("File '${file("file1.txt")}' specified for property 'inputFile' does not exist.")
    }

    def "fails when input location has incorrect type"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @InputFile
                File inputFile
                @InputDirectory
                File inputDir
                
                @TaskAction
                def go() {
                }
            }
            
            task someTask(type: TransformTask) {
                inputDir = file("dir1")
                inputFile = file("file1.txt")
            }
"""

        when:
        file("dir1").text = "123"
        file("file1.txt").createDir()

        fails("someTask")

        then:
        failure.assertHasDescription("Some problems were found with the configuration of task ':someTask'.")
        failure.assertHasCause("Directory '${file("dir1")}' specified for property 'inputDir' is not a directory.")
        failure.assertHasCause("File '${file("file1.txt")}' specified for property 'inputFile' is not a file.")
    }

    def "does not validate input files declared using @InputFiles"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @InputFiles
                List<File> inputFiles
                
                @TaskAction
                def go() {
                }
            }
            
            task someTask(type: TransformTask) {
                inputFiles = [file("missing"), file("file1.txt")]
            }
"""

        expect:
        file("file1.txt").createDir()

        succeeds("someTask")
    }

    def "does not validate input locations for ad hoc properties"() {
        buildFile << """
            task someTask {
                inputs.file("missing")
                inputs.file("file1.txt")
                inputs.dir("missing")
                inputs.dir("dir1")
                inputs.files("missing", "dir1", "file1.txt")
                doLast { }
            }
"""

        file("dir1").text = "123"
        file("file1.txt").createDir()

        expect:
        succeeds("someTask")
    }

    @Unroll
    def "complains when output directory for ad hoc property cannot be created"() {
        buildFile << """
            task someTask {
                outputs.${expression}
                doLast { }
            }
"""

        file("dir1").text = "123"

        expect:
        fails("someTask")
        failure.assertHasDescription("Cannot create directory '${file('dir1')}' as it already exists, but is not a directory")

        where:
        expression                   | _
        "dir('dir1')"                | _
        "dirs('dir1', 'dir2')"       | _
        "dirs(a: 'dir1', b: 'dir2')" | _
    }

    def "complains when output file parent directory for ad hoc property already exists and is a file"() {
        buildFile << """
            task someTask {
                outputs.${expression}
                doLast { }
            }
"""

        file("dir1").text = "123"

        expect:
        fails("someTask")
        failure.assertHasDescription("Cannot create directory '${file('dir1')}' as it already exists, but is not a directory")

        where:
        expression                            | _
        "file('dir1/child')"                  | _
        "files('dir1/child', 'child2')"       | _
        "files(a: 'dir1/child', b: 'child2')" | _
    }

    def "fails when output location has incorrect type"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @OutputFile
                File outputFile
                @OutputDirectory
                File outputDir
                @OutputFiles
                List<File> outputFiles
                @OutputFiles
                Map<String, File> outputFilesMap
                @OutputDirectories
                List<File> outputDirs
                @OutputDirectories
                Map<String, File> outputDirsMap
                
                @TaskAction
                def go() {
                }
            }
            
            task someTask(type: TransformTask) {
                outputFile = file("file1.txt")
                outputDir = file("dir1")
                outputFiles = [file("file1.txt")]
                outputDirs = [file("dir1")]
                outputFilesMap = [a: file("file1.txt")]
                outputDirsMap = [a: file("dir1")]
            }
"""

        when:
        file("dir1").text = "123"
        file("file1.txt").createDir()

        fails("someTask")

        then:
        failure.assertHasDescription("Some problems were found with the configuration of task ':someTask'.")
        failure.assertHasCause("Directory '${file("dir1")}' specified for property 'outputDir' is not a directory.")
        failure.assertHasCause("Directory '${file("dir1")}' specified for property 'outputDirs' is not a directory.")
        failure.assertHasCause("Cannot write to file '${file("file1.txt")}' specified for property 'outputFile' as it is a directory.")
        failure.assertHasCause("Cannot write to file '${file("file1.txt")}' specified for property 'outputFiles' as it is a directory.")
    }

    def "task can use Path to represent input and output locations on annotated properties"() {
        buildFile << """
            import java.nio.file.Path
            import java.nio.file.Files
            
            class TransformTask extends DefaultTask {
                @InputFile
                Path inputFile
                @InputDirectory
                Path inputDir
                @OutputFile
                Path outputFile
                @OutputDirectory
                Path outputDir
                
                @TaskAction
                def go() {
                    outputFile.text = inputFile.text
                    inputDir.toFile().listFiles().each { f -> outputDir.resolve(f.name).text = f.text }
                }
            }
            
            task transform(type: TransformTask) {
                inputFile = file("file1.txt").toPath()
                inputDir = file("dir1").toPath()
                outputFile = file("build/file1.txt").toPath()
                outputDir = file("build/dir1").toPath()
            }
"""

        when:
        file("file1.txt").text = "123"
        file("dir1/file2.txt").text = "1234"
        run("transform")

        then:
        file("build/file1.txt").text == "123"
        file("build/dir1/file2.txt").text == "1234"

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("file1.txt").text = "321"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("dir1/file3.txt").text = "new"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")
    }

    def "task can use Path to represent input and output locations for ad hoc properties"() {
        buildFile << """
            import java.nio.file.Path
            import java.nio.file.Files
            
            task transform {
                def inputFile = file("file1.txt").toPath()
                def inputDir = file("dir1").toPath()
                def outputFile = file("build/file1.txt").toPath()
                def outputDir = file("build/dir1").toPath()
                inputs.file(inputFile)
                inputs.dir(inputDir)
                outputs.file(outputFile)
                outputs.dir(outputDir)
                doLast {
                    outputFile.text = inputFile.text
                    inputDir.toFile().listFiles().each { f -> outputDir.resolve(f.name).text = f.text }
                }
            }
"""

        when:
        file("file1.txt").text = "123"
        file("dir1/file2.txt").text = "1234"
        run("transform")

        then:
        file("build/file1.txt").text == "123"
        file("build/dir1/file2.txt").text == "1234"

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("file1.txt").text = "321"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("dir1/file3.txt").text = "new"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")
    }
}
