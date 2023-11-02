package com.vectorcast.plugins.vectorcastcoverage;

import hudson.Util;
import hudson.model.Api;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.Graph;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import org.jfree.chart.renderer.category.BarRenderer;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.XmlFile;
import hudson.model.Job;

/**
 * Base class of all coverage objects.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public abstract class CoverageObject<SELF extends CoverageObject<SELF>> {

    private static final Logger logger = Logger.getLogger(CoverageObject.class.getName());
  
    Ratio Statement = new Ratio();
    Ratio Branch = new Ratio();
    Ratio BasisPath = new Ratio();
    Ratio MCDC = new Ratio();
    Ratio Function = new Ratio();
    Ratio FunctionCall = new Ratio();
    Ratio Complexity = new Ratio();

    public boolean hasCoverage() {
        return hasFunctionCoverage()
                || hasFunctionCallCoverage()
                || hasStatementCoverage()
                || hasBranchCoverage()
                || hasBasisPathCoverage()
                || hasMCDCCoverage();
    }

    private volatile boolean failed = false;

    public boolean isFailed() {
        return failed;
    }

    /**
     * Marks this coverage object as failed.
     * @see Rule
     */
    public void setFailed() {
        failed = true;
    }

    @Exported(inline = true)
    public Ratio getStatementCoverage() {
        return Statement;
    }

    @Exported(inline = true)
    public Ratio getBranchCoverage() {
        return Branch;
    }

    @Exported(inline = true)
    public Ratio getComplexity() {
        return Complexity;
    }

    @Exported(inline = true)
    public Ratio getBasisPathCoverage() {
        return BasisPath;
    }

    /**
     * MCDC coverage. Can be null if this information is not collected.
     * @return coverage value (null if not collected)
     */
    @Exported(inline = true)
    public Ratio getMCDCCoverage() {
        return MCDC;
    }

    /**
     * MCDC coverage. Can be null if this information is not collected.
     * @return function coverage (null if not collected)
     */
    @Exported(inline = true)
    public Ratio getFunctionCoverage() {
        return Function;
    }

    @Exported(inline = true)
    public Ratio getFunctionCallCoverage() {
        return FunctionCall;
    }

    /**
     * Gets the build object that owns the whole coverage report tree.
     * @return the run instance
     */
    public abstract Run<?,?> getBuild();

    /**
     * Gets the corresponding coverage report object in the previous
     * run that has the record.
     *
     * @return
     *      null if no earlier record was found.
     */
    @Exported
    public abstract SELF getPreviousResult();

    /**
     * Used in the view to print out four table columns with the coverage info.
     * @return four columns string
     */
    public String printFourCoverageColumns() {
        StringBuilder buf = new StringBuilder();
        printRatioCell(isFailed(), Complexity, buf);
        printRatioCell(isFailed(), Statement, buf);
        printRatioCell(isFailed(), Branch, buf);
        //printRatioCell(isFailed(), BasisPath, buf);
        printRatioCell(isFailed(), MCDC, buf);
        printRatioCell(isFailed(), Function, buf);
        printRatioCell(isFailed(), FunctionCall, buf);
        return buf.toString();
    }

    public boolean hasFunctionCoverage() {
        return Function.isInitialized();
    }

    public boolean hasFunctionCallCoverage() {
        return FunctionCall.isInitialized();
    }

    public boolean hasMCDCCoverage() {
        return MCDC.isInitialized();
    }

    public boolean hasStatementCoverage() {
        return Statement.isInitialized();
    }

    public boolean hasBranchCoverage() {
        return Branch.isInitialized();
    }

    public boolean hasComplexity() {
        return Complexity.isInitialized();
    }

    public boolean hasBasisPathCoverage() {
        return BasisPath.isInitialized();
    }
    
    protected Integer getMaxHistoryFreestyleJob(String xml) {
      Integer maxHistory;

      // try for a freestyle job <maxHistory>20</maxHistory>
      try {
        maxHistory = Integer.parseInt(xml.split("<maxHistory>")[1].split("</maxHistory>")[0]);
      } catch (ArrayIndexOutOfBoundsException e) {
        maxHistory = 20;
        logger.log(Level.INFO,"error finding <maxHistory>###</maxhistory>: ", e);        
      } catch (java.lang.NumberFormatException e) {
        maxHistory = 20;
        logger.log(Level.INFO,"error Converting to number:", e);
      }
      
      return maxHistory;
    }
    
    protected Integer getMaxHistoryPipelineJob(String xml) {
      Integer maxHistory = 20;
      
      int subIndex = xml.indexOf("maxHistory");
      
      if (subIndex == -1) {
          return maxHistory;
      } else {
        subIndex = xml.indexOf("maxHistory");
        String substr = xml.substring(subIndex);
        int colonIdx = substr.indexOf(":") + 1;
        int commaIdx = substr.indexOf(",");
        int sqrBktIdx = substr.indexOf("]");

        int endingIdx = 0;
        
        if ((sqrBktIdx != -1) && (sqrBktIdx < commaIdx))  {
            endingIdx = sqrBktIdx;
        } else if (commaIdx != -1) {
            endingIdx = commaIdx;
        } else {
            return maxHistory;
        }
        
        substr = substr.substring(colonIdx, endingIdx);
        substr = substr.replace("'","");
        substr = substr.replace("\"","\"");
        maxHistory = Integer.parseInt(substr.trim());      
      }
      
      return maxHistory;
      
    }
    
    protected Integer getMaxHistory() {
        
      Run<?,?> build = getBuild();
      Job<?,?> job  = build.getParent();
      
      Integer maxHistory = 20;
      
      try {
        XmlFile configFile = job.getConfigFile();
        String xml = configFile.asString(); //Populated XML String....
        
        if (xml.contains("<maxHistory>")) {
          maxHistory = getMaxHistoryFreestyleJob(xml);
        }
        else if (xml.contains("maxHistory")) {
          maxHistory = getMaxHistoryPipelineJob(xml);
        }
      } catch (IOException e ){
        logger.log(Level.INFO,"error reading configFile: ", e);
      }

      return maxHistory;
    }
    
    static NumberFormat dataFormat = new DecimalFormat("000.00");
    static NumberFormat percentFormat = new DecimalFormat("0.0");
    static NumberFormat intFormat = new DecimalFormat("0");

    protected static void printRatioCell(boolean failed, Ratio ratio, StringBuilder buf) {
        if (ratio != null && ratio.isInitialized()) {

            String className = "nowrap" + (failed ? " red" : "");
            buf.append("<td align=\"center\" class='").append(className).append("'");
            buf.append(" data='").append(dataFormat.format(ratio.getPercentageFloat()));
            buf.append("'>\n");
            if (ratio.getNumerator() != 0.0 && ratio.getDenominator() == 0.0) {
                buf.append("<span class='text'>").append(ratio.getNumerator()).append("</span>");
            } else if (ratio.getNumerator() == 0.0 && ratio.getDenominator() == 0.0) {
                buf.append("<span class='text'>-</span>");
            } else {
                printRatioTable(ratio, buf);
            }
            buf.append("</td>\n");
        }
        else {
            buf.append("<td align=\"center\" >-</td>\n");
        }
    }

    protected static void printRatioTable(Ratio ratio, StringBuilder buf) {
        String percent = percentFormat.format(ratio.getPercentageFloat());
        String numerator = intFormat.format(ratio.getNumerator());
        String denominator = intFormat.format(ratio.getDenominator());
        buf.append("<table class='percentgraph' cellpadding='0px' cellspacing='0px'><tr class='percentgraph'>")
                .append("<td width='64px' class='data'>").append(percent).append("%</td>")
                .append("<td class='percentgraph'>")
                .append("<div class='percentgraph'><div class='greenbar' style='width: ").append(ratio.getPercentageFloat()).append("px;'>")
                .append("<span class='text'>").append(numerator).append("/").append(denominator)
                .append("</span></div></div></td></tr></table>");
    }

    /**
     * Generates the graph that shows the coverage trend up to this report.
     * @param req web request
     * @param rsp web response
     * @throws IOException if unable to read/parse
     */
    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if(ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath()+"/images/headless.png");
            return;
        }
        
        Run<?,?> build = getBuild();
        Calendar t = build.getTimestamp();
        
        final Integer maxHistory = getMaxHistory();

        String w = Util.fixEmptyAndTrim(req.getParameter("width"));
        String h = Util.fixEmptyAndTrim(req.getParameter("height"));
        int width = (w != null) ? Integer.parseInt(w) : 500;
        int height = (h != null) ? Integer.parseInt(h) : 200;          
        
        new GraphImpl(this, t, width, height) {

            @Override
            protected DataSetBuilder<String, NumberOnlyBuildLabel> createDataSet(CoverageObject<SELF> obj) {
                logger.log(Level.INFO,"In CoverageObject::doGraph::GraphImpl::createDataSet");
                
                DataSetBuilder<String, NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, NumberOnlyBuildLabel>();

                Integer historyCount = 0;
                
                for (CoverageObject<SELF> a = obj; a != null; a = a.getPreviousResult()) {
                    
                    if (historyCount++ >= maxHistory) {
                        break;
                    }


                    NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(a.getBuild());
                    if (a.Statement != null && a.Statement.isInitialized()) {
                        dsb.add(a.Statement.getPercentageFloat(), Messages.CoverageObject_Legend_Statement(), label);
                    }
                    if (a.Branch != null && a.Branch.isInitialized()) {
                        dsb.add(a.Branch.getPercentageFloat(), Messages.CoverageObject_Legend_Branch(), label);
                    }
                    if (a.BasisPath != null && a.BasisPath.isInitialized()) {
                        dsb.add(a.BasisPath.getPercentageFloat(), Messages.CoverageObject_Legend_BasisPath(), label);
                    }
                    if (a.MCDC != null && a.MCDC.isInitialized()) {
                        dsb.add(a.MCDC.getPercentageFloat(), Messages.CoverageObject_Legend_MCDC(), label);
                    }
                    if (a.Function != null && a.Function.isInitialized()) {
                        dsb.add(a.Function.getPercentageFloat(), Messages.CoverageObject_Legend_Function(), label);
                    }
                    if (a.FunctionCall != null && a.FunctionCall.isInitialized()) {
                        dsb.add(a.FunctionCall.getPercentageFloat(), Messages.CoverageObject_Legend_FunctionCall(), label);
                    }
                }

                logger.log(Level.INFO,"History Count = " + Integer.toString(historyCount));
                return dsb;
            }
        }.doPng(req, rsp);
    }

    public Api getApi() {
    	return new Api(this);
    }

    private abstract class GraphImpl extends Graph {

        private CoverageObject<SELF> obj;

        public GraphImpl(CoverageObject<SELF> obj, Calendar timestamp, int defaultW, int defaultH) {
            super(timestamp, defaultW, defaultH);
            this.obj = obj;
        }

        protected abstract DataSetBuilder<String, NumberOnlyBuildLabel> createDataSet(CoverageObject<SELF> obj);

        protected JFreeChart createGraph() {
            final CategoryDataset dataset = createDataSet(obj).build();
            
            logger.log(Level.INFO,"dataset (C | R) = " + Integer.toString(dataset.getColumnCount()) + " | " + Integer.toString(dataset.getRowCount()) );

            final JFreeChart chart = ChartFactory.createLineChart(
                    null, // chart title
                    null, // unused
                    "Coverage (%)", // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    true, // include legend
                    true, // tooltips
                    false // urls
                    );

            // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

            final LegendTitle legend = chart.getLegend();
            legend.setPosition(RectangleEdge.RIGHT);

            chart.setBackgroundPaint(Color.white);

            final CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlinePaint(null);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.black);

            CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
            plot.setDomainAxis(domainAxis);
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            domainAxis.setCategoryMargin(0.0);

            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setUpperBound(100);
            rangeAxis.setLowerBound(0);

            final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setBaseStroke(new BasicStroke(4.0f));
            renderer.setBaseShapesVisible(true);
            ColorPalette.apply(renderer);

            // crop extra space around the graph
            plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

            plot.setDataset(1, createComplexityDataSet(obj).build());
            plot.mapDatasetToRangeAxis(1, 1);

            final NumberAxis axis2 = new NumberAxis("Complexity");
            axis2.setAutoRange(true);
            plot.setRangeAxis(1, axis2);

            final BarRenderer renderer2 = new BarRenderer();
            Color color = new Color(0xC0, 0xC0, 0xC0, 128);
            renderer2.setSeriesPaint(0, color);

            plot.setRenderer(1, renderer2);
            return chart;
        }


        protected DataSetBuilder<String, NumberOnlyBuildLabel> createComplexityDataSet(CoverageObject<SELF> obj) {
            DataSetBuilder<String, NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, NumberOnlyBuildLabel>();
            
            Integer maxHistory = getMaxHistory();
            Integer historyCount = 0;

            for (CoverageObject<SELF> a = obj; a != null; a = a.getPreviousResult()) {
                NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(a.getBuild());

                if (historyCount++ >= maxHistory) {
                    break;
                }
                dsb.add(a.Complexity.getNumerator(), "Complexity", label);
            }
            return dsb;
        }

    }
}
