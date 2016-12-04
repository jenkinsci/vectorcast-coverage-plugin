package com.vectorcast.plugins.vectorcastcoverage;

/**
 * @author Kohsuke Kawaguchi
 */
public class VectorCASTBuildActionTest extends AbstractVectorCASTTestBase {
  
    public void testLoadReport1a() throws Exception {
        VectorCASTBuildAction r = VectorCASTBuildAction.load(null,null,
                new VectorCASTHealthReportThresholds(30, 90, 25, 80, 20, 70, 15, 60, 20, 70, 80, 90),
                getClass().getResourceAsStream("top-level.xml"),
                getClass().getResourceAsStream("coverage.xml"));
        assertEquals(75, r.Statement.getPercentage());
        assertEquals(33, r.Branch.getPercentage());
        assertRatio(r.Statement, 45, 60);
        assertRatio(r.Branch, 18, 54);
        assertRatio(r.MCDC, 7, 14);
        assert(r.Function == null);
        assertEquals("Coverage: Statement 45/60 (75%). Branch 18/54 (33%). MC/DC 7/14 (50%).   ",
                     r.getBuildHealth().getDescription());
    }
    
    public void testLoadReport1b() throws Exception {
        VectorCASTBuildAction r = VectorCASTBuildAction.load(null,null,
                new VectorCASTHealthReportThresholds(30, 90, 25, 80, 20, 70, 15, 60, 20, 70, 80, 90),
                getClass().getResourceAsStream("coverage.xml"),
                getClass().getResourceAsStream("top-level.xml"));
        assertEquals(75, r.Statement.getPercentage());
        assertEquals(33, r.Branch.getPercentage());
        assertRatio(r.Statement, 45, 60);
        assertRatio(r.Branch, 18, 54);
        assertRatio(r.MCDC, 7, 14);
        assert(r.Function == null);
        assertEquals("Coverage: Statement 45/60 (75%). Branch 18/54 (33%). MC/DC 7/14 (50%).   ",
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
      assertRatio(r.Statement, 43, 95);
      assertRatio(r.Branch, 17, 66);
      assertRatio(r.MCDC, 2, 17);
      assert(r.Function == null);
      assertEquals("Coverage: Statement 43/95 (45%). Branch 17/66 (26%). MC/DC 2/17 (12%).   ",
                   r.getBuildHealth().getDescription());
  }
}
