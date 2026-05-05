package com.vectorcast.plugins.vectorcastcoverage.utils;

import com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Utils;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test {@link com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Utils}
 * through HudsonTestCase extension.
 *
 * @author Mauro Durante Junior &lt;Mauro.Durantejunior@sonyericsson.com&gt;
 */
public class UtilsHudsonTest {

  /**
   * Tests {@link com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Utils#getLastDate(java.util.List) }.
   * @throws Exception on any exception occurrence.
   */

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void testGetLastDate() throws Exception {

    FreeStyleProject prj = j.createFreeStyleProject("prj1");
    prj.scheduleBuild2(0).get();
    FreeStyleProject prj2 = j.createFreeStyleProject("prj2");
    prj2.scheduleBuild2(0).get();

    List<Job> jobs = new ArrayList<Job>();
    jobs.add(prj);
    jobs.add(prj2);

    LocalDate lastDate = Utils.getLastDate(jobs);
    assertNotNull(lastDate);
  }

  /**
   * Tests {@link com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Utils#roundFLoat(int scale, int roundingMode, Float value) }.
   */
  @Test
  public void testRoundFloat() {
    int scale = 1;
    RoundingMode roundingMode = RoundingMode.HALF_EVEN;
    final Float value = 9.987f;
    final Float roundedAs = 10.0f;

    assertEquals(roundedAs, Utils.roundFLoat(scale, roundingMode.ordinal(), value));
  }
}
