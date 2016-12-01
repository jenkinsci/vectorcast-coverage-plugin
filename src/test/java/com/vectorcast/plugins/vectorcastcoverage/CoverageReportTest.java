package com.vectorcast.plugins.vectorcastcoverage;

/**
 * @author Kohsuke Kawaguchi
 */
public class CoverageReportTest extends AbstractVectorCASTTestBase {

    public void testLoad() throws Exception {
        CoverageReport r = new CoverageReport(null, 
                                              getClass().getResourceAsStream("coverage.xml"),
                                              getClass().getResourceAsStream("top-level.xml"));
        EnvironmentReport env = r.getChildren().get("VectorCAST_MinGW_C_TestSuite_ORDER");
        assertRatio(env.getStatementCoverage(), 23, 55);
        assertEquals(45f, r.getStatementCoverage().getNumerator());
    }

    public void testLoadMultipleReports() throws Exception {
      CoverageReport r = new CoverageReport(null,  
          getClass().getResourceAsStream("coverage.xml"), 
          getClass().getResourceAsStream("coverageh.xml"),
          getClass().getResourceAsStream("top-level.xml"));

      assertRatio(r.getStatementCoverage(), 45, 60);

      EnvironmentReport env = r.getChildren().get("VectorCAST_MinGW_C_TestSuite_ORDER");
      assertRatio(env.getStatementCoverage(), 23, 55);

      env = r.getChildren().get("AnotherEnv");
      assertRatio(env.getStatementCoverage(), 20, 40);

    }

    public void testTreeReport() throws Exception {
        CoverageReport r = new CoverageReport(null,getClass().getResourceAsStream("coverage.xml"));
        assertRatio(r.getStatementCoverage(), 25, 60);

        EnvironmentReport env = r.getChildren().get("VectorCAST_MinGW_C_TestSuite_ORDER");
        assertRatio(env.getStatementCoverage(),23,55);

        UnitReport unit = env.getChildren().get("manager");
        assertRatio(unit.getStatementCoverage(), 18, 44);

        SubprogramReport sub = unit.getChildren().get("Add_Included_Dessert");
        assertRatio(sub.getStatementCoverage(), 4, 8);
    }

    public void testEmptyEnvironment() throws Exception {
        CoverageReport r = new CoverageReport(null,getClass().getResourceAsStream("coverage.xml"));

        EnvironmentReport env = r.getChildren().get("EmptyEnvironment");
        assertTrue(env != null);
        assertRatio(env.getStatementCoverage(), 0, 0);
        assertFalse(env.hasChildren());
        assertFalse(env.hasChildrenStatementCoverage());

        env = r.getChildren().get("EnvironmentWithoutStatements");
        assertTrue(env != null);
        assertRatio(env.getStatementCoverage(), 0, 0);
        assertTrue(env.hasChildren());
        assertFalse(env.hasChildrenStatementCoverage());
    }
}
