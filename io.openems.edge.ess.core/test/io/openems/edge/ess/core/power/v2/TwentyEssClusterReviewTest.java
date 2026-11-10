package io.openems.edge.ess.core.power.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

/** Regression for power conservation across a cluster of 20 ESS units. */
public class TwentyEssClusterReviewTest {

    @Test
    public void testTwentyEssWithForcedMinimumConservesTarget() {
        var members = new ArrayList<ManagedSymmetricEss>();
        for (int i = 0; i < 20; i++) {
            var min = i == 19 ? 4000 : -10000;
            members.add(new DummyManagedSymmetricEss("ess" + i) //
                    .withSoc(50) //
                    .withAllowedChargePower(min) //
                    .withAllowedDischargePower(10000) //
                    .withMaxApparentPower(10000));
        }
        var cluster = new DummyMetaEss("cluster", members.toArray(SymmetricEss[]::new));
        var all = new ArrayList<ManagedSymmetricEss>();
        all.add(cluster);
        all.addAll(members);
        var pd = PowerDistribution.from(all);
        pd.setEquals("cluster", 5000);
        pd.solve();

        long sum = 0;
        for (var member : members) {
            var value = ((PowerDistribution.Entry.Actual) pd.getEntries().get(member.id())).activePowerSetPoint;
            assertTrue(value >= (member.id().equals("ess19") ? 4000 : -10000));
            assertTrue(value <= 10000);
            sum += value;
        }
        assertEquals(5000L, sum);
    }
}
