/*
 *  The MIT License
 *
 *  Copyright 2010 Sony Ericsson Mobile Communications. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

/**
 * @author Allyn Pierre (Allyn.GreyDeAlmeidaLimaPierre@sonyericsson.com)
 * @author Eduardo Palazzo (Eduardo.Palazzo@sonyericsson.com)
 * @author Mauro Durante (Mauro.DuranteJunior@sonyericsson.com)
 */
package com.vectorcast.plugins.vectorcastcoverage.portlet.chart;

import com.vectorcast.plugins.vectorcastcoverage.portlet.VectorCASTLoadData;
import com.vectorcast.plugins.vectorcastcoverage.portlet.Messages;
import com.vectorcast.plugins.vectorcastcoverage.portlet.bean.VectorCASTCoverageResultSummary;
import com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Constants;
import com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import java.time.LocalDate;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.plugins.view.dashboard.DashboardPortlet;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A portlet for VecctorCAST coverage results - Trend Chart.
 */
public class VectorCASTBuilderTrendChart extends DashboardPortlet {

  /**
   * Chart width that can be set by user.
   */
  private final int width;

  /**
   * Chart height that can be set by user.
   */
  private final int height;

  /**
   * Number of days of the chart that can be set by user.
   */
  private final int daysNumber;

  /**
   * Constructor with chart attributes as parameters.
   * DataBoundConstructor annotation helps the Stapler class to find
   * which constructor that should be used when automatically copying
   * values from a web form to a class.
   *
   * @param name
   *          chart name
   * @param width
   *          the chart width
   * @param height
   *          the chart height
   * @param daysNumber
   *          the number of days
   */
  @DataBoundConstructor
  public VectorCASTBuilderTrendChart(String name, String width, String height, String daysNumber) {

    super(name);

    this.width = Utils.validateChartAttributes(width, Constants.DEFAULT_WIDTH);
    this.height = Utils.validateChartAttributes(height, Constants.DEFAULT_HEIGHT);
    this.daysNumber = Utils.validateChartAttributes(daysNumber, Constants.DEFAULT_DAYS_NUMBER);
  }

  /**
   * This method will be called by portlet.jelly to load data and
   * create the chart.
   *
   * @return Graph a summary graph
   */
  public Graph getSummaryGraph() {

    Map<LocalDate, VectorCASTCoverageResultSummary> summaries;

    // Retrieve Dashboard View jobs
    List<Job> jobs = getDashboard().getJobs();

    // Fill a HashMap with the data will be showed in the chart
    summaries = VectorCASTLoadData.loadChartDataWithinRange(jobs, daysNumber);

    return createTrendChart(summaries, width, height);
  }

  /**
   * Creates a graph for VecctorCAST Coverage results.
   *
   * @param summaries
   *          HashMap(key = run date and value = Instrumentation tests
   *          results)
   * @param widthParam
   *          the chart width
   * @param heightParam
   *          the chart height
   * @return Graph (JFreeChart)
   */
  private static Graph createTrendChart(final Map<LocalDate, VectorCASTCoverageResultSummary> summaries, int widthParam,
    int heightParam) {

    return new Graph(-1, widthParam, heightParam) {

      @Override
      protected JFreeChart createGraph() {

        // Show empty chart
        if (summaries == null) {
          JFreeChart chart = ChartFactory.createStackedAreaChart(null, Constants.AXIS_LABEL,
            Constants.AXIS_LABEL_VALUE, null, PlotOrientation.VERTICAL, true, false, false);

          return chart;
        }
        JFreeChart chart = ChartFactory.createLineChart("", Constants.AXIS_LABEL, Constants.AXIS_LABEL_VALUE,
          buildDataSet(summaries), PlotOrientation.VERTICAL, true, false, false);
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

        plot.setDataset(1, buildComplexityDataSet(summaries));
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
    };
  }

  /**
   * Build data set.
   *
   * @param summaries
   *          HashMap containing data of chart.
   * @return CategoryDataset Interface for a dataset with one or more
   *         series, and values associated with categories.
   */
  private static CategoryDataset buildDataSet(Map<LocalDate, VectorCASTCoverageResultSummary> summaries) {

      DataSetBuilder<String, LocalDate> dataSetBuilder = new DataSetBuilder<String, LocalDate>();

      for (Map.Entry<LocalDate, VectorCASTCoverageResultSummary> entry : summaries.entrySet()) {
          float BasisPathCoverage = 0;
          float StatementCoverage = 0;
          float MCDCCoverage = 0;
          float BranchCoverage = 0;
          float FunctionCoverage = 0;
          float FunctionCallCoverage = 0;

          boolean hasStatementCoverage = false;
          boolean hasBranchCoverage = false;
          boolean hasBasisPathCoverage = false;
          boolean hasMCDCCoverage = false;
          boolean hasFunctionCoverage = false;
          boolean hasFunctionCallCoverage = false;

          int count = 0;

          List<VectorCASTCoverageResultSummary> list = entry.getValue().getVectorCASTCoverageResults();

          for (VectorCASTCoverageResultSummary item : list) {
              if (item.hasStatementCoverage()) {
                  hasStatementCoverage = true;
                  StatementCoverage += item.getStatementCoverage();

              }
              if (item.hasBranchCoverage()) {
                  BranchCoverage += item.getBranchCoverage();
                  hasBranchCoverage = true;
              }
              if (item.hasBasisPathCoverage()) {
                  BasisPathCoverage += item.getBasisPathCoverage();
                  hasBasisPathCoverage = true;
              }
              if (item.hasMCDCCoverage()) {
                  MCDCCoverage += item.getMCDCCoverage();
                  hasMCDCCoverage = true;
              }
              if (item.hasFunctionCoverage()) {
                  FunctionCoverage += item.getFunctionCoverage();
                  hasFunctionCoverage = true;
              }
              if (item.hasFunctionCallCoverage()) {
                  FunctionCallCoverage += item.getFunctionCallCoverage();
                  hasFunctionCallCoverage = true;
              }
              count++;
          }

          if (hasStatementCoverage) {
              dataSetBuilder.add((StatementCoverage / count), "Statement", entry.getKey());
          }
          if (hasBranchCoverage) {
              dataSetBuilder.add((BranchCoverage / count), "Branch", entry.getKey());
          }
          if (hasBasisPathCoverage) {
              dataSetBuilder.add((BasisPathCoverage / count), "Basis Path", entry.getKey());
          }
          if (hasMCDCCoverage) {
              dataSetBuilder.add((MCDCCoverage / count), "MC/DC", entry.getKey());
          }
          if (hasFunctionCoverage) {
              dataSetBuilder.add((FunctionCoverage / count), "Function", entry.getKey());
          }
          if (hasFunctionCallCoverage) {
              dataSetBuilder.add((FunctionCallCoverage / count), "Function Call", entry.getKey());
          }
      }

      return dataSetBuilder.build();
  }

    private static CategoryDataset buildComplexityDataSet(Map<LocalDate, VectorCASTCoverageResultSummary> summaries) {

        DataSetBuilder<String, LocalDate> dataSetBuilder = new DataSetBuilder<String, LocalDate>();

        for (Map.Entry<LocalDate, VectorCASTCoverageResultSummary> entry : summaries.entrySet()) {

            float Complexity = 0;
            boolean hasComplexity = false;

            List<VectorCASTCoverageResultSummary> list = entry.getValue().getVectorCASTCoverageResults();

            for (VectorCASTCoverageResultSummary item : list) {
                if (item.hasComplexity()) {
                    Complexity += item.getComplexity();
                    hasComplexity = true;
                }
            }

            if (hasComplexity) {
                dataSetBuilder.add(Complexity, "Complexity", entry.getKey());
            }
        }

        return dataSetBuilder.build();
    }

  /**
   * Descriptor that will be shown on Dashboard Portlets view.
   */
  @Extension(optional = true)
  public static class DescriptorImpl extends Descriptor<DashboardPortlet> {

    @Override
    public String getDisplayName() {
      return Messages.Portlet_ChartTitle();
    }
  }

  /**
   * Getter of the width.
   *
   * @return int the width
   */
  public int getWidth() {
    return width;
  }

  /**
   * Getter of the height.
   *
   * @return int the height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Getter of the number of days.
   *
   * @return int the number of days
   */
  public int getDaysNumber() {
    return daysNumber;
  }
}
