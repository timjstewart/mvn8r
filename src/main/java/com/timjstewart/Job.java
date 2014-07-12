package com.timjstewart;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public class Job {

    private final String pomFile;

    private final String[] tasks;

    public Job(
            final String pomFile,
            final String[] tasks
    ) {
        this.pomFile = Objects.requireNonNull(pomFile);
        this.tasks   = Objects.requireNonNull(tasks);
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