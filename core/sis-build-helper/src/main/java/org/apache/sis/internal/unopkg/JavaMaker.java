/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.unopkg;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * Compiles Java interfaces from OpenOffice IDL files.
 *
 * <p>In an ideal world, this plugin would execute {@code idlc} on the {@code *.idl} files,
 * then {@code regmerge} on the generated {@code *.urd} files,
 * then {@code javamaker} on the generated {@code *.rdb} files.
 * However, since the above mentioned tools are native and would require a manual installation
 * on every developer machine, current version just copies a pre-compiled class file.
 * This copy must occurs after the compilation phase (in order to overwrite the files generated
 * by {@code javac}), which is why the usual Maven resources mechanism doesn't fit.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.8
 */
@Mojo(name = "javamaker", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public final class JavaMaker extends AbstractMojo {
    /**
     * Base directory of the module to compile.
     */
    @Parameter(property="basedir", required=true, readonly=true)
    private String baseDirectory;

    /**
     * Directory where the output Java files will be located.
     */
    @Parameter(property="project.build.outputDirectory", required=true, readonly=true)
    private String outputDirectory;

    /**
     * Invoked by reflection for creating the MOJO.
     */
    public JavaMaker() {
    }

    /**
     * Copies the {@code .class} files generated by OpenOffice.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final Copier c = new Copier(baseDirectory, outputDirectory);
        try {
            c.run();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy *.class files.", e);
        }
        getLog().info("[geotk-unopkg] Copied " + c.count + " pre-compiled class files.");
    }

    /**
     * Copies {@code *.class} files from source directory to output directory.
     * The output directory should already exist. It should be the case if all
     * sources files have been compiled before this method is invoked.
     */
    private static final class Copier extends SimpleFileVisitor<Path> {
        /**
         * The root of source and target directories. Files below {@code source} will be copied
         * with identical path (relative to {@code source}) under {@code target} directory.
         */
        private final Path source, target;

        /**
         * Number of files copied.
         */
        int count;

        /**
         * Creates a new copier.
         *
         * @param baseDirectory    base directory of the module to compile.
         * @param outputDirectory  directory where the output Java files will be located.
         */
        Copier(final String baseDirectory, final String outputDirectory) {
            source = Path.of(baseDirectory).resolve(UnoPkg.SOURCE_DIRECTORY);
            target = Path.of(outputDirectory);
        }

        /**
         * Executes the copy operation.
         */
        void run() throws IOException {
            Files.walkFileTree(source, this);
        }

        /**
         * Determines whether the given directory should be visited.
         * This method skips hidden directories.
         */
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
            return dir.getFileName().toString().startsWith(".") ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
        }

        /**
         * Invoked for a file in a directory. This method creates the directory if it does not exist
         * and performs the actual copy operation.
         */
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final String filename = file.getFileName().toString();
            if (filename.endsWith(".class") || filename.endsWith(".CLASS")) {
                final Path dst = target.resolve(source.relativize(file)).normalize();
                if (!dst.startsWith(target)) {
                    throw new IOException("Unexpected target path: " + dst);
                }
                Files.createDirectories(dst.getParent());
                Files.copy(file, dst, StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
