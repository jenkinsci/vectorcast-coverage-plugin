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
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * {@link Publisher} that captures VectorCAST coverage reports.
 *
 * @author Kohsuke Kawaguchi
 */
public class VectorCASTPublisher extends Recorder {

    /**
     * Relative path to the VectorCAST XML file inside the workspace.
     */
    public String includes;
    
    public boolean useThreshold;

    /**
     * Rule to be enforced. Can be null.
     *
     * TODO: define a configuration mechanism.
     */
    public Rule rule;

    /**
     * {@link hudson.model.HealthReport} thresholds to apply.
     */
    public VectorCASTHealthReportThresholds healthReports = new VectorCASTHealthReportThresholds();

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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();
        
        // Make sure VectorCAST actually ran
        if (build instanceof MavenBuild) {
            MavenBuild mavenBuild = (MavenBuild) build;
            if (!didVecctorCASTRun(mavenBuild)) {
                listener.getLogger().println("Skipping VecctorCAST coverage report as mojo did not run.");
                return true;
            }
        } else if (build instanceof MavenModuleSetBuild) {
            MavenModuleSetBuild moduleSetBuild = (MavenModuleSetBuild) build;
            if (!didVecctorCASTRun(moduleSetBuild.getModuleLastBuilds().values())) {
                listener.getLogger().println("Skipping VecctorCAST coverage report as mojo did not run.");
                return true;
            }
        }
                
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());
        includes = env.expand(includes);

        FilePath[] reports;
        if (includes == null || includes.trim().length() == 0) {
            FilePath workspace = build.getWorkspace();
            if (workspace!= null) {
                logger.println("[VectorCASTCoverage] [INFO]: looking for coverage reports in the entire workspace: " + workspace.getRemote());
            }
            reports = locateCoverageReports(build.getWorkspace(), "**/coverage.xml");
        } else {
            logger.println("[VectorCASTCoverage] [INFO]: looking for coverage reports in the provided path: " + includes);
            reports = locateCoverageReports(build.getWorkspace(), includes);
        }

        if (reports.length == 0) {
            Result result = build.getResult();
            if (result == null || result.isWorseThan(Result.UNSTABLE)) {
                return true;
            }

            logger.println("[VectorCASTCoverage] [INFO]: no coverage files found in workspace. Was any report generated?");
            build.setResult(Result.FAILURE);
            return true;
        } else {
            StringBuffer buf = new StringBuffer();
            for (FilePath f : reports) {
                buf.append("\n          ");
                buf.append(f.getRemote());
            }
            logger.println("[VectorCASTCoverage] [INFO]: found " + reports.length + " report files: " + buf.toString());
        }

        FilePath vcFolder = new FilePath(getVectorCASTReport(build));
        saveCoverageReports(vcFolder, reports);
        logger.println("[VectorCASTCoverage] [INFO]: stored " + reports.length + " report file(s) in the build folder: " + vcFolder);

        final VectorCASTBuildAction action = VectorCASTBuildAction.load(build, rule, healthReports, reports);

        logger.println("[VectorCASTCoverage] [INFO]: " + action.getBuildHealth().getDescription());

        build.getActions().add(action);

        final CoverageReport result = action.getResult();
        if (result == null) {
            logger.println("[VectorCASTCoverage] [INFO]: Could not parse coverage results. Setting Build to failure.");
            build.setResult(Result.FAILURE);
        } else if (result.isFailed()) {
            logger.println("[VectorCASTCoverage] [INFO]: code coverage enforcement failed. Setting Build to unstable.");
            build.setResult(Result.UNSTABLE);
        }
        
        checkThreshold(build, logger, env, action);

        return true;
    }

	private void checkThreshold(AbstractBuild<?, ?> build,
			final PrintStream logger, EnvVars env, final VectorCASTBuildAction action) {
			
		Ratio ratio = null;

		if (useThreshold) {
		
			try {		        

				if (isBranchCoverageOk(action) 
						|| isStatementCoverageOk(action) 
						|| isBasisPathCoverageOk(action)
						|| isMCDCCoverageOk(action)
						|| isFunctionCoverageOk(action)
						|| isFunctionCallCoverageOk(action)){
					logger.println("[VectorCASTCoverage] [INFO]: Build failed due to coverage percentage threshold exceeds ");
					build.setResult(Result.FAILURE);
				} 
				if (isStatementCoverageOk(action)) {
					logger.println("[VectorCASTCoverage] [INFO]: Statement coverage "+action.getStatementCoverage().getPercentage()+"% < "+healthReports.getMinStatement()+"%.");
				}
				if (isBranchCoverageOk(action)) {
					logger.println("[VectorCASTCoverage] [INFO]: Branch coverage "+action.getBranchCoverage().getPercentage()+"% < "+healthReports.getMinBranch()+"%.");
				}
				if (isBasisPathCoverageOk(action)) {
					logger.println("[VectorCASTCoverage] [INFO]: Basis Path coverage "+action.getBasisPathCoverage().getPercentage()+"% < "+healthReports.getMinBasisPath()+"%.");
				}
				if (isMCDCCoverageOk(action)) {
					logger.println("[VectorCASTCoverage] [INFO]: MC/DC coverage "+action.getMCDCCoverage().getPercentage()+"% < "+healthReports.getMinMCDC()+"%.");
				}
				if (isFunctionCoverageOk(action)) {
					logger.println("[VectorCASTCoverage] [INFO]: Function Coverage "+action.getFunctionCoverage().getPercentage()+"% < "+healthReports.getMinFunction()+"%.");
				}
				if (isFunctionCallCoverageOk(action)) {
					logger.println("[VectorCASTCoverage] [INFO]: Function Call coverage "+action.getFunctionCallCoverage().getPercentage()+"% < "+healthReports.getMinFunctionCall()+"%.");
				}
			} catch (NullPointerException e) {logger.println("[VectorCASTCoverage] [INFO]: VectorCASTPublisher::checkThreshold: Still catching NullPointerException...");}
		}
	}

	private boolean isMCDCCoverageOk(final VectorCASTBuildAction action) throws NullPointerException {
		
		if (action.getMCDCCoverage() == null)
			return false;

		return action.getMCDCCoverage().getPercentage() < healthReports.getMinMCDC();
	}

	private boolean isBasisPathCoverageOk(final VectorCASTBuildAction action) throws NullPointerException{
		if (action.getBasisPathCoverage() == null)
			return false;
		return action.getBasisPathCoverage().getPercentage() < healthReports.getMinBasisPath();
	}

	private boolean isStatementCoverageOk(final VectorCASTBuildAction action) throws NullPointerException{
		if (action.getStatementCoverage() == null)
			return false;
		return action.getStatementCoverage().getPercentage() < healthReports.getMinStatement();
	}

	private boolean isBranchCoverageOk(final VectorCASTBuildAction action) throws NullPointerException{
		if (action.getBranchCoverage() == null)
			return false;
		return action.getBranchCoverage().getPercentage() < healthReports.getMinBranch();
	}
	
	private boolean isFunctionCoverageOk(final VectorCASTBuildAction action) throws NullPointerException {
		if (action.getFunctionCoverage() == null)
			return false;
		return action.getFunctionCoverage().getPercentage() < healthReports.getMinFunction();
	}

	private boolean isFunctionCallCoverageOk(final VectorCASTBuildAction action) throws NullPointerException {
		if (action.getFunctionCallCoverage() == null)
			return false;
		return action.getFunctionCallCoverage().getPercentage() < healthReports.getMinFunctionCall();
	}

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new VectorCASTProjectAction(project);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * Gets the directory to store report files
     */
    static File getVectorCASTReport(AbstractBuild<?, ?> build) {
        return new File(build.getRootDir(), "vectorcastcoverage");
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    private boolean didVecctorCASTRun(Iterable<MavenBuild> mavenBuilds) {
        for (MavenBuild build : mavenBuilds) {
            if (didVecctorCASTRun(build)) {
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

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject json) throws FormException {
            VectorCASTPublisher pub = new VectorCASTPublisher();
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
