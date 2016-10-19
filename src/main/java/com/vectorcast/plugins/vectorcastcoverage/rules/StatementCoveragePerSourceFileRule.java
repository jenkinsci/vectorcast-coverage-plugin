package com.vectorcast.plugins.vectorcastcoverage.rules;

import hudson.model.TaskListener;
import com.vectorcast.plugins.vectorcastcoverage.Rule;
import com.vectorcast.plugins.vectorcastcoverage.CoverageReport;
import com.vectorcast.plugins.vectorcastcoverage.EnvironmentReport;
import com.vectorcast.plugins.vectorcastcoverage.UnitReport;

/**
 * Flags a failure if the Statement coverage of a source file
 * goes below a certain threshold.
 */
public class StatementCoveragePerSourceFileRule extends Rule {

    private static final long serialVersionUID = -2869893039051762047L;

    private final float minPercentage;

    public StatementCoveragePerSourceFileRule(float minPercentage) {
        this.minPercentage = minPercentage;
    }

    public void enforce(CoverageReport report, TaskListener listener) {
        for (EnvironmentReport env : report.getChildren().values()) {
            for (UnitReport unitReport : env.getChildren().values()) {
                float percentage = unitReport.getStatementCoverage().getPercentageFloat();

                if (percentage < minPercentage) {
                    listener.getLogger().println("VCASTCoverage: " + unitReport.getDisplayName() + " failed (below " + minPercentage + "%).");
                    unitReport.setFailed();
                }
            }
        }
    }
}
