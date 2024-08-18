package io.openems.edge.controller.ess.timeframe;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


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

    @Test
    public void testLimitChargePower() throws InvalidValueException {

        var ess = new DummyManagedSymmetricEss(ESS_ID)
                .withCapacity(10000)
                .withSoc(25)
                .withActivePower(0)
                .withMaxApparentPower(10000);

        assertEquals(Integer.valueOf(-1000), //
                ControllerEssTimeframeImpl.getAcPower(
                        ess,
                        0,
                        50,
                        1000,
                        0,
                        this.getIso8601String(this.now()),
                        this.getIso8601String(this.inOneHour())
                ));


        Integer acPower = ControllerEssTimeframeImpl.getAcPower(
                ess,
                0,
                50,
                0,
                0,
                this.getIso8601String(this.now()),
                this.getIso8601String(this.inOneHour())
        );
        assertNotNull(acPower);
        assertTrue(acPower < -1000);

    }

    @Test
    public void testLimitDischargePower() throws InvalidValueException {

        var ess = new DummyManagedSymmetricEss(ESS_ID)
                .withCapacity(10000)
                .withSoc(75)
                .withActivePower(0)
                .withMaxApparentPower(10000);

        assertEquals(Integer.valueOf(1000), //
                ControllerEssTimeframeImpl.getAcPower(
                        ess,
                        0,
                        50,
                        0,
                        1000,
                        this.getIso8601String(this.now()),
                        this.getIso8601String(this.inOneHour())
                ));


        Integer acPower = ControllerEssTimeframeImpl.getAcPower(
                ess,
                0,
                50,
                0,
                0,
                this.getIso8601String(this.now()),
                this.getIso8601String(this.inOneHour())
        );
        assertNotNull(acPower);
        assertTrue(acPower > 1000);

    }


    private Date now() {
        return new Date();
    }

    private Date inOneHour() {
        return new Date(this.now().getTime() + 3600 * 1000);
    }


    private String getIso8601String(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }
}
