package com.vectorcast.plugins.vectorcastcoverage;

import java.io.Serializable;

/**
 * Holds the configuration details for {@link hudson.model.HealthReport} generation
 *
 * @author Stephen Connolly
 * @since 1.7
 */
public class VectorCASTHealthReportThresholds implements Serializable {
    private int minStatement;
    private int maxStatement;
    private int minBranch;
    private int maxBranch;
    private int minBasisPath;
    private int maxBasisPath;
    private int minMCDC;
    private int maxMCDC;
    private int minFunction;
    private int maxFunction;

    private int minFunctionCall;
    private int maxFunctionCall;

    public VectorCASTHealthReportThresholds() {
    }

    public VectorCASTHealthReportThresholds(int minStatement, int maxStatement, int minBranch, int maxBranch, int minBasisPath, int maxBasisPath, int minMCDC, int maxMCDC, int minFunction, int maxFunction, int minFunctionCall, int maxFunctionCall) {
        this.minStatement = minStatement;
        this.maxStatement = maxStatement;
        this.minBranch = minBranch;
        this.maxBranch = maxBranch;
        this.minBasisPath = minBasisPath;
        this.maxBasisPath = maxBasisPath;
        this.minMCDC = minMCDC;
        this.maxMCDC = maxMCDC;
        this.minFunction = minFunction;
        this.maxFunction = maxFunction;
        this.minFunctionCall = minFunctionCall;
        this.maxFunctionCall = maxFunctionCall;
        ensureValid();
    }

    private int applyRange(int min , int value, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public void ensureValid() {
        maxStatement = applyRange(0, maxStatement, 100);
        minStatement = applyRange(0, minStatement, maxStatement);
        maxBranch = applyRange(0, maxBranch, 100);
        minBranch = applyRange(0, minBranch, maxBranch);
        maxBasisPath = applyRange(0, maxBasisPath, 100);
        minBasisPath = applyRange(0, minBasisPath, maxBasisPath);
        maxMCDC = applyRange(0, maxMCDC, 100);
        minMCDC = applyRange(0, minMCDC, maxMCDC);
        maxFunction = applyRange(0, maxFunction, 100);
        minFunction = applyRange(0, minFunction, maxFunction);
        maxFunctionCall = applyRange(0, maxFunctionCall, 100);
        minFunctionCall = applyRange(0, minFunctionCall, maxFunctionCall);
    }

    public int getMinStatement() {
        return minStatement;
    }

    public void setMinStatement(int minStatement) {
        this.minStatement = minStatement;
    }

    public int getMaxStatement() {
        return maxStatement;
    }

    public void setMaxStatement(int maxStatement) {
        this.maxStatement = maxStatement;
    }

    public int getMinBranch() {
        return minBranch;
    }

    public void setMinBranch(int minBranch) {
        this.minBranch = minBranch;
    }

    public int getMaxBranch() {
        return maxBranch;
    }

    public void setMaxBranch(int maxBranch) {
        this.maxBranch = maxBranch;
    }

    public int getMinBasisPath() {
        return minBasisPath;
    }

    public void setMinBasisPath(int minBasisPath) {
        this.minBasisPath = minBasisPath;
    }

    public int getMaxBasisPath() {
        return maxBasisPath;
    }

    public void setMaxBasisPath(int maxBasisPath) {
        this.maxBasisPath = maxBasisPath;
    }

    public int getMinMCDC() {
        return minMCDC;
    }

    public void setMinMCDC(int minMCDC) {
        this.minMCDC = minMCDC;
    }

    public int getMaxMCDC() {
        return maxMCDC;
    }

    public void setMaxMCDC(int maxMCDC) {
        this.maxMCDC = maxMCDC;
    }
    
    public void setMinFunction(int minFunction) {
        this.minFunction = minFunction;
    }

    public int getMinFunction() {
        return minFunction;
    }

    public void setMaxFunction(int maxFunction) {
        this.maxFunction = maxFunction;
    }

    public int getMaxFunction() {
        return maxFunction;
    }
    
    public void setMinFunctionCall(int minFunctionCall) {
        this.minFunctionCall = minFunctionCall;
    }

    public int getMinFunctionCall() {
        return minFunctionCall;
    }

    public void setMaxFunctionCall(int maxFunctionCall) {
        this.maxFunctionCall = maxFunctionCall;
    }

    public int getMaxFunctionCall() {
        return maxFunctionCall;
    }

}
