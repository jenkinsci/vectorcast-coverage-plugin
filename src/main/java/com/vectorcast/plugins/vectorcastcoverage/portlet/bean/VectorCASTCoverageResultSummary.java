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
package com.vectorcast.plugins.vectorcastcoverage.portlet.bean;

import hudson.model.Job;
import com.vectorcast.plugins.vectorcastcoverage.portlet.utils.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary of the VectorCAST Coverage result.
 */
public class VectorCASTCoverageResultSummary {

  /**
   * The related job.
   */
  private Job job;

  /**
   * basisPath coverage percentage.
   */
  private float BasisPathCoverage;
  private boolean hasBasisPathCoverage;

  /**
   * MCDC coverage percentage.
   */
  private float MCDCCoverage;
  private boolean hasMCDCCoverage;
  
  /**
   * branch coverage percentage.
   */
  private float BranchCoverage;
  private boolean hasBranchCoverage;

  /**
   * Class coverage percentage.
   */
  private float StatementCoverage;
  private boolean hasStatementCoverage;
  
  /**
   * function coverage percentage.
   */
  private float FunctionCoverage;
  private boolean hasFunctionCoverage;

  private float FunctionCallCoverage;
  private boolean hasFunctionCallCoverage;

  private float Complexity;
  private boolean hasComplexity;

  private List<VectorCASTCoverageResultSummary> coverageResults = new ArrayList<VectorCASTCoverageResultSummary>();

  /**
   * Default Constructor.
   */
  public VectorCASTCoverageResultSummary() {  
     this.hasStatementCoverage = false;
     this.hasBranchCoverage = false;
     this.hasBasisPathCoverage = false;
     this.hasMCDCCoverage = false;
     this.hasFunctionCoverage = false;
     this.hasFunctionCallCoverage = false;
     this.hasComplexity = false;
  }

  /**
   * Constructor with parameters.
   *
   * @param job the related Job
   * @param lBasisPathCoverage basis path coverage
   * @param lMCDCCoverage MCDC coverage
   * @param lBranchCoverage branch coverage
   * @param lFunctionCoverage function coverage
   * @param lFunctionCallCoverage function call coverage
   * @param lStatementCoverage statement coverage
   * @param lComplexity complexity
   */
  public VectorCASTCoverageResultSummary(Job job, float lBasisPathCoverage, float lMCDCCoverage, float lBranchCoverage,
    float lStatementCoverage, float lFunctionCoverage, float lFunctionCallCoverage, float lComplexity) {
    this.job = job;
         
    if (lStatementCoverage < 0.0f)
    {
	    this.StatementCoverage = 0.0f;
	    this.hasStatementCoverage = false;
    }
    else
    {
	    this.StatementCoverage = lStatementCoverage;
	    this.hasStatementCoverage = true;
    }

    if (lBranchCoverage < 0.0f)
    {
	    this.BranchCoverage = 0.0f;
	    this.hasBranchCoverage = false;
    }
    else
    {
	    this.BranchCoverage = lBranchCoverage;
	    this.hasBranchCoverage = true;
    }
    
    if (lBasisPathCoverage < 0.0f)
    {
	    this.BasisPathCoverage = 0.0f;
	    this.hasBasisPathCoverage = false;
    }
    else
    {
	    this.BasisPathCoverage = lBasisPathCoverage;
	    this.hasBasisPathCoverage = true;
    }

    if (lMCDCCoverage < 0.0f)
    {
	    this.MCDCCoverage = 0.0f;
	    this.hasMCDCCoverage = false;
    }
    else
    {
	    this.MCDCCoverage = lMCDCCoverage;
	    this.hasMCDCCoverage = true;
    }

    if (lFunctionCoverage < 0.0f)
    {
	    this.FunctionCoverage = 0.0f;
	    this.hasFunctionCoverage = false;
    }
    else
    {
	    this.FunctionCoverage = lFunctionCoverage;
	    this.hasFunctionCoverage = true;
    }
    if (lFunctionCallCoverage < 0.0f)
    {
	    this.FunctionCallCoverage = 0.0f;
	    this.hasFunctionCallCoverage = false;
    }
    else
    {
	    this.FunctionCallCoverage = lFunctionCallCoverage;
	    this.hasFunctionCallCoverage = true;
    }
    
    if (lComplexity < 0.0f)
    {
	    this.Complexity = 0.0f;
	    this.hasComplexity = false;
    }
    else
    {
	    this.Complexity = lComplexity;
	    this.hasComplexity = true;
    }    
  }

  /**
   * Add a coverage result.
   *
   * @param coverageResult
   *          a coverage result
   * @return VectorCASTCoverageResultSummary summary of the VectorCAST coverage
         result
   */
  public VectorCASTCoverageResultSummary addCoverageResult(VectorCASTCoverageResultSummary coverageResult) {
    this.setBasisPathCoverage(this.getBasisPathCoverage() + coverageResult.getBasisPathCoverage());
    this.setMCDCCoverage(this.getMCDCCoverage() + coverageResult.getMCDCCoverage());
    this.setBranchCoverage(this.getBranchCoverage() + coverageResult.getBranchCoverage());
    this.setStatementCoverage(this.getStatementCoverage() + coverageResult.getStatementCoverage());
    this.setFunctionCoverage(this.getFunctionCoverage() + coverageResult.getFunctionCoverage());
    this.setFunctionCallCoverage(this.getFunctionCallCoverage() + coverageResult.getFunctionCallCoverage());
    this.setComplexity(this.getComplexity() + coverageResult.getComplexity());

    this.hasStatementCoverage = this.hasStatementCoverage || coverageResult.hasStatementCoverage;
    this.hasBranchCoverage = this.hasBranchCoverage || coverageResult.hasBranchCoverage;
    this.hasBasisPathCoverage = this.hasBasisPathCoverage || coverageResult.hasBasisPathCoverage;
    this.hasMCDCCoverage = this.hasMCDCCoverage || coverageResult.hasMCDCCoverage;
    this.hasFunctionCoverage = this.hasFunctionCoverage || coverageResult.hasFunctionCoverage;
    this.hasFunctionCallCoverage = this.hasFunctionCallCoverage || coverageResult.hasFunctionCallCoverage;
    this.hasComplexity = this.hasComplexity || coverageResult.hasComplexity;
    	    
    getCoverageResults().add(coverageResult);

    return this;
  }

  /**
   * Get list of VectorCASTCoverageResultSummary objects.
   *
   * @return List a List of VectorCASTCoverageResultSummary objects
   */
  public List<VectorCASTCoverageResultSummary> getVectorCASTCoverageResults() {
    return this.getCoverageResults();
  }

  /**
   * Getter of the total of class coverage.
   *
   * @return float the total of class coverage.
   */
  public float getTotalStatementCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } else {
      float totalStatement = this.getStatementCoverage() / this.getCoverageResults().size();
      totalStatement = Utils.roundFLoat(1, BigDecimal.ROUND_HALF_EVEN, totalStatement);
      return totalStatement;
    }
  }
  /**
   * Get the total complexity
   * @return  total complexity
   */
  public float getTotalComplexity() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } else {
      float totalComplexity = this.getComplexity();
      totalComplexity = Utils.roundFLoat(1, BigDecimal.ROUND_HALF_EVEN, totalComplexity);
      return totalComplexity;
    }
  }
  /**
   * Get the total branch coverage
   * @return total branch coverage
   */  
  public float getTotalBranchCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } else {
      float totalBranch = this.getBranchCoverage() / this.getCoverageResults().size();
      totalBranch = Utils.roundFLoat(1, BigDecimal.ROUND_HALF_EVEN, totalBranch);
      return totalBranch;
    }
  }

  /**
   * Getter of the total of basisPath coverage.
   *
   * @return float the total of basisPath coverage.
   */
  public float getTotalBasisPathCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } else {
      float totalBasisPath = this.getBasisPathCoverage() / this.getCoverageResults().size();
      totalBasisPath = Utils.roundFLoat(1, BigDecimal.ROUND_HALF_EVEN, totalBasisPath);
      return totalBasisPath;
    }
  }

  /**
   * Getter of the total of MCDC coverage.
   *
   * @return float the total of MCDC coverage.
   */
  public float getTotalMCDCCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } else {
      float totalMCDC = this.getMCDCCoverage() / this.getCoverageResults().size();
      totalMCDC = Utils.roundFLoat(1, BigDecimal.ROUND_HALF_EVEN, totalMCDC);
      return totalMCDC;
    }
  }

  /**
   * Getter of the total of function coverage.
   *
   * @return float the total of branch coverage.
   */
  public float getTotalFunctionCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } else {
      float totalFunction = this.getFunctionCoverage() / this.getCoverageResults().size();
      totalFunction = Utils.roundFLoat(1, BigDecimal.ROUND_HALF_EVEN, totalFunction);
      return totalFunction;
    }
  }
  /**
   * Getter of the total of function call coverage.
   *
   * @return float the total of function call coverage.
   */
  public float getTotalFunctionCallCoverage() {
    if (this.getCoverageResults().size() <= 0) {
      return 0.0f;
    } else {
      float totalFunctionCall = this.getFunctionCallCoverage() / this.getCoverageResults().size();
      totalFunctionCall = Utils.roundFLoat(1, BigDecimal.ROUND_HALF_EVEN, totalFunctionCall);
      return totalFunctionCall;
    }
  }

  /**
   * @return Job a job
   */
  public Job getJob() {
    return job;
  }

  /**
   * @return the BasisPathCoverage
   */
  public float getBasisPathCoverage() {
    return BasisPathCoverage;
  }

  /**
   * @return the BasisPathCoverage
   */
  public float getComplexity() {
    return Complexity;
  }

  /**
   * @return the MCDCCoverage
   */
  public float getMCDCCoverage() {
    return MCDCCoverage;
  }

  /**
   * @return the FunctionCoverage
   */
  public float getFunctionCoverage() {
    return FunctionCoverage;
  }

  /**
   * @return the FunctionCallCoverage
   */
  public float getFunctionCallCoverage() {
    return FunctionCallCoverage;
  }

  /**
   * @return the BranchCoverage
   */
  public float getBranchCoverage() {
    return BranchCoverage;
  }

  /**
   * @return the StatementCoverage
   */
  public float getStatementCoverage() {
    return StatementCoverage;
  }

  public boolean hasStatementCoverage() {
    return this.hasStatementCoverage;
  }
  public boolean hasComplexity() {
    return this.hasComplexity;
  }

  public boolean hasBranchCoverage() {
    return this.hasBranchCoverage;
  }

  public boolean hasBasisPathCoverage() {
    return this.hasBasisPathCoverage;
  }

  public boolean hasMCDCCoverage() {
    return this.hasMCDCCoverage;
  }

  public boolean hasFunctionCoverage() {
    return this.hasFunctionCoverage;
  }

  public boolean hasFunctionCallCoverage() {
    return this.hasFunctionCallCoverage;
  }

  /**
   * @param job
   *          the job to set
   */
  public void setJob(Job job) {
    this.job = job;
  }

  /**
   * @param BasisPathCoverage
   *          the BasisPathCoverage to set
   */
  public void setBasisPathCoverage(float BasisPathCoverage) {
    this.BasisPathCoverage = BasisPathCoverage;
  }

  public void setComplexity(float Complexity) {
    this.Complexity = Complexity;
  }

  /**
   * @param MCDCCoverage
   *          the MCDCCoverage to set
   */
  public void setMCDCCoverage(float MCDCCoverage) {
    this.MCDCCoverage = MCDCCoverage;
  }

  /**
   * @param BranchCoverage
   *          the BranchCoverage to set
   */
  public void setBranchCoverage(float BranchCoverage) {
    this.BranchCoverage = BranchCoverage;
  }


  /**
   * @param FunctionCoverage
   *          the FunctionCoverage to set
   */
  public void setFunctionCoverage(float FunctionCoverage) {
    this.FunctionCoverage = FunctionCoverage;
  }
  /**
   * @param FunctionCallCoverage
   *          the FunctionCallCoverage to set
   */
  public void setFunctionCallCoverage(float FunctionCallCoverage) {
    this.FunctionCallCoverage = FunctionCallCoverage;
  }

  /**
   * @param StatementCoverage
   *          the StatementCoverage to set
   */
  public void setStatementCoverage(float StatementCoverage) {
    this.StatementCoverage = StatementCoverage;
  }

  /**
   * @return a list of coverage results
   */
  public List<VectorCASTCoverageResultSummary> getCoverageResults() {
    return coverageResults;
  }

  /**
   * @param coverageResults
   *          the list of coverage results to set
   */
  public void setCoverageResults(List<VectorCASTCoverageResultSummary> coverageResults) {
    this.coverageResults = coverageResults;
  }
}
