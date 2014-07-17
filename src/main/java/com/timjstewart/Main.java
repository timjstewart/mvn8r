package com.timjstewart;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.timjstewart.rules.BlockRule;
import com.timjstewart.rules.BlockRuleListener;
import com.timjstewart.rules.SingleLineRegexRule;
import com.timjstewart.rules.NullRule;
import com.timjstewart.rules.MultiLineRule;
import com.timjstewart.rules.IgnoreLineMatchingRegexRule;
import com.timjstewart.rules.IgnoreLineContainingRule;

public class Main {

    /**
     * main entry point into the program
     */
    public static void main(String[] args) {

        if (args.length < 2) {
            usage();
            System.exit(1);
        }

        AnsiConsole.systemInstall();

        final Properties properties = loadProperties();

        final Job job = createJob(args, properties);

        watchForChanges(job, properties);
    }

    private static void watchForChanges(final Job job,
                                        final Properties properties) {
        try {
            new WatchDir(job.getProjectDirectory())
                .processEvents(new WatchDir.Handler() {

                        @Override
                        public void onChange(final String[] changedFiles) {
                            
                            AnsiConsole.out.println(ansi()
                                                    .fg(BLUE).a("=> ") 
                                                    .fg(WHITE).a("Building:")
                                                    .reset());
                            
                            for (final String file : changedFiles) {
                                AnsiConsole.out.println(ansi()
                                                        .fg(BLUE).a("===> ")
                                                        .fg(WHITE).a(file)
                                                        .reset());
                            }
                            
                            perform(job, properties, changedFiles);
                        }
                    });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void perform(final Job job, 
                                final Properties properties, 
                                final String[] changedFiles) {

        final Boolean[] unitTestFailed = new Boolean[] { false };

        final List<BlockRule> rules = getBlockRules(new BlockRuleListener() {
                @Override
                public void onRuleMatched(final String line) {
                    unitTestFailed[0] = true;
                }
            });

        InvocationRequest request = new DefaultInvocationRequest()
            .setPomFile(new File(job.getPomFile()))
            .setGoals(Arrays.asList(job.getTasks()))
            .setDebug(false)
            .setOffline(!pomFileChanged(changedFiles))
            .setOutputHandler(new InvocationOutputHandler() {
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
                            // done processing the current rule
                            if (!currentRule.shouldIgnore(line)) {
                                AnsiConsole.out.println(currentRule.format(line));
                            }
                            currentRule = NullRule.getInstance();
                        } else {
                            // there is a current rule; see if it applies
                            if (!currentRule.shouldIgnore(line)) {
                                AnsiConsole.out.println(currentRule.format(line));
                            }
                        }
                        
                        if (line.contains("BUILD FAILURE")) {
                            done = true;
                        }
                    }
                });
        
        if (job.getJavaHome() != null) {
            request.setJavaHome(new File(job.getJavaHome()));
        }

        if (job.getThreadsSpec() != null) {
            request.setThreads(job.getThreadsSpec());
        }

        final Invoker invoker = new DefaultInvoker()
                .setMavenHome(new File(properties.getProperty("maven.home")));

        try {
            final InvocationResult result = invoker.execute(request);

            //if (unitTestFailed[0]) {
            //    printFailedUnitTestStackTraces(job);
            //}
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true iff the pom.xml file is among the changed files.
     * This way we can build the project in offline mode if the
     * pom.xml file has not changed and save some time.
     *
     * Downside: user may have to run 'mvn compile' from time to time
     * to update dependencies introduced while this program is not
     * running.
     */
    private static boolean pomFileChanged(final String[] changedFiles) {
        for (final String changedFile : changedFiles)
            if (changedFile.equals("pom.xml"))
                return true;
        return false;
    }

    private static BlockRule findMatchingRule(
            final Collection<BlockRule> rules,
            final String line) {

        for (final BlockRule rule : rules) {
            if (rule.isStartOfBlock(line)) {
                rule.onRuleMatched(line);
                return rule;
            }
        }

        return NullRule.getInstance();
    }

    private static String makePathsRelative(final Job job, final String s) {

        final String projectPath = Paths.get(System.getProperty("user.dir"), 
                                             job.getProjectDirectory().toString())
            .getParent().toString();

        return s.replace(projectPath, ".");
    }

    private static Properties loadProperties() {
        final Properties defaults = new Properties();

        defaults.setProperty("maven.home", "/usr/");

        final String homeDir = System.getProperty("user.home");

        if (homeDir == null) {
            System.err.println("Could not determine where user's home directory is. " +
                               "Unable to load config.");
            return defaults;
        } 

        Properties properties = new Properties(defaults);

        File propertyFile = new File(new File(homeDir), ".mvn8r.properties");

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

    /**
     * returns an ordered list of BlockRules
     *
     * @todo load them from a file perhaps?
     */
    private static List<BlockRule> getBlockRules(final BlockRuleListener onUnitTestFailed) {

        final List<BlockRule> rules = new ArrayList<>();

        rules.add(new SingleLineRegexRule(WHITE  , "(Running .*)"));

        rules.add(new IgnoreLineMatchingRegexRule("\\[[A-Z]+\\] *$"));

        rules.add(new IgnoreLineContainingRule("---"));
        rules.add(new IgnoreLineContainingRule("Total time:"));
        rules.add(new IgnoreLineContainingRule("Finished at:"));
        rules.add(new IgnoreLineContainingRule("Compiling "));
        rules.add(new IgnoreLineContainingRule("Building "));
        rules.add(new IgnoreLineContainingRule("Final Memory"));
        rules.add(new IgnoreLineContainingRule("Copying"));
        rules.add(new IgnoreLineContainingRule("Deleting "));
        rules.add(new IgnoreLineContainingRule("Error stacktraces are turned on."));
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
        rules.add(new IgnoreLineContainingRule("selectAuthScheme"));

        rules.add(new SingleLineRegexRule(GREEN, "(Tests run: [0-9]+, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: [0-9\\.]+ sec)"));
        rules.add(new SingleLineRegexRule(YELLOW, "(Tests run: [0-9]+, Failures: 0, Errors: 0, Skipped: [0-9]+, Time elapsed: [0-9\\.]+ sec)"));

        final BlockRule failedUnitTest = 
            new SingleLineRegexRule(RED, "(Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+, Time elapsed: [0-9\\.]+ sec)");
        rules.add(failedUnitTest);
        failedUnitTest.addListener(onUnitTestFailed);

        rules.add(new SingleLineRegexRule(GREEN, "(BUILD SUCCESSFUL)"));
        rules.add(new SingleLineRegexRule(GREEN, "(BUILD SUCCESS)"));
        rules.add(new SingleLineRegexRule(YELLOW, "(No tests to run\\.)"));
        rules.add(new SingleLineRegexRule(WHITE, "( *symbol: .*)"));
        rules.add(new SingleLineRegexRule(RED, "\\[ERROR\\] (.*)"));

        rules.add(new MultiLineRule(BLUE , "(Results :.*)", "(Tests run:.*)"));

        return rules;
    }

    private static Job createJob(final String[] args,
                                 final Properties properties) {
        return new Job(args[0], 
                       Arrays.copyOfRange(args, 1, args.length),
                       properties.getProperty("java.home"),
                       properties.getProperty("threads.spec"));
    }

    private static void printFailedUnitTestStackTraces(final Job job) {
        final Path reportDirectory = 
            Paths.get(job.getProjectDirectory().toString(),
                      "target",
                      "surefire-reports");

        for (final File file : reportDirectory.toFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File file, final String name) {
                    return name.endsWith(".txt");
                }
            })) {
            try {
                final FileInputStream stream = new FileInputStream(file.toString());
                final InputStreamReader reader = new InputStreamReader(stream, Charset.forName("UTF-8"));
                final BufferedReader bufReader = new BufferedReader(reader);
                
                String line = null;
                
                while ((line = bufReader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (FileNotFoundException  ex) {
                
            } catch (IOException  ex) {

            }
        }
    }
}
