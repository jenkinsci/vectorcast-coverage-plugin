package com.vectorcast.plugins.vectorcastcoverage;

import hudson.model.Action;
import javax.servlet.ServletOutputStream;
import org.htmlunit.WebResponse;
import org.junit.Test;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.model.FreeStyleProject;
import hudson.model.FreeStyleBuild;
import org.htmlunit.Page;
import jenkins.model.TransientActionFactory;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

/**
 * @author Manuel Carrasco
 */
public class CoverageObjectTest extends AbstractVectorCASTTestBase {

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Test
	public void testSomething()  throws Exception {
		assertEquals(10, 5 + 5);
	}

	@Test
	public void debugDoGraph() throws Exception {
		// Load your VectorCASTBuildAction the normal way
		VectorCASTBuildAction action = VectorCASTBuildAction.load(
				null,
				null,
				new VectorCASTHealthReportThresholds(
						30, 90, 25, 80, 20, 70, 15, 60,
						20, 70, 80, 90
				),
				getClass().getResourceAsStream("coverage.xml")
		);

		// Create mocks for StaplerRequest and StaplerResponse
		StaplerRequest req = Mockito.mock(StaplerRequest.class);
		StaplerResponse rsp = Mockito.mock(StaplerResponse.class);

		// Provide width & height parameters for doGraph()
		when(req.getParameter("width")).thenReturn("500");
		when(req.getParameter("height")).thenReturn("200");

		// Capture PNG output using an OutputStream in a custom ServletOutputStream
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ServletOutputStream out = new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				buffer.write(b);
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(javax.servlet.WriteListener writeListener) {
				// no-op
			}
		};

		when(rsp.getOutputStream()).thenReturn(out);

		// Provide a build context because doGraph() calls getBuild()
		FreeStyleProject p = j.createFreeStyleProject();
		FreeStyleBuild build = p.scheduleBuild2(0).get();
		action.onAttached(build);   // IMPORTANT: simulates Jenkins attaching the action

		// 🔥🔥🔥 BREAKPOINT HERE to step into doGraph()
		action.doGraph(req, rsp);

		// Validate we got PNG bytes
		byte[] pngBytes = buffer.toByteArray();
		assertTrue("Graph output should not be empty", pngBytes.length > 0);

		// Optional: write bytes to /tmp for visual inspection
		// Files.write(Paths.get("/tmp/debug_graph.png"), pngBytes);
	}

	// Register a test-only TransientActionFactory
	@TestExtension("testDoGraph")
	public static class MyFactory extends TransientActionFactory<FreeStyleProject> {

		@Override
		public Class<FreeStyleProject> type() {
			return FreeStyleProject.class;
		}

		@Override
		public Collection<? extends Action> createFor(FreeStyleProject project) {
			// Return your action here
			float oneHundy = (float) 100.0f;
			Ratio r = new Ratio(oneHundy, oneHundy);
			Ratio vg = new Ratio(oneHundy, 0);
			VectorCASTHealthReportThresholds thresholds = new VectorCASTHealthReportThresholds(
			    80, 80, 80, 80, 80,
				80, 80, 80,
				80, 80, 80, 80);

			return Collections.singleton(new VectorCASTBuildAction(null, null, r, r, r, r, r, r, vg, thresholds));
		}
	}


	@Test
	public void testPrintRatioTable() throws Exception {

    	Ratio r = null;
    	StringBuilder b = new StringBuilder();

    	r = new Ratio(0,100);
    	b = new StringBuilder();
    	CoverageObject.printRatioTable(r, b);
    	assertEquals("<table class='percentgraph' cellpadding='0px' cellspacing='0px'><tr class='percentgraph'><td width='64px' class='data'>0.0%</td><td class='percentgraph'><div class='percentgraph'><div class='greenbar' style='width: 0.0px;'><span class='text'>0/100</span></div></div></td></tr></table>", b.toString());

    	r = new Ratio(51,200);
    	b = new StringBuilder();
    	CoverageObject.printRatioTable(r, b);
    	assertEquals("<table class='percentgraph' cellpadding='0px' cellspacing='0px'><tr class='percentgraph'><td width='64px' class='data'>25.5%</td><td class='percentgraph'><div class='percentgraph'><div class='greenbar' style='width: 25.5px;'><span class='text'>51/200</span></div></div></td></tr></table>", b.toString());

    }

	@Test
    public void testPrintColumn() throws Exception {

    	Ratio r = null;
    	StringBuilder b = new StringBuilder();
    	CoverageObject.printRatioCell(true, null, b);
        assertTrue(b.toString().contains("<td align=\"center\" >-</td>"));

    	r = new Ratio(0,100);
    	b = new StringBuilder();
    	CoverageObject.printRatioCell(true, r, b);
    	assertTrue(b.toString().contains("'nowrap red'"));

    	r = new Ratio(0,100);
    	b = new StringBuilder();
    	CoverageObject.printRatioCell(false, r, b);
    	assertTrue(b.toString().contains("'nowrap'"));

    	r = new Ratio(51,200);
    	b = new StringBuilder();
    	CoverageObject.printRatioCell(false, r, b);
    	assertEquals("<td align=\"center\" class='nowrap' data='025.50'>\n" +
    			"<table class='percentgraph' cellpadding='0px' cellspacing='0px'><tr class='percentgraph'><td width='64px' class='data'>25.5%</td><td class='percentgraph'><div class='percentgraph'><div class='greenbar' style='width: 25.5px;'><span class='text'>51/200</span></div></div></td></tr></table></td>\n", b.toString());

    }
 
}
