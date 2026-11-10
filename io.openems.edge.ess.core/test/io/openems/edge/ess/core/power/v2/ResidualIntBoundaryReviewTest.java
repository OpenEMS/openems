package io.openems.edge.ess.core.power.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

/** Large feasible active-power targets with forced minimum and maximum constraints. */
public class ResidualIntBoundaryReviewTest {

    private static DummyManagedSymmetricEss ess(String id, int min, int max, int apparent) {
        return new DummyManagedSymmetricEss(id) //
                .withSoc(50) //
                .withAllowedChargePower(min) //
                .withAllowedDischargePower(max) //
                .withMaxApparentPower(apparent);
    }

    private static int output(PowerDistribution pd, String id) {
        return ((PowerDistribution.Entry.Actual) pd.getEntries().get(id)).activePowerSetPoint;
    }

    @Test
    public void testLargeFeasiblePositiveTargetWithForcedMinimum() {
        var a = ess("a", 0, 2_000_000_000, 2_000_000_000);
        var b = ess("b", 100_000_000, 100_000_000, 100_000_000);
        var root = new DummyMetaEss("root", a, b);
        var pd = PowerDistribution.from(List.of(root, a, b));
        pd.setEquals("root", 2_000_000_000);
        pd.solve();

        assertEquals(1_900_000_000, output(pd, "a"));
        assertEquals(100_000_000, output(pd, "b"));
        assertEquals(2_000_000_000L, (long) output(pd, "a") + output(pd, "b"));
    }

    @Test
    public void testLargeFeasibleNegativeTargetWithForcedMaximum() {
        var a = ess("a", -2_000_000_000, 0, 2_000_000_000);
        var b = ess("b", -100_000_000, -100_000_000, 100_000_000);
        var root = new DummyMetaEss("root", a, b);
        var pd = PowerDistribution.from(List.of(root, a, b));
        pd.setEquals("root", -2_000_000_000);
        pd.solve();

        assertEquals(-1_900_000_000, output(pd, "a"));
        assertEquals(-100_000_000, output(pd, "b"));
        assertEquals(-2_000_000_000L, (long) output(pd, "a") + output(pd, "b"));
        assertTrue(output(pd, "a") >= -2_000_000_000);
    }
}
