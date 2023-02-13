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
package com.vectorcast.plugins.vectorcastcoverage.portlet;

import hudson.model.Job;
import hudson.model.Run;
import com.vectorcast.plugins.vectorcastcoverage.VectorCASTBuildAction;
import com.vectorcast.plugins.vectorcastcoverage.portlet.bean.VectorCASTCoverageResultSummary;
import com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Utils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.LocalDate;

/**
 * Load data of VectorCAST coverage results used by chart or grid.
 */
public final class VectorCASTLoadData {

  /**
   * Private constructor avoiding this class to be used in a non-static way.
   */
  private VectorCASTLoadData() {
  }

  /**
   * Get VcastCoverage coverage results of all jobs and store into a sorted
   * HashMap by date.
   *
   * @param jobs
   *        jobs of Dashboard view
   * @param daysNumber
   *          number of days
   * @return Map The sorted summaries
   */
  public static Map<LocalDate, VectorCASTCoverageResultSummary> loadChartDataWithinRange(List<Job> jobs, int daysNumber) {

    Map<LocalDate, VectorCASTCoverageResultSummary> summaries = new HashMap<LocalDate, VectorCASTCoverageResultSummary>();

    // Get the last build (last date) of the all jobs
    LocalDate lastDate = Utils.getLastDate(jobs);

    // No builds
    if (lastDate == null) {
      return null;
    }

    // Get the first date from last build date minus number of days
    LocalDate firstDate = lastDate.minusDays(daysNumber);

    // For each job, get VcastCoverage coverage results according with
    // date range (last build date minus number of days)
    for (Job job : jobs) {

      Run run = job.getLastBuild();

      if (null != run) {
        LocalDate runDate = Utils.calendarToLocalData(run.getTimestamp());

        while (runDate.isAfter(firstDate)) {

          summarize(summaries, run, runDate, job);

          run = run.getPreviousBuild();

          if (null == run) {
            break;
          }

          runDate = Utils.calendarToLocalData(run.getTimestamp());

        }
      }
    }

    // Sorting by date, ascending order
    Map<LocalDate, VectorCASTCoverageResultSummary> sortedSummaries = new TreeMap(summaries);

    return sortedSummaries;

  }

  /**
   * Summarize VectorCAST Coverage results.
   *
   * @param summaries
   *          a Map of VectorCASTCoverageResultSummary objects indexed by
          dates
   * @param run
   *          the build which will provide information about the
   *          coverage result
   * @param runDate
   *          the date on which the build was performed
   * @param job
   *          job from the DashBoard Portlet view
   */
  private static void summarize(Map<LocalDate, VectorCASTCoverageResultSummary> summaries, Run run, LocalDate runDate, Job job) {

    VectorCASTCoverageResultSummary vectorCASTCoverageResult = getResult(run);

    // Retrieve VcastCoverage information for informed date
    VectorCASTCoverageResultSummary vectorCASTCoverageResultSummary = summaries.get(runDate);

    // Consider the last result of each
    // job date (if there are many builds for the same date). If not
    // exists, the VcastCoverage coverage data must be added. If exists
    // VcastCoverage coverage data for the same date but it belongs to other
    // job, sum the values.
    if (vectorCASTCoverageResultSummary == null) {
      vectorCASTCoverageResultSummary = new VectorCASTCoverageResultSummary();
      vectorCASTCoverageResultSummary.addCoverageResult(vectorCASTCoverageResult);
      vectorCASTCoverageResultSummary.setJob(job);
    } else {

      // Check if exists VectorCASTCoverage data for same date and job
      List<VectorCASTCoverageResultSummary> listResults = vectorCASTCoverageResultSummary.getVectorCASTCoverageResults();
      boolean found = false;

      for (VectorCASTCoverageResultSummary item : listResults) {
        if ((null != item.getJob()) && (null != item.getJob().getName()) && (null != job)) {
          if (item.getJob().getName().equals(job.getName())) {
            found = true;
            break;
          }
        }
      }

      if (!found) {
        vectorCASTCoverageResultSummary.addCoverageResult(vectorCASTCoverageResult);
        vectorCASTCoverageResultSummary.setJob(job);
      }
    }

    summaries.put(runDate, vectorCASTCoverageResultSummary);
  }

  /**
   * Get the VcastCoverage coverage result for a specific run.
   *
   * @param run
   *          a job execution
   * @return VectorCASTCoverageResultSummary the coverage result
   */
  private static VectorCASTCoverageResultSummary getResult(Run run) {
    VectorCASTBuildAction vectorCASTAction = run.getAction(VectorCASTBuildAction.class);

    float BasisPathCoverage = -1.0f;
    float StatementCoverage = -1.0f;
    float MCDCCoverage = -1.0f;
    float BranchCoverage = -1.0f;
    float FunctionCoverage = -1.0f;
    float FunctionCallCoverage = -1.0f;
    float Complexity = -1.0f;

    if (vectorCASTAction != null) {
      if (null != vectorCASTAction.getBasisPathCoverage()) {
        BasisPathCoverage = vectorCASTAction.getBasisPathCoverage().getPercentageFloat();
      }
      if (null != vectorCASTAction.getStatementCoverage()) {
        StatementCoverage = vectorCASTAction.getStatementCoverage().getPercentageFloat();
      }
      if (null != vectorCASTAction.getMCDCCoverage()) {
        MCDCCoverage = vectorCASTAction.getMCDCCoverage().getPercentageFloat();
      }
      if (null != vectorCASTAction.getBranchCoverage()) {
        BranchCoverage = vectorCASTAction.getBranchCoverage().getPercentageFloat();
      }
      if (null != vectorCASTAction.getFunctionCoverage()) {
        FunctionCoverage = vectorCASTAction.getFunctionCoverage().getPercentageFloat();
      }
      if (null != vectorCASTAction.getFunctionCallCoverage()) {
        FunctionCallCoverage = vectorCASTAction.getFunctionCallCoverage().getPercentageFloat();
      }
      if (null != vectorCASTAction.getComplexity()) {
        Complexity = vectorCASTAction.getComplexity().getNumerator();
      }
    }
    return new VectorCASTCoverageResultSummary(run.getParent(), BasisPathCoverage, MCDCCoverage, BranchCoverage, StatementCoverage, FunctionCoverage, FunctionCallCoverage,Complexity);
  }

  /**
   * Summarize the last coverage results of all jobs, which have coverage.
   *
   * @param jobs
   *          a final Collection of Job objects
   * @return VectorCASTCoverageResultSummary the result summary
   */
  public static VectorCASTCoverageResultSummary getResultSummary(final Collection<Job> jobs) {
    VectorCASTCoverageResultSummary summary = new VectorCASTCoverageResultSummary();

    for (Job job : jobs) {

      float BasisPathCoverage = -1.0f;
      float StatementCoverage = -1.0f;
      float MCDCCoverage = -1.0f;
      float BranchCoverage = -1.0f;
      float FunctionCoverage = -1.0f;
      float FunctionCallCoverage = -1.0f;
      float Complexity = -1.0f;

      Run run = job.getLastSuccessfulBuild();

      if (run != null) {

        VectorCASTBuildAction vectorCASTAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null == vectorCASTAction) {
            continue;
        } else {
          if (null != vectorCASTAction.getBasisPathCoverage()) {
            BasisPathCoverage = vectorCASTAction.getBasisPathCoverage().getPercentageFloat();
            BigDecimal bigBasisPathCoverage = new BigDecimal(BasisPathCoverage);
            bigBasisPathCoverage = bigBasisPathCoverage.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            BasisPathCoverage = bigBasisPathCoverage.floatValue();
          }

          if (null != vectorCASTAction.getStatementCoverage()) {
            StatementCoverage = vectorCASTAction.getStatementCoverage().getPercentageFloat();
            BigDecimal bigStatementCoverage = new BigDecimal(StatementCoverage);
            bigStatementCoverage = bigStatementCoverage.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            StatementCoverage = bigStatementCoverage.floatValue();
          }
          if (null != vectorCASTAction.getMCDCCoverage()) {
            MCDCCoverage = vectorCASTAction.getMCDCCoverage().getPercentageFloat();
            BigDecimal bigMCDCCoverage = new BigDecimal(MCDCCoverage);
            bigMCDCCoverage = bigMCDCCoverage.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            MCDCCoverage = bigMCDCCoverage.floatValue();
          }

          if (null != vectorCASTAction.getBranchCoverage()) {
            BranchCoverage = vectorCASTAction.getBranchCoverage().getPercentageFloat();
            BigDecimal bigBranchCoverage = new BigDecimal(BranchCoverage);
            bigBranchCoverage = bigBranchCoverage.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            BranchCoverage = bigBranchCoverage.floatValue();
          }
          
          if (null != vectorCASTAction.getFunctionCoverage()) {
            FunctionCoverage = vectorCASTAction.getFunctionCoverage().getPercentageFloat();
            BigDecimal bigFunctionCoverage = new BigDecimal(FunctionCoverage);
            bigFunctionCoverage = bigFunctionCoverage.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            FunctionCoverage = bigFunctionCoverage.floatValue();
          }
          if (null != vectorCASTAction.getFunctionCallCoverage()) {
            FunctionCallCoverage = vectorCASTAction.getFunctionCallCoverage().getPercentageFloat();
            BigDecimal bigFunctionCallCoverage = new BigDecimal(FunctionCallCoverage);
            bigFunctionCallCoverage = bigFunctionCallCoverage.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            FunctionCallCoverage = bigFunctionCallCoverage.floatValue();
          }
          if (null != vectorCASTAction.getComplexity()) {
            Complexity = vectorCASTAction.getComplexity().getNumerator();
            BigDecimal bigComplexity = new BigDecimal(Complexity);
            bigComplexity = bigComplexity.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            Complexity = bigComplexity.floatValue();
          }
        }
      }

      summary.addCoverageResult(new VectorCASTCoverageResultSummary(job, BasisPathCoverage, MCDCCoverage, BranchCoverage,
        StatementCoverage, FunctionCoverage, FunctionCallCoverage, Complexity));
    }
    return summary;
  }

  public static boolean hasStatementCoverage(final Collection<Job> jobs) {
    for (Job job : jobs) {
      Run run = job.getLastSuccessfulBuild();
      if (run != null) {
        VectorCASTBuildAction vcastcoverageAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null == vcastcoverageAction) {
            continue;
        } 
        else if (null != vcastcoverageAction.getStatementCoverage()) {
           return true;
        }
      }
    }
    return false;
  }

  public static boolean hasBranchCoverage(final Collection<Job> jobs) {
    for (Job job : jobs) {
      Run run = job.getLastSuccessfulBuild();
      if (run != null) {
        VectorCASTBuildAction vcastcoverageAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null == vcastcoverageAction) {
            continue;
        } 
        else if (null != vcastcoverageAction.getBranchCoverage()) {
           return true;
        }
      }
    }
    return false;
  }
  
  public static boolean hasComplexity(final Collection<Job> jobs) {
    for (Job job : jobs) {
      Run run = job.getLastSuccessfulBuild();
      if (run != null) {
        VectorCASTBuildAction vcastcoverageAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null != vcastcoverageAction.getComplexity()) {
           return true;
        }
      }
    }
    return false;
  }

  public static boolean hasBasisPathCoverage(final Collection<Job> jobs) {
    for (Job job : jobs) {
      Run run = job.getLastSuccessfulBuild();
      if (run != null) {
        VectorCASTBuildAction vcastcoverageAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null == vcastcoverageAction) {
            continue;
        } 
        else if (null != vcastcoverageAction.getBasisPathCoverage()) {
           return true;
        }
      }
    }
    return false;
  }

  public static boolean hasMCDCCoverage(final Collection<Job> jobs) {
    for (Job job : jobs) {
      Run run = job.getLastSuccessfulBuild();
      if (run != null) {
        VectorCASTBuildAction vcastcoverageAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null == vcastcoverageAction) {
            continue;
        } 
        else if (null != vcastcoverageAction.getMCDCCoverage()) {
           return true;
        }
      }
    }
    return false;
  }

  public static boolean hasFunctionCoverage(final Collection<Job> jobs) {
    for (Job job : jobs) {
      Run run = job.getLastSuccessfulBuild();
      if (run != null) {
        VectorCASTBuildAction vcastcoverageAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null == vcastcoverageAction) {
            continue;
        } 
        else if (null != vcastcoverageAction.getFunctionCoverage()) {
           return true;
        }
      }
    }
    return false;
  }

  public static boolean hasFunctionCallCoverage(final Collection<Job> jobs) {
    for (Job job : jobs) {
      Run run = job.getLastSuccessfulBuild();
      if (run != null) {
        VectorCASTBuildAction vcastcoverageAction = job.getLastSuccessfulBuild().getAction(VectorCASTBuildAction.class);

        if (null == vcastcoverageAction) {
            continue;
        } 
        else if (null != vcastcoverageAction.getFunctionCallCoverage()) {
           return true;
        }
      }
    }
    return false;
  }
}
