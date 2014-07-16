package com.timjstewart;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public class Job {

    /**
     * the relative path to the pom.xml file
     */
    private final String pomFile;

    /**
     * the tasks to execute when a change is detected
     */
    private final String[] tasks;

    /**
     * the (optional) path to the Java home directory to use
     */
    private final String javaHome;

    /**
     * the (optional) specifier for how many threads to use
     */
    private final String threadsSpec;

    /**
     * creates a Job
     */
    public Job(
            final String pomFile,
            final String[] tasks,
            final String javaHome,
            final String threadsSpec
    ) {
        this.pomFile     = Objects.requireNonNull(pomFile);
        this.tasks       = Objects.requireNonNull(tasks);
        this.javaHome    = javaHome;
        this.threadsSpec = threadsSpec;
    }

    /**
     * returns the Java Home directory for maven to use
     */
    public String getJavaHome() {
        return javaHome;
    }

    /**
     * returns the maven thread parameter
     */
    public String getThreadsSpec() {
        return threadsSpec;
    }

    /**
     * returns the relative path to the pom.xml file
     */
    public String getPomFile() {
        return pomFile;
    }

    /**
     * returns the tasks that maven should run when a change is
     * detected
     */
    public String[] getTasks() {
        return tasks;
    }

    /**
     * returns the root directory of the project
     */
    public Path getProjectDirectory() {
        return new File(pomFile).getParentFile().toPath();
    }
}
