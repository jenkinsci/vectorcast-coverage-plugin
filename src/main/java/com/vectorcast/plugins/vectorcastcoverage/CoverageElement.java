package com.vectorcast.plugins.vectorcastcoverage;

import java.io.IOException;

/**
 * This is a transitive object used during the parsing, but not a part of
 * the final tree built. 
 *
 * @author Kohsuke Kawaguchi
 */
public final class CoverageElement {
    private String type;
    private String value;

    // set by attributes
    public void setType(String type) {
        this.type = type;
    }

    // set by attributes
    public void setValue(String value) {
        this.value = value;
    }

    void addTo(AbstractReport<?,?> report) throws IOException {

    	Ratio r = null;
    	if(type.equals("statement, %")) {
    		r = report.Statement;
        } else if(type.equals("branch, %")) {
    		r = report.Branch;
        } else if(type.equals("basispath, %")) {
    		r = report.BasisPath;
        } else if(type.equals("mcdc, %")) {
    		r = report.MCDC;
        } else if(type.equals("function, %")) {
    		r = report.Function;
        } else if(type.equals("functioncall, %")) {
    		r = report.FunctionCall;
        } else if(type.equals("complexity, %")) {
    		r = report.Complexity;
        } else {
            throw new IllegalArgumentException("Invalid type: "+type);
        }
    	r.addValue(value);

    }
}
