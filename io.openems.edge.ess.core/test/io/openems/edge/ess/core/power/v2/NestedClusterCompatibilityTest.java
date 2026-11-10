package io.openems.edge.ess.core.power.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

/** Regression for nested ESS clusters with BMS-enforced minimum power. */
public class NestedClusterCompatibilityTest {

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

    private static PowerDistribution nested(int target, int forcedMin) {
        var a = ess("a", -10000, 10000, 10000);
        var b = ess("b", forcedMin, 10000, 10000);
        var c = ess("c", -10000, 10000, 10000);
        var d = ess("d", -10000, 10000, 10000);
        var sub1 = new DummyMetaEss("sub1", a, b);
        var sub2 = new DummyMetaEss("sub2", c, d);
        var root = new DummyMetaEss("root", sub1, sub2);
        var pd = PowerDistribution.from(List.of(root, sub1, sub2, a, b, c, d));
        pd.setEquals("root", target);
        TreeSolver.solve(pd.getEntries());
        return pd;
    }

    @Test
    public void testNestedClusterWithForcedBmsMaintainsFeasibleAggregate() {
        for (int target : new int[] { -5000, 0, 3000, 5000, 8000, 16000 }) {
            var pd = nested(target, 4000);
            var a = output(pd, "a");
            var b = output(pd, "b");
            var c = output(pd, "c");
            assertTrue(a >= -10000 && a <= 10000);
            assertTrue(b >= 4000 && b <= 10000);
            assertTrue(c >= -10000 && c <= 10000);
            var d = output(pd, "d");
            assertTrue(d >= -10000 && d <= 10000);
            assertEquals(target, a + b + c + d, "nested feasible target " + target);
        }
    }

}
