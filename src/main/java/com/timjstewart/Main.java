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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
                    AnsiConsole.out.println(ansi().fg(BLUE).a("Building:").reset());
                    for (final String file : changedFiles) {
                        AnsiConsole.out.println(ansi().fg(WHITE).a("  ○ ").fg(BLUE).a(  file).reset());
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

        final List<BlockRule> rules = new ArrayList<>();

        rules.add(new SingleLineRegexRule(WHITE  , "(Running .*)"));

        rules.add(new IgnoreLineContainingRule("----"));
        rules.add(new IgnoreLineContainingRule("Total time:"));
        rules.add(new IgnoreLineContainingRule("Finished at:"));
        rules.add(new IgnoreLineContainingRule("Compiling "));
        rules.add(new IgnoreLineContainingRule("Building "));
        rules.add(new IgnoreLineContainingRule("Final Memory"));
        rules.add(new IgnoreLineContainingRule("Copying"));
        rules.add(new IgnoreLineContainingRule("For more information"));
        rules.add(new IgnoreLineContainingRule("Nothing to compile"));
        rules.add(new IgnoreLineContainingRule("Changes detected"));
        rules.add(new IgnoreLineContainingRule("[compiler:"));
        rules.add(new IgnoreLineContainingRule("skip non existing"));
        rules.add(new IgnoreLineContainingRule("File encoding has not been set"));
        rules.add(new IgnoreLineContainingRule("[resources:"));
        rules.add(new IgnoreLineContainingRule("[surefire:"));
        rules.add(new IgnoreLineContainingRule("Surefire"));
        rules.add(new IgnoreLineContainingRule("Using platform encoding"));
        rules.add(new IgnoreLineContainingRule("Scanning for projects"));
        rules.add(new IgnoreLineContainingRule("Surefire"));
        rules.add(new IgnoreLineContainingRule("COMPILATION ERROR"));
        rules.add(new IgnoreLineContainingRule("Compilation failure"));
        rules.add(new IgnoreLineContainingRule("task-segment:"));

        rules.add(new SingleLineRegexRule(GREEN, "(Tests run: [0-9]+, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: [0-9\\.]+ sec)"));
        rules.add(new SingleLineRegexRule(RED, "(Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+, Time elapsed: [0-9\\.]+ sec)"));

        rules.add(new SingleLineRegexRule(GREEN, "(BUILD SUCCESSFUL)"));
        rules.add(new SingleLineRegexRule(YELLOW, "(No tests to run\\.)"));
        rules.add(new SingleLineRegexRule(WHITE, "( *symbol: .*)"));
        rules.add(new SingleLineRegexRule(RED, "\\[ERROR\\] (.*)"));
        rules.add(new SingleLineRegexRule(YELLOW, "\\[WARNING\\] (.*)"));
        rules.add(new SingleLineRegexRule(WHITE, "\\[INFO\\] (.*)"));

        rules.add(new MultiLineRule(BLUE , "(Results :.*)", "(Tests run:.*)"));

        InvocationRequest request = new DefaultInvocationRequest()
                .setPomFile(new File(job.getPomFile()))
                .setGoals(Arrays.asList(job.getTasks()))
                .setOutputHandler(
                        new InvocationOutputHandler() {
                            private boolean done = false;
                            private BlockRule currentRule = NullRule.getInstance();

                            @Override
                            public void consumeLine(String line) {
                                if (done)
                                    return;

                                line = makePathsRelative(job, line);

                                if (currentRule.isNull()) {
                                    currentRule = findMatchingRule(rules, line);

                                    if (!currentRule.shouldIgnore(line)) {
                                        AnsiConsole.out.println(currentRule.format(line));
                                    }

                                    if (currentRule.isEndOfBlock(line)) {
                                        currentRule = NullRule.getInstance();
                                    }
                                } else if (currentRule.isEndOfBlock(line)) {
                                    if (!currentRule.shouldIgnore(line)) {
                                        AnsiConsole.out.println(currentRule.format(line));
                                    }
                                    currentRule = NullRule.getInstance();
                                } else {
                                    if (!currentRule.shouldIgnore(line)) {
                                        AnsiConsole.out.println(currentRule.format(line));
                                    }
                                }

                                if (line.contains("BUILD FAILURE")) {
                                    done = true;
                                }
                            }
                        }
                );


        final Invoker invoker = new DefaultInvoker()
                .setMavenHome(new File(properties.getProperty("maven.home")));

        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }

    private static BlockRule findMatchingRule(
            final Collection<BlockRule> rules,
            final String line) {
        for (final BlockRule rule : rules) {
            if (rule.isStartOfBlock(line)) {
                return rule;
            }
        }
        return NullRule.getInstance();
    }

    private static String makePathsRelative(final Job job, final String s) {
        final String projectPath = Paths.get(System.getProperty("user.dir"), job.getProjectDirectory().toString()).getParent().toString();
        return s.replace(projectPath, ".");
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
