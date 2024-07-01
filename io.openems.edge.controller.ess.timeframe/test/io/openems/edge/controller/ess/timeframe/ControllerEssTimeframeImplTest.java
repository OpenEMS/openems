package io.openems.edge.controller.ess.timeframe;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ControllerEssTimeframeImplTest {

    private static final String CTRL_ID = "ctrl0";
    private static final String ESS_ID = "ess0";

    @Test
    public void testManual() throws OpenemsException, Exception {
        final var ess = new DummyManagedAsymmetricEss(ESS_ID);
        new ControllerTest(new ControllerEssTimeframeImpl()) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("ess", ess) //
                .activate(MyConfig.create() //
                        .setId(CTRL_ID) //
                        .setEssId(ESS_ID) //
                        .setMode(Mode.MANUAL) //
                        .setStartTime("2021-01-01T00:00:00Z") //
                        .setEndTime("2021-01-01T01:00:00Z") //
                        .setTargetSoC(50) //
                        .setPhase(Phase.ALL) //
                        .setRelationship(Relationship.EQUALS) //
                        .build()); //
    }

    @Test
    public void testOff() throws OpenemsException, Exception {
        new ControllerTest(new ControllerEssTimeframeImpl()) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("ess", new DummyManagedAsymmetricEss(ESS_ID)) //
                .activate(MyConfig.create() //
                        .setId(CTRL_ID) //
                        .setEssId(ESS_ID) //
                        .setMode(Mode.OFF) //
                        .setStartTime("2021-01-01T00:00:00Z") //
                        .setEndTime("2021-01-01T01:00:00Z") //
                        .setTargetSoC(50) //
                        .setPhase(Phase.ALL) //
                        .setRelationship(Relationship.EQUALS) //
                        .build()); //
    }
}
