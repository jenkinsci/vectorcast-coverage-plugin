package com.vectorcast.plugins.vectorcastcoverage;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.util.IOException2;
import hudson.util.NullStream;
import hudson.util.StreamTaskListener;

import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.StaplerProxy;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build view extension by VectorCAST plugin.
 *
 * As {@link CoverageObject}, it retains the overall coverage report.
 *
 * @author Kohsuke Kawaguchi
 */
public final class VectorCASTBuildAction extends CoverageObject<VectorCASTBuildAction> implements HealthReportingAction, StaplerProxy {
	
    public final AbstractBuild<?,?> owner;

    private transient WeakReference<CoverageReport> report;

    /**
     * Non-null if the coverage has pass/fail rules.
     */
    private final Rule rule;

    /**
     * The thresholds that applied when this build was built.
     * @TODO add ability to trend thresholds on the graph
     */
    private final VectorCASTHealthReportThresholds thresholds;

    public VectorCASTBuildAction(AbstractBuild<?,?> owner, Rule rule, Ratio StatementCoverage, Ratio BranchCoverage, Ratio BasisPathCoverage, Ratio MCDCCoverage, Ratio FunctionCoverage,  Ratio FunctionCallCoverage, Ratio Complexity, VectorCASTHealthReportThresholds thresholds) {
        this.owner = owner;
        this.rule = rule;
        this.Statement = StatementCoverage;
        this.Branch = BranchCoverage;
        this.BasisPath = BasisPathCoverage;
        this.MCDC = MCDCCoverage;
        this.Function = FunctionCoverage;
        this.FunctionCall = FunctionCallCoverage;
        this.Complexity = Complexity;
        this.thresholds = thresholds;
    }

    public String getDisplayName() {
        return Messages.BuildAction_DisplayName();
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getUrlName() {
        return "vectorcastcoverage";
    }

    /**
     * Get the coverage {@link hudson.model.HealthReport}.
     *
     * @return The health report or <code>null</code> if health reporting is disabled.
     * @since 1.7
     */
    public HealthReport getBuildHealth() {
        if (thresholds == null) {
            // no thresholds => no report
            return null;
        }
        thresholds.ensureValid();
        int score = 100, percent;
        ArrayList<Localizable> reports = new ArrayList<Localizable>(5);
        if (Statement != null && thresholds.getMaxStatement() > 0) {
            percent = Statement.getPercentage();
            if (percent < thresholds.getMaxStatement()) {
                reports.add(Messages._BuildAction_Statement(Statement, percent));
            }
            score = updateHealthScore(score, thresholds.getMinStatement(),
                                      percent, thresholds.getMaxStatement());
        }
        if (Branch != null && thresholds.getMaxBranch() > 0) {
            percent = Branch.getPercentage();
            if (percent < thresholds.getMaxBranch()) {
                reports.add(Messages._BuildAction_Branch(Branch, percent));
            }
            score = updateHealthScore(score, thresholds.getMinBranch(),
                                      percent, thresholds.getMaxBranch());
        }
        if (BasisPath != null && thresholds.getMaxBasisPath() > 0) {
            percent = BasisPath.getPercentage();
            if (percent < thresholds.getMaxBasisPath()) {
                reports.add(Messages._BuildAction_BasisPath(BasisPath, percent));
            }
            score = updateHealthScore(score, thresholds.getMinBasisPath(),
                                      percent, thresholds.getMaxBasisPath());
        }
        if (MCDC != null && thresholds.getMaxMCDC() > 0) {
            percent = MCDC.getPercentage();
            if (percent < thresholds.getMaxMCDC()) {
                reports.add(Messages._BuildAction_MCDC(MCDC, percent));
            }
            score = updateHealthScore(score, thresholds.getMinMCDC(),
                                      percent, thresholds.getMaxMCDC());
        }
        if (Function != null && thresholds.getMaxFunction() > 0) {
            percent = Function.getPercentage();
            if (percent < thresholds.getMaxFunction()) {
                reports.add(Messages._BuildAction_Function(Function, percent));
            }
            score = updateHealthScore(score, thresholds.getMinFunction(),
                                      percent, thresholds.getMaxFunction());
        }
        if (FunctionCall != null && thresholds.getMaxFunctionCall() > 0) {
            percent = FunctionCall.getPercentage();
            if (percent < thresholds.getMaxFunctionCall()) {
                reports.add(Messages._BuildAction_FunctionCall(FunctionCall, percent));
            }
            score = updateHealthScore(score, thresholds.getMinFunctionCall(),
                                      percent, thresholds.getMaxFunctionCall());
        }
        if (score == 100) {
            reports.add(Messages._BuildAction_Perfect());
        }
        // Collect params and replace nulls with empty string
        Object[] args = reports.toArray(new Object[6]);
        for (int i = 5; i >= 0; i--) if (args[i]==null) args[i] = ""; else break;
        return new HealthReport(score, Messages._BuildAction_Description(
                args[0], args[1], args[2], args[3], args[4],args[5]));
    }

    private static int updateHealthScore(int score, int min, int value, int max) {
        if (value >= max) return score;
        if (value <= min) return 0;
        assert max != min;
        final int scaled = (int) (100.0 * ((float) value - min) / (max - min));
        if (scaled < score) return scaled;
        return score;
    }

    public Object getTarget() {
        return getResult();
    }

    @Override
    public AbstractBuild<?,?> getBuild() {
        return owner;
    }
    
	protected static FilePath[] getVectorCASTCoverageReports(File file) throws IOException, InterruptedException {
		FilePath path = new FilePath(file);
		if (path.isDirectory()) {
			return path.list("*xml");
		} else {
			// Read old builds (before 1.11) 
			FilePath report = new FilePath(new File(path.getName() + ".xml"));
			return report.exists() ? new FilePath[]{report} : new FilePath[0];
		}
	}

    /**
     * Obtains the detailed {@link CoverageReport} instance.
     */
    public synchronized CoverageReport getResult() {

        if(report!=null) {
            final CoverageReport r = report.get();
            if(r!=null)     return r;
        }

        final File reportFolder = VectorCASTPublisher.getVectorCASTReport(owner);

        try {
        	
        	// Get the list of report files stored for this build
            FilePath[] reports = getVectorCASTCoverageReports(reportFolder);
            InputStream[] streams = new InputStream[reports.length];
            for (int i=0; i<reports.length; i++) {
            	streams[i] = reports[i].read();
            }
            
            // Generate the report
            CoverageReport r = new CoverageReport(this, streams);

            if(rule!=null) {
                // we change the report so that the FAILED flag is set correctly
                logger.info("calculating failed packages based on " + rule);
                rule.enforce(r,new StreamTaskListener(new NullStream()));
            }

            report = new WeakReference<CoverageReport>(r);
            return r;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Failed to load " + reportFolder, e);
            return null;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load " + reportFolder, e);
            return null;
        }
    }

    @Override
    public VectorCASTBuildAction getPreviousResult() {
        return getPreviousResult(owner);
    }

    /**
     * Gets the previous {@link VectorCASTBuildAction} of the given build.
     */
    /*package*/ static VectorCASTBuildAction getPreviousResult(AbstractBuild<?,?> start) {
        AbstractBuild<?,?> b = start;
        while(true) {
            b = b.getPreviousBuild();
            if(b==null)
                return null;
            if(b.getResult()== Result.FAILURE)
                continue;
            VectorCASTBuildAction r = b.getAction(VectorCASTBuildAction.class);
            if(r!=null)
                return r;
        }
    }

    /**
     * Constructs the object from VectorCAST XML report files.
     *
     * @throws IOException
     *      if failed to parse the file.
     */
    public static VectorCASTBuildAction load(AbstractBuild<?,?> owner, Rule rule, VectorCASTHealthReportThresholds thresholds, FilePath... files) throws IOException {
        Ratio ratios[] = null;
        for (FilePath f: files ) {
            InputStream in = null;
            try {
                in = f.read();
                ratios = loadRatios(in, ratios);
            } catch (XmlPullParserException e) {
                throw new IOException2("Failed to parse " + f, e);
            } catch (InterruptedException e) {
                Logger.getLogger(VectorCASTBuildAction.class.getName()).log(Level.SEVERE, null, e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
           
        return new VectorCASTBuildAction(owner,rule,ratios[0],ratios[1],ratios[2],ratios[3],ratios[4],ratios[5],ratios[6],thresholds);
    }

    public static VectorCASTBuildAction load(AbstractBuild<?,?> owner, Rule rule, VectorCASTHealthReportThresholds thresholds, InputStream... streams) throws IOException, XmlPullParserException {
        Ratio ratios[] = null;
        for (InputStream in: streams) {
          ratios = loadRatios(in, ratios);
        }
        return new VectorCASTBuildAction(owner,rule,ratios[0],ratios[1],ratios[2],ratios[3],ratios[4],ratios[5],ratios[6],thresholds);
    }

    private static Ratio[] loadRatios(InputStream in, Ratio[] r) throws IOException, XmlPullParserException {
      
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
      
        XmlPullParser parser = factory.newPullParser();

        parser.setInput(in,null);
        String versionRead = "undefined";
        while(true) {
            if(parser.nextTag()!=XmlPullParser.START_TAG)
                continue;
            if (parser.getName().equals("version")) {
                versionRead = parser.getAttributeValue("", "value");
            }
            if(!parser.getName().equals("coverage"))
                continue;
            break;
        }

        if (!versionRead.equals("2")) {
            throw new XmlPullParserException("Unsupported version: '" + versionRead + "', expecting 2");
        }

        if (r == null || r.length < 7) 
            r = new Ratio[7];
        
        // head for the first <coverage> tag.
        for( int i=0; i<r.length; i++ ) {
            if(!parser.getName().equals("coverage"))
                break;

            parser.require(XmlPullParser.START_TAG,"","coverage");
            String v = parser.getAttributeValue("", "value");
            String t = parser.getAttributeValue("", "type");
            
            int index ;
            if ( t.equals("statement, %") )
                index = 0;
            else if (t.equals("branch, %"))
                index = 1;
            else if ( t.equals("basispath, %"))
                index = 2;
            else if ( t.equals("mcdc, %") )
                index = 3;
            else if ( t.equals("function, %") )
                index = 4;
            else if ( t.equals("functioncall, %") )
                index = 5;
            else if ( t.equals("complexity, %") )
                index = 6;
            else
                continue;
                
            
            if (r[index] == null) {
                r[index] = Ratio.parseValue(v);
            } else {
                r[index].addValue(v);
            }
            
            // move to the next coverage tag.
            parser.nextTag();
            parser.nextTag();
        }
        
        return r;

    }

    private static final Logger logger = Logger.getLogger(VectorCASTBuildAction.class.getName());
}
