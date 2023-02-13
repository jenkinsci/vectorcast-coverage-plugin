package com.vectorcast.plugins.vectorcastcoverage;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.ExecutedMojo;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TaskListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.model.Run;
import java.io.InputStream;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jenkins.tasks.SimpleBuildStep;
import hudson.model.Job;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * {@link Publisher} that captures VectorCAST coverage reports.
 *
 * @author Kohsuke Kawaguchi
 */
public class VectorCASTPublisher extends Recorder implements SimpleBuildStep {
    
    /**
     * Relative path to the VectorCAST XML file inside the workspace.
     */
    public String includes;
    public Boolean useThreshold;
    public Boolean useCoverageHistory;
    
    /**
    /**
     * Rule to be enforced. Can be null.
     *
     * TODO: define a configuration mechanism.
     */
    public Rule rule;

    /**
     * {@link hudson.model.HealthReport} thresholds to apply.
     */
    public VectorCASTHealthReportThresholds healthReports = new VectorCASTHealthReportThresholds(0, 100, 0, 70, 0, 80, 0, 80, 0, 80, 0, 80 );

    // should not be used
    public VectorCASTHealthReportThresholds healthyTarget;
    public VectorCASTHealthReportThresholds unhealthyTarget = null;

    public VectorCASTPublisher() {
        
        this.includes = "xml_data/coverage_results*.xml";
        this.useThreshold = false;
        this.useCoverageHistory = false;
    }

    @DataBoundConstructor
    public VectorCASTPublisher(String includes, Boolean useThreshold, VectorCASTHealthReportThresholds healthyTarget, VectorCASTHealthReportThresholds unhealthyTarget, Boolean useCoverageHistory){
        
        this.includes = includes;
        this.useThreshold = useThreshold;
        this.healthReports = healthyTarget;
        this.unhealthyTarget = unhealthyTarget;
        if (useCoverageHistory == null) {
            this.useCoverageHistory = false;
        } else {
            this.useCoverageHistory = useCoverageHistory;
        }
        
    }
    
    @Nonnull
    public final String getIncludes() {
        return includes;
    }
    
    @Nonnull
    public final Boolean getUseThreshold() {
        return useThreshold;
    }
    @Nonnull
    public final Boolean getUseCoverageHistory() {
        if (this.useCoverageHistory == null) {
            this.useCoverageHistory = false;
        }
        return useCoverageHistory;
    }
    @Nonnull
    public final VectorCASTHealthReportThresholds getHealthReports() {
        return healthReports;
    }
    
    @Nonnull
    public final VectorCASTHealthReportThresholds getUnhealthReports() {
        return unhealthyTarget;
    }
    
    @Nonnull
    public final VectorCASTHealthReportThresholds getHealthyTarget() {
        return healthReports;
    }
    
    @DataBoundSetter public final void setIncludes(String includes) {
        this.includes = includes;
    }
    
    @DataBoundSetter public final void setUseThreshold(Boolean useThreshold) {
        this.useThreshold = useThreshold;
    }
    
    @DataBoundSetter public final void setUseCoverageHistory(Boolean useCoverageHistory) {
        this.useCoverageHistory = useCoverageHistory;
    }
    
    @DataBoundSetter public final void setHealthReports(VectorCASTHealthReportThresholds healthReports) {
        this.healthReports = healthReports;
    }
    
    @DataBoundSetter public final void setHealthyTarget(VectorCASTHealthReportThresholds healthyTarget) {
        this.healthReports = healthyTarget;
    }
    
    @DataBoundSetter public final void setUnhealthyTarget(VectorCASTHealthReportThresholds unhealthyTarget) {
        this.unhealthyTarget = unhealthyTarget;
    }
    
    /**
     * look for VectorCAST reports based in the configured parameter includes. 'includes' is - an Ant-style pattern - a list
     * of files and folders separated by the characters ;:,
     * @param workspace workspace
     * @param includes includes
     * @return filepaths loaded
     * @throws IOException IO error
     * @throws InterruptedException interrupted
     */
    protected static FilePath[] locateCoverageReports(FilePath workspace, String includes) throws IOException, InterruptedException {

        // First use ant-style pattern
        try {
            FilePath[] ret = workspace.list(includes);
            if (ret.length > 0) {
                return ret;
            }
        } catch (Exception e) {
        }

        // If it fails, do a legacy search
        ArrayList<FilePath> files = new ArrayList<FilePath>();
        String parts[] = includes.split("\\s*[;:,]+\\s*");
        for (String path : parts) {
            FilePath src = workspace.child(path);
            if (src.exists()) {
                if (src.isDirectory()) {
                    files.addAll(Arrays.asList(src.list("**/coverage*.xml")));
                } else {
                    files.add(src);
                }
            }
        }
        return files.toArray(new FilePath[files.size()]);
    }

    /**
     * save VectorCAST reports from the workspace to build folder
     * @param folder folder
     * @param files files
     * @throws IOException for UI error
     * @throws InterruptedException if interrupted
     */
    protected static void saveCoverageReports(FilePath folder, FilePath[] files) throws IOException, InterruptedException {
        folder.mkdirs();
        for (int i = 0; i < files.length; i++) {
            String name = "coverage" + (i > 0 ? i : "") + ".xml";
            FilePath src = files[i];
            FilePath dst = folder.child(name);
            src.copyTo(dst);
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        performImpl(run, workspace, listener);
    }

    public boolean performImpl(Run<?, ?> run, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();
        
        
        // Make sure VectorCAST actually ran
        if (run instanceof MavenBuild) {
            MavenBuild mavenBuild = (MavenBuild) run;
            if (!didVecctorCASTRun(mavenBuild)) {
                listener.getLogger().println("Skipping VecctorCAST coverage report as mojo did not run.");
                return true;
            }
        } else if (run instanceof MavenModuleSetBuild) {
            MavenModuleSetBuild moduleSetBuild = (MavenModuleSetBuild) run;
            if (!didVecctorCASTRun(moduleSetBuild.getModuleLastBuilds().values())) {
                listener.getLogger().println("Skipping VecctorCAST coverage report as mojo did not run.");
                return true;
            }
        }
                
        Map<String, String> envs;
        if (run instanceof AbstractBuild)
        {
            envs = ((AbstractBuild<?,?>) run).getBuildVariables();
        }
        else
        {
            envs = Collections.emptyMap();
        }
        
        EnvVars env = run.getEnvironment(listener);
        env.overrideAll(envs);
        includes = env.expand(includes);

        FilePath[] reports;
        if (includes == null || includes.trim().length() == 0) {
            //FilePath workspace = build.getWorkspace();
            if (workspace!= null) {
                logger.println("[VectorCASTCoverage] [INFO]: looking for coverage reports in the entire workspace: " + workspace.getRemote());
            }
            reports = locateCoverageReports(workspace, "**/coverage.xml");
        } else {
            logger.println("[VectorCASTCoverage] [INFO]: looking for coverage reports in the provided path: " + includes);
            reports = locateCoverageReports(workspace, includes);
        }

        if (reports.length == 0) {
            Result result = run.getResult();
            if (result == null || result.isWorseThan(Result.UNSTABLE)) {
                return true;
            }

            logger.println("[VectorCASTCoverage] [INFO]: no coverage files found in workspace. Was any report generated?");
            run.setResult(Result.FAILURE);
            return true;
        } else {
            StringBuffer buf = new StringBuffer();
            int idx = 0;
            for (FilePath f : reports) {
                if (idx == 0) {
                    buf.append("\n          coverage.xml:  ");
                }
                else {
                    buf.append("\n          coverage"+idx+".xml:  ");
                }
                buf.append(f.getRemote());
                idx += 1;
            }
            logger.println("[VectorCASTCoverage] [INFO]: found " + reports.length + " report files: " + buf.toString());
        }

        FilePath vcFolder = new FilePath(getVectorCASTReport(run));
        saveCoverageReports(vcFolder, reports);
        logger.println("[VectorCASTCoverage] [INFO]: stored " + reports.length + " report file(s) in the run folder: " + vcFolder);

        //convert FilePath to steams
        InputStream[] streams = new InputStream[reports.length];
        for (int i=0; i<reports.length; i++) {
            File localXMLFile = new File(vcFolder + "/" + "coverage" + (i > 0 ? i : "") + ".xml");
            streams[i] = new FileInputStream(localXMLFile);
        }

        final VectorCASTBuildAction action = VectorCASTBuildAction.load(run, rule, healthReports, streams); //reports);
        
        if (action.getBuildHealth() != null) {
            logger.println("**[VectorCASTCoverage] [INFO]: " + action.getBuildHealth().getDescription());
        } else {
            logger.println("**[VectorCASTCoverage] [INFO]: No thresholds set");
        }
        
        run.getActions().add(action);

        final CoverageReport result = action.getResult();
        if (result == null) {
            logger.println("[VectorCASTCoverage] [INFO]: Could not parse a coverage result file:");
            logger.println("[VectorCASTCoverage] [INFO]:     See Manage Jenkins > System Log > All Jenkins Log and search for 'Error Parsing VectorCAST Coverage'");
            logger.println("[VectorCASTCoverage] [INFO]:     Use file list above to relate coverage*.xml to xml_data/coverage_results*.xml");
            logger.println("[VectorCASTCoverage] [INFO]:     Setting Build to failure.");
            run.setResult(Result.FAILURE);
        } else if (result.isFailed()) {
            logger.println("[VectorCASTCoverage] [INFO]: code coverage enforcement failed. Setting Build to unstable.");
            run.setResult(Result.UNSTABLE);
        }
        
        if (getUseCoverageHistory()) {
            VectorCASTProjectAction vcProjAction = new VectorCASTProjectAction (run.getParent());
            VectorCASTBuildAction historyAction = vcProjAction.getPreviousNotFailedBuild();
            if (historyAction != null) {
                float prevStCov = 0.0f;
                float currStCov = 0.0f;
                float currBrCov = 0.0f;
                float prevBrCov = 0.0f;
                
                if (historyAction.getStatementCoverage() != null) {
                    prevStCov = historyAction.getStatementCoverage().getPercentageFloat();
                }
                if (action.getStatementCoverage() != null) {
                    currStCov = action.getStatementCoverage().getPercentageFloat();
                }
                if (historyAction.getBranchCoverage() != null) {
                    prevBrCov = historyAction.getBranchCoverage().getPercentageFloat();
                }
                if (action.getBranchCoverage() != null) {
                    currStCov = action.getBranchCoverage().getPercentageFloat();
                }
                                
                if ((currBrCov < prevBrCov) || (currStCov < prevStCov)) {
                    logger.println("**[VectorCASTCoverage] [INFO]: code coverage history enforcement failed. Setting Build to FAILURE.");
                    run.setResult(Result.FAILURE);
                } else {
                    logger.println("**[VectorCASTCoverage] [INFO]: code coverage history enforcement passed.");
                }
            
                logger.println("**[VectorCASTCoverage] [INFO] Previous (st/br): " +  String.format(" %.02f /", prevStCov) +   String.format(" %.02f", prevBrCov));
                logger.println("**[VectorCASTCoverage] [INFO] Current  (st/br): " +  String.format(" %.02f /", currStCov) +   String.format(" %.02f", currBrCov));
            } else {
                logger.println("[VectorCASTCoverage] [INFO]: Could not find previous non-failing build to checking code coverage history.");
            }
        } else {
            logger.println("[VectorCASTCoverage] [INFO]: Not checking code coverage history.");
        }
        
        checkThreshold(run, logger, env, action);

        return true;
    }

	private void printThresholdFailure(final PrintStream logger, String coverageType, int percent, int threshold) {
        logger.println("[VectorCASTCoverage] [FAIL]: " + coverageType + " coverage " + percent +"% < " + threshold + "% threshold.");
    }
    
    
	private void checkThreshold(Run<?, ?> run,
		final PrintStream logger, EnvVars env, final VectorCASTBuildAction action) {
			
		Ratio ratio = null;

		if (useThreshold && unhealthyTarget == null) {
		
            if (isBranchCoverageOk(action) 
                    || isStatementCoverageOk(action) 
                    || isBasisPathCoverageOk(action)
                    || isMCDCCoverageOk(action)
                    || isFunctionCoverageOk(action)
                    || isFunctionCallCoverageOk(action)){
                logger.println("[VectorCASTCoverage] [FAIL]: Build failed due to a coverage metric fell below the minimum threshold");
                run.setResult(Result.FAILURE);
            } 
            if (isStatementCoverageOk(action)) {
                printThresholdFailure(logger, "Statement", action.getStatementCoverage().getPercentage(), healthReports.getMinStatement());                    
            }
            if (isBranchCoverageOk(action)) {
                printThresholdFailure(logger, "Branch", action.getBranchCoverage().getPercentage(), healthReports.getMinBranch());
            }
            if (isBasisPathCoverageOk(action)) {
                printThresholdFailure(logger, "Basis Path", action.getBasisPathCoverage().getPercentage(), healthReports.getMinBasisPath());
            }
            if (isMCDCCoverageOk(action)) {
                printThresholdFailure(logger, "MC/DC", action.getMCDCCoverage().getPercentage(), healthReports.getMinMCDC());
            }
            if (isFunctionCoverageOk(action)) {
                printThresholdFailure(logger, "Function", action.getFunctionCoverage().getPercentage(), healthReports.getMinFunction());
            }
            if (isFunctionCallCoverageOk(action)) {
                printThresholdFailure(logger, "Function Call", action.getFunctionCallCoverage().getPercentage(), healthReports.getMinFunctionCall());
            }
		}
	}

	private boolean isMCDCCoverageOk(final VectorCASTBuildAction action)  {
		
		if (action.getMCDCCoverage() == null)
			return false;

		return action.getMCDCCoverage().getPercentage() < healthReports.getMinMCDC();
	}

	private boolean isBasisPathCoverageOk(final VectorCASTBuildAction action) {
		if (action.getBasisPathCoverage() == null)
			return false;
		return action.getBasisPathCoverage().getPercentage() < healthReports.getMinBasisPath();
	}

	private boolean isStatementCoverageOk(final VectorCASTBuildAction action) {
		if (action.getStatementCoverage() == null)
			return false;
		return action.getStatementCoverage().getPercentage() < healthReports.getMinStatement();
	}

	private boolean isBranchCoverageOk(final VectorCASTBuildAction action) {
		if (action.getBranchCoverage() == null)
			return false;
		return action.getBranchCoverage().getPercentage() < healthReports.getMinBranch();
	}
	
	private boolean isFunctionCoverageOk(final VectorCASTBuildAction action)  {
		if (action.getFunctionCoverage() == null)
			return false;
		return action.getFunctionCoverage().getPercentage() < healthReports.getMinFunction();
	}

	private boolean isFunctionCallCoverageOk(final VectorCASTBuildAction action)  {
		if (action.getFunctionCallCoverage() == null)
			return false;
		return action.getFunctionCallCoverage().getPercentage() < healthReports.getMinFunctionCall();
	}

    
    public Action getProjectAction(Job<?, ?> project) {
        return new VectorCASTProjectAction(project);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * Gets the directory to store report files
     */
    static File getVectorCASTReport(Run<?, ?> run  ) {
        return new File(run.getRootDir(), "vectorcastcoverage");
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    private boolean didVecctorCASTRun(Iterable<MavenBuild> mavenBuilds) {
        
        for (MavenBuild run : mavenBuilds) {
            if (didVecctorCASTRun(run)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean didVecctorCASTRun(MavenBuild mavenBuild) {
        for (ExecutedMojo mojo : mavenBuild.getExecutedMojos()) {
            if ("org.codehaus.mojo".equals(mojo.groupId) && "vectorcastexecution-maven-plugin".equals(mojo.artifactId)) {
                return true;
            }
        }
        return false;
    }
    
    @Extension
    public static final BuildStepDescriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(VectorCASTPublisher.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.VcastCoveragePublisher_DisplayName();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this, "vectorcastcoverage.");
            save();
            return super.configure(req, formData);
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject json) throws FormException {
            
            /*
             *  Setup for snippet generator
             */
            String loc_includes;
            Boolean loc_useThreshold;
            Boolean loc_useCoverageHistory;
            int maxStatement,maxBranch,maxBasisPath,maxMCDC,maxFunction,maxFunctionCall;
            int minStatement,minBranch,minBasisPath,minMCDC,minFunction,minFunctionCall;
            
            /* Get the input from the JSON object */
            loc_includes = json.optString("includes", "xml_data/coverage_results*.xml");
            if (loc_includes.isEmpty()) {
                    loc_includes = "xml_data/coverage_results*.xml";
            }
            loc_useThreshold = json.optBoolean("useThreshold", false);
            loc_useCoverageHistory = json.optBoolean("useCoverageHistory", false);
            
            maxStatement = json.optInt("maxStatement", 100);
            maxBranch = json.optInt("maxBranch", 70);
            maxBasisPath = json.optInt("maxBasisPath", 80);
            maxMCDC = json.optInt("maxMCDC", 80);
            maxFunction = json.optInt("maxFunction", 80);
            maxFunctionCall = json.optInt("maxFunctionCall", 80);

            minStatement = json.optInt("minStatement",0);
            minBranch = json.optInt("minBranch",0);
            minBasisPath = json.optInt("minBasisPath",0);
            minMCDC = json.optInt("minMCDC",0);
            minFunction = json.optInt("minFunction",0);
            minFunctionCall = json.optInt("minFunctionCall",0);
            
            /* Setup the healthReport */
            VectorCASTHealthReportThresholds loc_healthReports = new VectorCASTHealthReportThresholds( minStatement,  maxStatement,  minBranch,  maxBranch,  minBasisPath,  maxBasisPath,  minMCDC,  maxMCDC,  minFunction,  maxFunction,  minFunctionCall,  maxFunctionCall);
            
            VectorCASTPublisher pub = new VectorCASTPublisher(loc_includes,loc_useThreshold,loc_healthReports, null, loc_useCoverageHistory);
                                
            req.bindParameters(pub, "vectorcastcoverage.");
            req.bindParameters(pub.healthReports, "vectorCASTHealthReports.");
            // start ugly hack
            //@TODO remove ugly hack
            // the default converter for integer values used by req.bindParameters
            // defaults an empty value to 0. This happens even if the type is Integer
            // and not int.  We want to change the default values, so we use this hack.
            //
            // If you know a better way, please fix.
            if ("".equals(req.getParameter("vectorCASTHealthReports.maxStatement"))) {
                pub.healthReports.setMaxStatement(100);
            }
            if ("".equals(req.getParameter("vectorCASTHealthReports.maxBranch"))) {
                pub.healthReports.setMaxBranch(70);
            }
            if ("".equals(req.getParameter("vectorCASTHealthReports.maxBasisPath"))) {
                pub.healthReports.setMaxBasisPath(80);
            }
            if ("".equals(req.getParameter("vectorCASTHealthReports.maxMCDC"))) {
                pub.healthReports.setMaxMCDC(80);
            }
            if ("".equals(req.getParameter("vectorCASTHealthReports.maxFunction"))) {
                pub.healthReports.setMaxFunction(80);
            }
            if ("".equals(req.getParameter("vectorCASTHealthReports.maxFunctionCall"))) {
                pub.healthReports.setMaxFunctionCall(80);
            }
            // end ugly hack
            return pub;
        }
    }
}
