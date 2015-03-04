package plugins;

import java.util.SortedMap;
import java.util.TreeMap;

import org.jenkinsci.test.acceptance.junit.Bug;
import org.jenkinsci.test.acceptance.junit.SmokeTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.plugins.analysis_core.AnalysisConfigurator;
import org.jenkinsci.test.acceptance.plugins.dashboard_view.DashboardView;
import org.jenkinsci.test.acceptance.plugins.maven.MavenModuleSet;
import org.jenkinsci.test.acceptance.plugins.pmd.PmdAction;
import org.jenkinsci.test.acceptance.plugins.pmd.PmdColumn;
import org.jenkinsci.test.acceptance.plugins.pmd.PmdFreestyleSettings;
import org.jenkinsci.test.acceptance.plugins.pmd.PmdMavenSettings;
import org.jenkinsci.test.acceptance.plugins.pmd.PmdWarningsPortlet;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.Build.Result;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.ListView;
import org.jenkinsci.test.acceptance.po.Node;
import org.jenkinsci.test.acceptance.po.PageObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.jenkinsci.test.acceptance.Matchers.*;

/**
 * Acceptance tests for the PMD plugin.
 *
 * @author Martin Kurz
 * @author Fabian Trampusch
 * @author Ullrich Hafner
 */
@WithPlugins("pmd")
public class PmdPluginTest extends AbstractAnalysisTest {
    private static final String PLUGIN_ROOT = "/pmd_plugin/";
    private static final String PATTERN_WITHOUT_WARNINGS = "pmd.xml";
    private static final String FILE_WITHOUT_WARNINGS = PLUGIN_ROOT + PATTERN_WITHOUT_WARNINGS;
    private static final String PATTERN_WITH_9_WARNINGS = "pmd-warnings.xml";
    private static final String FILE_WITH_9_WARNINGS = PLUGIN_ROOT + PATTERN_WITH_9_WARNINGS;

    /**
     * Checks that the plug-in sends a mail after a build has been failed. The content of the mail
     * contains several tokens that should be expanded in the mail with the correct values.
     */
    @Test  @Bug("25501") @WithPlugins("email-ext")
    public void should_send_mail_with_expanded_tokens() {
        setUpMailer();

        FreeStyleJob job = createFreeStyleJob(FILE_WITH_9_WARNINGS, new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.setBuildFailedTotalAll("0");
                settings.pattern.set(PATTERN_WITH_9_WARNINGS);
            }
        });

        configureEmailNotification(job, "PMD: ${PMD_RESULT}",
                "PMD: ${PMD_COUNT}-${PMD_FIXED}-${PMD_NEW}");

        job.startBuild().shouldFail();

        verifyReceivedMail("PMD: FAILURE", "PMD: 9-0-9");
    }

    /**
     * Configures a job with PMD and checks that the parsed PMD file does not contain warnings.
     */
    @Test
    public void should_find_no_warnings() {
        FreeStyleJob job = createFreeStyleJob(new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.pattern.set(PATTERN_WITHOUT_WARNINGS);
            }
        });

        Build lastBuild = buildJobWithSuccess(job);
        assertThatBuildHasNoWarnings(lastBuild);
    }

    private void assertThatBuildHasNoWarnings(final Build lastBuild) {
        assertThat(lastBuild.open(), hasContent("0 warnings"));
    }

    private FreeStyleJob createFreeStyleJob() {
        return createFreeStyleJob(FILE_WITH_9_WARNINGS, new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.pattern.set(PATTERN_WITH_9_WARNINGS);
            }
        });
    }

    private FreeStyleJob createFreeStyleJob(final AnalysisConfigurator<PmdFreestyleSettings> buildConfigurator) {
        return createFreeStyleJob(FILE_WITHOUT_WARNINGS, buildConfigurator);
    }

    private FreeStyleJob createFreeStyleJob(final String fileName, final AnalysisConfigurator<PmdFreestyleSettings> buildConfigurator) {
        return setupJob(fileName, FreeStyleJob.class, PmdFreestyleSettings.class, buildConfigurator);
    }

    /**
     * Checks that PMD runs even if the build failed if the property 'canRunOnFailed' is set.
     */
    @Test
    public void should_collect_warnings_even_if_build_failed() {
        FreeStyleJob job = createFreeStyleJob(new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.pattern.set(PATTERN_WITHOUT_WARNINGS);
                settings.setCanRunOnFailed(true);
            }
        });

        job.configure();
        job.addShellStep("false");
        job.save();

        Build lastBuild = job.startBuild().waitUntilFinished().shouldFail();
        assertThatBuildHasNoWarnings(lastBuild);
    }

    /**
     * Configures a job with PMD and checks that the parsed PMD file contains 9 warnings.
     */
    @Test
    public void should_report_details_in_different_tabs() {
        FreeStyleJob job = createFreeStyleJob(FILE_WITH_9_WARNINGS, new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.pattern.set(PATTERN_WITH_9_WARNINGS);
            }
        });

        Build lastBuild = buildJobWithSuccess(job);
        assertThatPageHasPmdResults(lastBuild);

        lastBuild.open();

        PmdAction action = new PmdAction(job);
        assertThat(action.getResultLinkByXPathText("9 warnings"), is("pmdResult"));
        assertThat(action.getResultLinkByXPathText("9 new warnings"), is("pmdResult/new"));
        assertThat(action.getWarningNumber(), is(9));
        assertThat(action.getNewWarningNumber(), is(9));
        assertThat(action.getFixedWarningNumber(), is(0));
        assertThat(action.getHighWarningNumber(), is(0));
        assertThat(action.getNormalWarningNumber(), is(3));
        assertThat(action.getLowWarningNumber(), is(6));
        assertThatFilesTabIsCorrectlyFilled(action);
        assertThatTypesTabIsCorrectlyFilled(action);
        assertThatWarningsTabIsCorrectlyFilled(action);
    }

    private void assertThatFilesTabIsCorrectlyFilled(PmdAction pa) {
        SortedMap<String, Integer> expectedContent = new TreeMap<>();
        expectedContent.put("ChannelContentAPIClient.m", 6);
        expectedContent.put("ProductDetailAPIClient.m", 2);
        expectedContent.put("ViewAllHoldingsAPIClient.m", 1);
        assertThat(pa.getFileTabContents(), is(expectedContent));
    }

    private void assertThatTypesTabIsCorrectlyFilled(PmdAction pa) {
        SortedMap<String, Integer> expectedContent = new TreeMap<>();
        expectedContent.put("long line", 6);
        expectedContent.put("unused method parameter", 3);
        assertThat(pa.getTypesTabContents(), is(expectedContent));
    }

    private void assertThatWarningsTabIsCorrectlyFilled(PmdAction pa) {
        SortedMap<String, Integer> expectedContent = new TreeMap<>();
        expectedContent.put("ChannelContentAPIClient.m:28", 28);
        expectedContent.put("ChannelContentAPIClient.m:28", 28);
        expectedContent.put("ChannelContentAPIClient.m:28", 28);
        expectedContent.put("ChannelContentAPIClient.m:32", 32);
        expectedContent.put("ChannelContentAPIClient.m:36", 36);
        expectedContent.put("ChannelContentAPIClient.m:40", 40);
        expectedContent.put("ProductDetailAPIClient.m:37", 37);
        expectedContent.put("ProductDetailAPIClient.m:38", 38);
        expectedContent.put("ViewAllHoldingsAPIClient.m:23", 23);
        assertThat(pa.getWarningsTabContents(), is(expectedContent));
    }

    /**
     * Builds a job and tests if the PMD api (with depth=0 parameter set) responds with the expected output.
     * Difference in whitespaces are ok.
     */
    @Test @Category(SmokeTest.class)
    public void should_return_results_via_remote_api() {
        FreeStyleJob job = createFreeStyleJob();
        Build build = buildJobWithSuccess(job);
        assertXmlApiMatchesExpected(build, "pmdResult/api/xml?depth=0", PLUGIN_ROOT + "api_depth_0.xml");
    }

    /**
     * Runs job two times to check if new and fixed warnings are displayed.
     */
    @Test
    public void should_report_new_and_fixed_warnings_in_consecutive_builds() {
        FreeStyleJob job = createFreeStyleJob();
        buildJobAndWait(job);

        editJob(PLUGIN_ROOT + "forSecondRun/pmd-warnings.xml", false, job);
        Build lastBuild = buildJobWithSuccess(job);
        assertThatPageHasPmdResults(lastBuild);
        lastBuild.open();

        PmdAction action = new PmdAction(job);
        assertThat(action.getResultLinkByXPathText("8 warnings"), is("pmdResult"));
        assertThat(action.getResultLinkByXPathText("1 new warning"), is("pmdResult/new"));
        assertThat(action.getResultLinkByXPathText("1 fixed warning"), is("pmdResult/fixed"));
        assertThat(action.getWarningNumber(), is(8));
        assertThat(action.getNewWarningNumber(), is(1));
        assertThat(action.getFixedWarningNumber(), is(1));
        assertThat(action.getHighWarningNumber(), is(0));
        assertThat(action.getNormalWarningNumber(), is(2));
        assertThat(action.getLowWarningNumber(), is(6));
    }

    /**
     * Runs job two times to check if the links of the graph are relative.
     */
    @Test @Bug("21723")
    public void should_have_relative_graph_links() {
        FreeStyleJob job = createFreeStyleJob();
        buildJobAndWait(job);
        editJob(PLUGIN_ROOT + "forSecondRun/pmd-warnings.xml", false, job);
        buildJobWithSuccess(job);

        assertAreaLinksOfJobAreLike(job, "pmd");
    }

    /**
     * Runs a job with warning threshold configured once and validates that build is marked as unstable.
     */
    @Test @Bug("19614")
    public void should_set_build_to_unstable_if_total_warnings_threshold_set() {
        FreeStyleJob job = createFreeStyleJob(FILE_WITH_9_WARNINGS, new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.pattern.set(PATTERN_WITH_9_WARNINGS);
                settings.setBuildUnstableTotalAll("0");
                settings.setNewWarningsThresholdFailed("0");
                settings.setUseDeltaValues(true);
            }
        });

        buildJobAndWait(job).shouldBeUnstable();
    }

    private MavenModuleSet createMavenJob() {
        return createMavenJob(null);
    }

    private MavenModuleSet createMavenJob(AnalysisConfigurator<PmdMavenSettings> configurator) {
        String projectPath = PLUGIN_ROOT + "sample_pmd_project";
        String goal = "clean package pmd:pmd";
        return setupMavenJob(projectPath, goal, PmdMavenSettings.class, configurator);
    }

    /**
     * Builds a freestyle project and checks if new warning are displayed.
     */
    @Test
    public void should_link_to_source_code_in_real_project() {
        AnalysisConfigurator<PmdFreestyleSettings> buildConfigurator = new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.pattern.set("target/pmd.xml");
            }
        };
        FreeStyleJob job = setupJob(PLUGIN_ROOT + "sample_pmd_project", FreeStyleJob.class, PmdFreestyleSettings.class, buildConfigurator, "clean package pmd:pmd"
        );
        Build lastBuild = buildJobWithSuccess(job);
        assertThatPageHasPmdResults(lastBuild);
        lastBuild.open();
        PmdAction pmd = new PmdAction(job);
        assertThat(pmd.getNewWarningNumber(), is(2));
        SortedMap<String, Integer> expectedContent = new TreeMap<>();
        expectedContent.put("Main.java:9", 9);
        expectedContent.put("Main.java:13", 13);

        verifySourceLine(pmd, "Main.java", 13,
                "13         if(false) {",
                "Do not use if statements that are always true or always false.");
    }

    /**
     * Builds a maven project and checks if new warnings are displayed.
     */
    @Test
    public void should_retrieve_results_from_maven_job() {
        MavenModuleSet job = createMavenJob();
        Build lastBuild = buildJobWithSuccess(job);
        assertThatPageHasPmdResults(lastBuild);
        lastBuild.open();
        PmdAction pmd = new PmdAction(job);
        assertThat(pmd.getNewWarningNumber(), is(2));
    }

    private void assertThatPageHasPmdResults(final PageObject page) {
        assertThat(page, hasAction("PMD Warnings"));
    }

    /**
     * Builds a maven project and checks if it is unstable.
     */
    @Test
    public void should_set_result_to_unstable_if_warning_found() {
        MavenModuleSet job = createMavenJob(new AnalysisConfigurator<PmdMavenSettings>() {
            @Override
            public void configure(PmdMavenSettings settings) {
                settings.setBuildUnstableTotalAll("0");
            }
        });
        buildJobAndWait(job).shouldBeUnstable();
    }

    /**
     * Builds a maven project and checks if it failed.
     */
    @Test
    public void should_set_result_to_failed_if_warning_found() {
        MavenModuleSet job = createMavenJob(new AnalysisConfigurator<PmdMavenSettings>() {
            @Override
            public void configure(PmdMavenSettings settings) {
                settings.setBuildFailedTotalAll("0");
            }
        });
        buildJobAndWait(job).shouldFail();
    }

    /**
     * Builds a job on a slave with pmd and verifies that the information pmd provides in the tabs about the build
     * are the information we expect.
     */
    @Test
    public void should_retrieve_results_from_slave() throws Exception {
        FreeStyleJob job = createFreeStyleJob();
        Node slave = makeASlaveAndConfigureJob(job);
        Build build = buildJobOnSlaveWithSuccess(job, slave);

        assertThat(build.getNode(), is(slave));
        assertThatPageHasPmdResults(build);
        assertThatPageHasPmdResults(job);
    }

    /**
     * Sets up a list view with a warnings column. Builds a job and checks if the column shows the correct number of
     * warnings and provides a direct link to the actual warning results.
     */
    @Test @Bug("24436")
    public void should_set_warnings_count_in_list_view_column() {
        MavenModuleSet job = createMavenJob();
        buildJobAndWait(job).shouldSucceed();

        ListView view = addDashboardListViewColumn(PmdColumn.class);
        assertValidLink(job.name);
        view.delete();
    }

    /**
     * Sets up a dashboard view with a warnings-per-project portlet. Builds a job and checks if the portlett shows the
     * correct number of warnings and provides a direct link to the actual warning results.
     */
    @Test @WithPlugins("dashboard-view")
    public void should_set_warnings_count_in_dashboard_portlet() {
        MavenModuleSet job = createMavenJob();
        buildJobAndWait(job).shouldSucceed();

        DashboardView view = addDashboardViewAndBottomPortlet(PmdWarningsPortlet.class);
        assertValidLink(job.name);
        view.delete();
    }

    private void assertValidLink(final String jobName) {
        By warningsLinkMatcher = by.css("a[href$='job/" + jobName + "/pmd']");

        assertThat(jenkins.all(warningsLinkMatcher).size(), is(1));
        WebElement link = jenkins.getElement(warningsLinkMatcher);
        assertThat(link.getText().trim(), is("2"));

        link.click();
        assertThat(driver, hasContent("PMD Result"));
    }

    /**
     * Creates a sequence of freestyle builds and checks if the build result is set correctly. New warning threshold is
     * set to zero, e.g. a new warning should mark a build as unstable.
     * <p/>
     * <ol>
     *     <li>Build 1: 1 new warning (SUCCESS since no reference build is set)</li>
     *     <li>Build 2: 2 new warnings (UNSTABLE since threshold is reached)</li>
     *     <li>Build 3: 1 new warning (UNSTABLE since still one warning is new based on delta with reference build)</li>
     *     <li>Build 4: 1 new warning (SUCCESS since there are no warnings)</li>
     * </ol>
     */
    @Test
    public void should_set_result_in_build_sequence_when_comparing_to_reference_build() {
        FreeStyleJob job = createFreeStyleJob();

        runBuild(job, 1, Result.SUCCESS, 1, false);
        runBuild(job, 2, Result.UNSTABLE, 2, false);
        runBuild(job, 3, Result.UNSTABLE, 1, false);
        runBuild(job, 4, Result.SUCCESS, 0, false);
    }

    /**
     * Creates a sequence of freestyle builds and checks if the build result is set correctly. New warning threshold is
     * set to zero, e.g. a new warning should mark a build as unstable.
     * <p/>
     * <ol>
     *     <li>Build 1: 1 new warning (SUCCESS since no reference build is set)</li>
     *     <li>Build 2: 2 new warnings (UNSTABLE since threshold is reached)</li>
     *     <li>Build 3: 1 new warning (SUCCESS since all warnings of previous build are fixed)</li>
     * </ol>
     */
    @Test @Bug("13458")
    public void should_set_result_in_build_sequence_when_comparing_to_previous_build() {
        FreeStyleJob job = createFreeStyleJob();

        runBuild(job, 1, Result.SUCCESS, 1, true);
        runBuild(job, 2, Result.UNSTABLE, 2, true);
        runBuild(job, 3, Result.SUCCESS, 0, true);
    }

    private void runBuild(final FreeStyleJob job, final int number, final Result expectedResult, final int expectedNewWarnings, final boolean usePreviousAsReference) {
        final String fileName = "pmd-warnings-build" + number + ".xml";
        AnalysisConfigurator<PmdFreestyleSettings> buildConfigurator = new AnalysisConfigurator<PmdFreestyleSettings>() {
            @Override
            public void configure(PmdFreestyleSettings settings) {
                settings.setNewWarningsThresholdUnstable("0", usePreviousAsReference);
                settings.pattern.set(fileName);
            }
        };

        editJob(PLUGIN_ROOT + fileName, false, job,
                PmdFreestyleSettings.class, buildConfigurator);
        Build lastBuild = buildJobAndWait(job).shouldBe(expectedResult);

        if (expectedNewWarnings > 0) {
            assertThatPageHasPmdResults(lastBuild);
            lastBuild.open();
            PmdAction pmd = new PmdAction(job);
            assertThat(pmd.getNewWarningNumber(), is(expectedNewWarnings));
        }
    }
}
