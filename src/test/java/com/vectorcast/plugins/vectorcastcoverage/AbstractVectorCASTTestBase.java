package com.vectorcast.plugins.vectorcastcoverage;

import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractVectorCASTTestBase extends TestCase {
    protected final void assertRatio(Ratio r, float numerator, float denominator) {
        assertEquals("Numerator doesn't match.",numerator, r.getNumerator());
        assertEquals("Denominator doesn't match.",denominator, r.getDenominator());
    }
}
