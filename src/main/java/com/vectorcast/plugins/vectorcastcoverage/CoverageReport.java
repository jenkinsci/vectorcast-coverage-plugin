package com.vectorcast.plugins.vectorcastcoverage;

import hudson.model.AbstractBuild;
import hudson.util.IOException2;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Root object of the coverage report.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class CoverageReport extends AggregatedReport<CoverageReport/*dummy*/,CoverageReport,EnvironmentReport> {
    private final VectorCASTBuildAction action;

    private CoverageReport(VectorCASTBuildAction action) {
        this.action = action;
        setName("VectorCAST");
    }

    public CoverageReport(VectorCASTBuildAction action, InputStream... xmlReports) throws IOException {
        this(action);
        for (InputStream is: xmlReports) {
          try {
            createDigester().parse(is);
          } catch (SAXException e) {
              throw new IOException2("Failed to parse XML",e);
          }
        }
        setParent(null);
    }

    public CoverageReport(VectorCASTBuildAction action, File xmlReport) throws IOException {
        this(action);
        try {
            createDigester().parse(xmlReport);
        } catch (SAXException e) {
            throw new IOException2("Failed to parse "+xmlReport,e);
        }
        setParent(null);
    }

    @Override
    public CoverageReport getPreviousResult() {
        VectorCASTBuildAction prev = action.getPreviousResult();
        if(prev!=null)
            return prev.getResult();
        else
            return null;
    }

    @Override
    public AbstractBuild<?,?> getBuild() {
        return action.owner;
    }

    /**
     * Creates a configured {@link Digester} instance for parsing report XML.
     */
    private Digester createDigester() {
        Digester digester = new Digester();
        digester.setClassLoader(getClass().getClassLoader());

        digester.push(this);

        digester.addObjectCreate( "*/environment", EnvironmentReport.class);
        digester.addSetNext(      "*/environment", "add");
        digester.addSetProperties("*/environment");
        digester.addObjectCreate( "*/unit", UnitReport.class);
        digester.addSetNext(      "*/unit","add");
        digester.addSetProperties("*/unit");
        digester.addObjectCreate( "*/subprogram", SubprogramReport.class);
        digester.addSetNext(      "*/subprogram", "add");
        digester.addSetProperties("*/subprogram");

        // Top-level (combined) coverage values
        digester.addObjectCreate( "report/combined-coverage", CoverageElement.class);
        digester.addSetProperties("report/combined-coverage");
        digester.addSetNext(      "report/combined-coverage", "addCombinedCoverage");

        digester.addObjectCreate( "*/coverage", CoverageElement.class);
        digester.addSetProperties("*/coverage");
        digester.addSetNext(      "*/coverage", "addCoverage");

        return digester;
    }
}
