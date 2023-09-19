package com.vectorcast.plugins.vectorcastcoverage.portlet;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import com.vectorcast.plugins.vectorcastcoverage.VectorCASTPublisher;
import com.vectorcast.plugins.vectorcastcoverage.portlet.bean.VectorCASTCoverageResultSummary;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.util.DescribableList;
import hudson.tasks.BuildWrapper;
import hudson.model.Descriptor;
import java.io.Serializable;
import static org.junit.Assert.*;

/**
 * Tests {@link com.vectorcast.plugins.vectorcastcoverage.portlet.VectorCASTLoadData} in a Hudson environment.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 * @author Mauro Durante Junior (Mauro.Durantejunior@sonyericsson.com)
 */
public class VectorCASTLoadDataHudsonTest extends JenkinsRule {

    /**
     * This method tests loadChartDataWithinRange() when it has positive number of days.
     * Tests {@link com.vectorcast.plugins.vectorcastcoverage.portlet.VectorCASTLoadData#loadChartDataWithinRange(java.util.List, int)}.
     *
     * @throws Exception if so.
     */
    public void testLoadChartDataWithinRangePositiveNumberOfDays() throws Exception {

        final float expectedStatementCoverage = 41.6f;
        final float expectedMCDCCoverage = 7.1f;
        final int numberOfDays = 1;
        final int summaryMapSize = 1;

        //Create the project
        FreeStyleProject job1 = createFreeStyleProject("job1");

        //Make it do something, in this case it writes a coverage report to the workspace.
        job1.getBuildersList().add(
          new CopyResourceToWorkspaceBuilder(getClass().getResourceAsStream("/com/vectorcast/plugins/vectorcastcoverage/portlet.xml"),
                        "reports/coverage/portlet.xml"));
        //Add a VectorCAST publisher
        VectorCASTPublisher vcPublisher = new VectorCASTPublisher();
        vcPublisher.includes = "reports/coverage/portlet.xml";
        job1.getPublishersList().add(vcPublisher);
        //Build it
        job1.scheduleBuild2(0).get();

        //Do the test
        List<Job> jobs = new LinkedList<Job>();
        jobs.add(job1);
        //Verify the result
        Map<LocalDate, VectorCASTCoverageResultSummary> summaryMap = VectorCASTLoadData.loadChartDataWithinRange(jobs, numberOfDays);

        // Testing the size of the returned map against the exepected value,
        // which is a non-zero, therefore tha map must not be empty
        assertEquals(summaryMapSize, summaryMap.size());

        VectorCASTCoverageResultSummary summary = summaryMap.entrySet().iterator().next().getValue();
        
        // Test evaluated values against expected ones
        assertEquals(expectedStatementCoverage, summary.getStatementCoverage(), 0.1f);
        assertEquals(expectedMCDCCoverage, summary.getMCDCCoverage(), 0.1f);
    }

    /**
     * This method tests loadChartDataWithinRange() when it has multiple jobs and a single build.
     * Tests {@link com.vectorcast.plugins.vectorcastcoverage.portlet.VectorCASTLoadData#loadChartDataWithinRange(java.util.List, int)}.
     *
     * @throws Exception if so.
     */
    public void testLoadChartDataWithinRangeMultJobsSingleBuild() throws Exception {

        final float expectedStatementCoverage = 41.6f;
        final float expectedMCDCCoverage = 7.1f;
        final int numberOfDays = 1;
        final int summaryMapSize = 1;

        //Create the project
        FreeStyleProject job1 = createFreeStyleProject("job1");

        //Make it do something, in this case it writes a coverage report to the workspace.
        job1.getBuildersList().add(
                new CopyResourceToWorkspaceBuilder(getClass().getResourceAsStream("/com/vectorcast/plugins/vectorcastcoverage/portlet.xml"),
                        "reports/coverage/portlet.xml"));        
        
        //Add a VectorCAST publisher
        VectorCASTPublisher vcPublisher = new VectorCASTPublisher();
        vcPublisher.includes = "reports/coverage/portlet.xml";
        job1.getPublishersList().add(vcPublisher);
        //Build it
        job1.scheduleBuild2(0).get();

        //Do the test
        List<Job> jobs = new LinkedList<Job>();

        FreeStyleProject job2 = createFreeStyleProject("job2");
        jobs.add(job1);
        jobs.add(job2);

        //Verify the result
        Map<LocalDate, VectorCASTCoverageResultSummary> summaryMap = VectorCASTLoadData.loadChartDataWithinRange(jobs, numberOfDays);

        // Testing the size of the returned map against the exepected value,
        // which is a non-zero, therefore tha map must not be empty
        assertEquals(summaryMapSize, summaryMap.size());

        VectorCASTCoverageResultSummary summary = summaryMap.entrySet().iterator().next().getValue();
        // Test evaluated values against expected ones
        assertEquals(expectedStatementCoverage, summary.getStatementCoverage(), 0.1f);
        assertEquals(expectedMCDCCoverage, summary.getMCDCCoverage(), 0.1f);
    }

    /**
     * This method tests the getResultSummary() behaviour.
     * Tests {@link com.vectorcast.plugins.vectorcastcoverage.portlet.VectorCASTLoadData#getResultSummary(java.util.Collection)}.
     * @throws Exception if any
     */
    public void testGetResultSummary() throws Exception {

        float basisPathCoverage = 12.0f;
        float MCDCCoverage = 78.0f;
        float branchCoverage = 82.0f;
        float statementCoverage = 0.7f;
        float functionCoverage = 0.8f;
        float functionCallCoverage = 0.55f;
        float complexity = 17.0f;

        float basisPathCoverage2 = 29.3f;
        float MCDCCoverage2 = 3.0f;
        float branchCoverage2 = 17.0f;
        float statementCoverage2 = 34.7f;
        float functionCoverage2 = 28.8f;
        float functionCallCoverage2 = 9.5f;
        float complexity2 = 12.5f;

        // create a result summary with data from the first VC action
        VectorCASTCoverageResultSummary coverageResultSummary = 
                new VectorCASTCoverageResultSummary(null,
                                                    basisPathCoverage,
                                                    MCDCCoverage,
                                                    branchCoverage,
                                                    statementCoverage,
                                                    functionCoverage,
                                                    functionCallCoverage,
                                                    complexity);

        // create a result summary with data from the second VC action
        VectorCASTCoverageResultSummary coverageResultSummary2 =
                new VectorCASTCoverageResultSummary(null,
                                                    basisPathCoverage2,
                                                    MCDCCoverage2,
                                                    branchCoverage2,
                                                    statementCoverage2,
                                                    functionCoverage2,
                                                    functionCallCoverage2,
                                                    complexity2);

        // add both coverage result summaries to the VC result summary
        VectorCASTCoverageResultSummary summary = new VectorCASTCoverageResultSummary();
        summary.addCoverageResult(coverageResultSummary);
        summary.addCoverageResult(coverageResultSummary2);

        // assert the sum has occurred correctly
        assertEquals(basisPathCoverage + basisPathCoverage2, summary.getBasisPathCoverage());
        assertEquals(MCDCCoverage + MCDCCoverage2, summary.getMCDCCoverage());
        assertEquals(branchCoverage + branchCoverage2, summary.getBranchCoverage());
        assertEquals(statementCoverage + statementCoverage2, summary.getStatementCoverage());
        assertEquals(functionCoverage + functionCoverage2, summary.getFunctionCoverage());
        assertEquals(functionCallCoverage + functionCallCoverage2, summary.getFunctionCallCoverage());
        assertEquals(complexity + complexity2, summary.getComplexity());
    }

    /**
     * Test utility class.
     * A Builder that writes some data into a file in the workspace.
     */
    static class CopyResourceToWorkspaceBuilder extends Builder {

        private final InputStream content;
        private final String fileName;
        
        /**
         * Default constructor.
         *
         * @param content  the content to write to the file.
         * @param fileName the name of the file relative to the workspace.
         */
        CopyResourceToWorkspaceBuilder(InputStream content, String fileName) {
            this.content = content;
            this.fileName = fileName;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            FilePath path = build.getWorkspace().child(fileName);
            path.copyFrom(content);
            return true;
        }
    }
}
