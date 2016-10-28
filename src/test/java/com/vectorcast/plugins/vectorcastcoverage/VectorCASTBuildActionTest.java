package com.vectorcast.plugins.vectorcastcoverage;

/**
 * @author Kohsuke Kawaguchi
 */
public class VectorCASTBuildActionTest extends AbstractVectorCASTTestBase {
  
    public void testLoadReport1() throws Exception {
        VectorCASTBuildAction r = VectorCASTBuildAction.load(null,null,
                new VectorCASTHealthReportThresholds(30, 90, 25, 80, 20, 70, 15, 60, 20, 70, 80, 90),
                getClass().getResourceAsStream("coverage.xml"));
        assertEquals(42, r.Statement.getPercentage());
        assertEquals(26, r.Branch.getPercentage());
        assertRatio(r.Statement, 25, 60);
        assertRatio(r.Branch, 14, 54);
        assertRatio(r.MCDC, 1, 14);
        assert(r.Function == null);
        assertEquals("Coverage: Statement 25/60 (42%). Branch 14/54 (26%). MC/DC 1/14 (7%).   ",
                     r.getBuildHealth().getDescription());
    }
    
    public void testLoadReport2() throws Exception {
        VectorCASTBuildAction r = VectorCASTBuildAction.load(null,null,
                new VectorCASTHealthReportThresholds(30, 90, 25, 80, 20, 70, 15, 60, 20, 70, 80, 90),
                getClass().getResourceAsStream("coverageh.xml"));
        assertEquals(50, r.Statement.getPercentage());
        assertEquals(25, r.Branch.getPercentage());
        assertRatio(r.Statement, 20, 40);
        assertRatio(r.Branch, 4, 16);
        assertRatio(r.MCDC, 1, 5);
        assert(r.Function == null);
        assertEquals("Coverage: Statement 20/40 (50%). Branch 4/16 (25%). MC/DC 1/5 (20%).   ",
                     r.getBuildHealth().getDescription());
    }
    
    public void testLoadMultipleReports() throws Exception {
      VectorCASTBuildAction r = VectorCASTBuildAction.load(null,null,
              new VectorCASTHealthReportThresholds(30, 90, 25, 80, 20, 70, 15, 60, 20, 70, 80, 90),
              getClass().getResourceAsStream("coverage.xml"), 
              getClass().getResourceAsStream("coverageh.xml"));
      assertEquals(45, r.Statement.getPercentage());
      assertEquals(26, r.Branch.getPercentage());
      assertRatio(r.Statement, 45, 100);
      assertRatio(r.Branch, 18, 70);
      assertRatio(r.MCDC, 2, 19);
      assert(r.Function == null);
      assertEquals("Coverage: Statement 45/100 (45%). Branch 18/70 (26%). MC/DC 2/19 (11%).   ",
                   r.getBuildHealth().getDescription());
  }
}
