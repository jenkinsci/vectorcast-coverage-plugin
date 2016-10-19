package com.vectorcast.plugins.vectorcastcoverage;

/**
 * @author Kohsuke Kawaguchi
 */
public class CoverageReportTest extends AbstractVectorCASTTestBase {
	
    public void testLoad() throws Exception {
//        CoverageReport r = new CoverageReport(null, getClass().getResourceAsStream("coverage.xml"));
//        EnvironmentReport env = r.getChildren().get("com.sun.tools.javac.v8.resources");
//        assertRatio(env.getLineCoverage(),3,12);
//        assertEquals(8346.3f, r.getLineCoverage().getNumerator());
    }

    public void testLoadMultipleReports() throws Exception {
//      CoverageReport r = new CoverageReport(null,  
//          getClass().getResourceAsStream("coverage.xml"), 
//          getClass().getResourceAsStream("coverageh.xml"));
//
//      assertRatio(r.getLineCoverage(), 8355.3f, 14828.0f);
//      
//      EnvironmentReport env = r.getChildren().get("com.sun.tools.javac.v8.resources");
//      assertRatio(env.getLineCoverage(),3,12);
//      
//      env = r.getChildren().get("org.apache.hupa.client.validation");
//      assertRatio(env.getLineCoverage(), 9,27);
//      
    }
    
    public void testTreeReport() throws Exception {
//        CoverageReport r = new CoverageReport(null,getClass().getResourceAsStream("coverageh.xml"));
//        assertRatio(r.getLineCoverage(), 9, 1693);
//        
//        EnvironmentReport env = r.getChildren().get("org.apache.hupa.client.validation");
//        assertRatio(env.getLineCoverage(), 9, 27);
//
//        UnitReport unit = env.getChildren().get("EmailListValidator.java");
//        assertRatio(unit.getLineCoverage(), 9, 18);
//
//        SubprogramReport sub = unit.getChildren().get("EmailListValidator");
//        assertRatio(sub.getLineCoverage(), 9, 18);
//        assertTrue(sub.hasClassCoverage());
//
//        DeadEndReport dead = sub.getChildren().get("isValidAddress (String): boolean");
//        assertRatio(dead.getLineCoverage(), 1, 1);
//        assertFalse(dead.hasClassCoverage());
//
//        mth = clz.getChildren().get("Foo (): void");
//        assertRatio(mth.getLineCoverage(), 0, 0);
//        assertFalse(mth.hasClassCoverage());
//        assertFalse(mth.hasLineCoverage());
    }
    
    public void testEmptyPackage() throws Exception {
//        CoverageReport r = new CoverageReport(null,getClass().getResourceAsStream("coverage.xml"));
//
//        EnvironmentReport env = r.getChildren().get("an.empty.package");
//        assertRatio(env.getLineCoverage(), 0, 0);
//        assertFalse(env.hasChildren());
//        assertFalse(env.hasChildrenClassCoverage());
//        assertFalse(env.hasChildrenLineCoverage());
//
//        env = r.getChildren().get("an.package.without.lines");
//        assertRatio(env.getLineCoverage(), 0, 0);
//        assertTrue(env.hasChildren());
//        assertFalse(env.hasChildrenClassCoverage());
//        assertFalse(env.hasChildrenLineCoverage());

    }
}
