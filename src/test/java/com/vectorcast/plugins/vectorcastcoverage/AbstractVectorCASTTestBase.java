package com.vectorcast.plugins.vectorcastcoverage;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractVectorCASTTestBase  {
    protected final void assertRatio(Ratio r, float numerator, float denominator) {
        Assert.assertEquals("Numerator doesn't match.",numerator, r.getNumerator());
        Assert.assertEquals("Denominator doesn't match.",denominator, r.getDenominator());
    }
}
