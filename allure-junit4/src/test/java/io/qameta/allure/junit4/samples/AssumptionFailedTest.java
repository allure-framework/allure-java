package io.qameta.allure.junit4.samples;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

/**
 * @author gladnik (Nikolai Gladkov)
 */
public class AssumptionFailedTest {

    @Test
    public void assumptionFailedTest() {
        assumeThat(true, is(false));
    }
}
