package com.timjstewart;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {

        if (args.length < 2) {
            usage();
            System.exit(1);
        }

        final Properties properties = loadProperties();

        final Job job = new Job(args[0], Arrays.copyOfRange(args, 1, args.length));

        try {
            new WatchDir(job.getProjectDirectory()).processEvents(new WatchDir.Handler() {
                @Override
                public void onChange(final String[] changedFiles) {
                    AnsiConsole.out.println(ansi().fg(WHITE).a("Building:").reset());
                    for (final String file : changedFiles) {
                        AnsiConsole.out.println(ansi().fg( WHITE).a("  " + file).reset());
                    }
                    perform(job, properties);
                }
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void perform(final Job job, final Properties properties) {

        AnsiConsole.systemInstall();

        InvocationRequest request = new DefaultInvocationRequest()
                .setPomFile(new File(job.getPomFile()))
                .setGoals(Arrays.asList(job.getTasks()))
                .setOutputHandler(new InvocationOutputHandler() {
                    @Override
                    public void consumeLine(String s) {
                        if (shouldIgnore(s)) {
                        } else if (s.contains("BUILD SUCCESSFUL")) {
                            AnsiConsole.out.println(ansi().fg(GREEN).a(s.substring(7)).reset());
                        } else if (s.startsWith("[INFO]")) {
                            AnsiConsole.out.println(ansi().fg(YELLOW).a(s.substring(7)).reset());
                        } else if (s.startsWith("[WARNING]")) {
                            AnsiConsole.out.println(ansi().fg(YELLOW).a(s.substring(10)).reset());
                        } else if (s.startsWith("[ERROR]") || s.contains("symbol: ") || s.contains("location: ")) {
                            AnsiConsole.out.println(ansi().fg(RED).a(s.substring(8)).reset());
                        } else {
                            //System.out.println(s);
                        }
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

    private static boolean shouldIgnore(final String s) {
        return s.contains("----") ||
                s.contains("Total time:") ||
                s.contains("Finished at:") ||
                s.contains("Compiling ") ||
                s.contains("Building ") ||
                s.contains("Final Memory") ||
                s.contains("For more information") ||
                s.contains("Copying") ||
                s.contains("Nothing to compile") ||
                s.contains("Changes detected") ||
                s.contains("[compiler:compile") ||
                s.contains("File encoding has not been set") ||
                s.contains("[resources:resources") ||
                s.contains("Using platform encoding") ||
                s.contains("Scanning for projects") ||
                s.contains("COMPILATION ERROR") ||
                s.contains("Compilation failure") ||
                s.contains("task-segment:");
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
