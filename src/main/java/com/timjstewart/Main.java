/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.timjstewart;

import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;

import java.nio.file.attribute.*;
import java.util.*;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class Main {

    private static class Job {
        private final String pomFile;
        private final String[] tasks;

        public Job(
                final String pomFile,
                final String[] tasks
        ) {
            this.pomFile = Objects.requireNonNull(pomFile);
            this.tasks = Objects.requireNonNull(tasks);
        }

        public String getPomFile() {
            return pomFile;
        }

        public String[] getTasks() {
            return tasks;
        }

        public Path getProjectDirectory() {
            return new File(pomFile).getParentFile().toPath();
        }
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            usage();
            System.exit(1);
        }

        final Properties properties = loadProperties();

        final Job job = new Job(args[0], Arrays.copyOfRange(args, 1, args.length - 1));

        try {

            new WatchDir(job.getProjectDirectory()).processEvents(new WatchDir.Handler() {
                @Override
                public void onChange( ) {
                    perform(job, properties);
                }
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private static void perform(final Job job, final Properties properties) {

        InvocationRequest request = new DefaultInvocationRequest()
                .setPomFile(new File(job.getPomFile()))
                .setGoals(Arrays.asList(job.getTasks()))
                .setOutputHandler(new InvocationOutputHandler() {
                    @Override
                    public void consumeLine(String s) {
                        System.out.println(s);
                    }
                });

        Invoker invoker = new DefaultInvoker()
                .setMavenHome(new File(properties.getProperty("maven.home")));

        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }

    private static Properties loadProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("maven.home", "/usr/");

        final String homeDir = System.getProperty("user.home");

        if (homeDir == null) {
            System.err.println("Could not determine where user's home directory is.  Unable to load config.");
            return defaults;
        }

        Properties properties = new Properties(defaults);

        File propertyFile = new File(new File(homeDir), "mvn8r.properties");

        try (FileInputStream in = new FileInputStream(propertyFile)) {
            properties.load(in);
            return properties;
        } catch (IOException ex) {
            return defaults;
        }

    }

    private static void usage() {
        System.err.println("usage: mvn8r POM_FILE TASK...");
    }

}


class WatchDir {

    public interface Handler {
        void onChange();
    }

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private boolean trace = false;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                // System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    WatchDir(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();

        registerAll(dir);

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents(final Handler handler) {
        for (; ; ) {

            boolean shouldBuild = false;

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                final String extension = getFileExtension(child);

                if (extension.equals("java") ||
                        extension.equals("scala") ||
                        extension.equals("groovy") ||
                        extension.equals("clj") ||
                        child.getFileName().toString().equals("pom.xml")) {
                    shouldBuild = true;
                }

                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    break;
                }
            }

            if (shouldBuild) {
                handler.onChange();
            }
        }
    }

    private String getFileExtension(Path child) {

        final String fileName = child.getFileName().toString();

        int i = fileName.lastIndexOf('.');

        if (i > 0) {
            return fileName.substring(i + 1);
        } else {
            return "";
        }
    }
}