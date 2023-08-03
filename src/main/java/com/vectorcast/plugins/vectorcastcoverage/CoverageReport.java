package com.vectorcast.plugins.vectorcastcoverage;

import hudson.model.Run;
import hudson.util.IOException2;
import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Root object of the coverage report.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class CoverageReport extends AggregatedReport<CoverageReport/*dummy*/,CoverageReport,EnvironmentReport> {
    
    private static final Logger logger = Logger.getLogger(CoverageReport.class.getName());

    private final VectorCASTBuildAction action;

    private CoverageReport(VectorCASTBuildAction action) {
        this.action = action;
        setName("VectorCAST");
    }

    public CoverageReport(VectorCASTBuildAction action, InputStream... xmlReports) throws IOException {
        this(action);
        int idx = 0;
        for (InputStream is: xmlReports) {
          try {
            createDigester(!Boolean.getBoolean(this.getClass().getName() + ".UNSAFE")).parse(is);
            idx += 1;
          } catch (SAXException e) {
              throw new IOException2("Failed to parse XML:" + idx,e);
          }
          
        }
        setParent(null);
    }

    public CoverageReport(VectorCASTBuildAction action, File xmlReport) throws IOException {
        this(action);
        try {
            createDigester(!Boolean.getBoolean(this.getClass().getName() + ".UNSAFE")).parse(xmlReport);
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
    public Run<?,?> getBuild() {
        return action.owner;
    }

    /**
     * Creates a configured {@link Digester} instance for parsing report XML.
     */
    private Digester createDigester(boolean secure) throws SAXException {
        Digester digester = new Digester();
        if (secure) {
            digester.setXIncludeAware(false);
            try {
                digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
                digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException ex) {
                throw new SAXException("Failed to securely configure xml digester parser", ex);
            }
        }
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
