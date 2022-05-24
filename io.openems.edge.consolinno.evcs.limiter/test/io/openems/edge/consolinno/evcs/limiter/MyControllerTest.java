package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyAsymmetricMeter;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class MyControllerTest {
    private static final String id = "test";
    private static final String evcsId = "evcs0";
    private static final String nonPriorityId = "nonPriority0";
    private static final String meterId = "meter";
    private static final String symmetryMeterId = "symmetryMeter";
    private static final int symmetryOffset = 0;
    private static final ChannelAddress CHARGE_POWER = new ChannelAddress(evcsId, "ChargePower");
    private static final ChannelAddress PHASES = new ChannelAddress(evcsId, "Phases");
    private static final ChannelAddress NON_PRIORITY_CHARGE_POWER = new ChannelAddress(nonPriorityId, "ChargePower");
    private static final ChannelAddress NON_PRIORITY_PHASES = new ChannelAddress(nonPriorityId, "Phases");
    private static final ChannelAddress POWERLIMIT = new ChannelAddress(id, "PowerLimit");
    private static final ChannelAddress METER_POWER = new ChannelAddress(meterId, "ActivePower");
    private DummyManagedEvcs evcs;
    private DummyManagedEvcs nonPriority;
    //private static final ChannelAddress output = new ChannelAddress(outputComponentId, outputChannelId);


    @Before
    public void setup() {
        this.evcs = new DummyManagedEvcs(evcsId, new DummyEvcsPower());
        this.evcs.setPriority(true);
        this.evcs.setMinimumHardwarePower(6800);
        this.evcs.setMinimumPower(6800);
        this.nonPriority = new DummyManagedEvcs(nonPriorityId, new DummyEvcsPower());
        this.nonPriority.setPriority(false);
        this.nonPriority.setMinimumHardwarePower(6800);
        this.nonPriority.setMinimumPower(6800);

    }

    @Test
    public void initialTest() throws Exception {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);

        new ComponentTest(test)
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(this.evcs)
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setEvcss(new String[]{evcsId})
                        .setUseMeter(false)
                        .setMeter("")
                        .setGrid(GridVoltage.V_230_HZ_50)
                        .setSymmetry(true)
                        .setSymmetryOffset(0)
                        .setSymmetryMeter(symmetryMeterId)
                        .setOffTime(20)
                        .setPhaseLimit(16 * 230)
                        .setPowerLimit(32 * 230)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                )
                .modified(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setGrid(GridVoltage.V_230_HZ_50)
                        .setEvcss(new String[]{evcsId})
                        .setUseMeter(false)
                        .setMeter("")
                        .setSymmetryOffset(0)
                        .setSymmetryMeter(symmetryMeterId)
                        .setSymmetry(true)
                        .setOffTime(20)
                        .setPhaseLimit(16 * 230)
                        .setPowerLimit(32 * 230)
                        .build())
                .next(new TestCase()
                )
                .next(new TestCase())
        ;

    }

    @Test
    public void balanceTest() throws Exception {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
        new ComponentTest(test)
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(this.evcs)
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setEvcss(new String[]{evcsId})
                        .setUseMeter(false)
                        .setMeter("")
                        .setSymmetryOffset(0)
                        .setSymmetryMeter(symmetryMeterId)
                        .setSymmetry(true)
                        .setGrid(GridVoltage.V_230_HZ_50)
                        .setOffTime(20)
                        .setPhaseLimit(10 * 230)
                        .setPowerLimit(7 * 230)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 1)
                        .input(CHARGE_POWER, 6 * 230)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 1)
                        .input(POWERLIMIT, 61 * 230)
                        .input(CHARGE_POWER, 8 * 230)
                )

                .next(new TestCase()
                        .input(PHASES, 2)
                        .input(POWERLIMIT, 61 * 230)
                        .input(CHARGE_POWER, 60 * 230)
                )
                .next(new TestCase()
                        .input(PHASES, 3)
                        .input(POWERLIMIT, 61 * 230)
                        .input(CHARGE_POWER, 90 * 230))

        ;

    }

    @Test
    public void meterTest() throws Throwable {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
        new ComponentTest(test)
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(this.evcs)
                .addComponent(new DummyAsymmetricMeter(meterId))
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setEvcss(new String[]{evcsId})
                        .setUseMeter(true)
                        .setMeter(meterId)
                        .setSymmetry(true)
                        .setSymmetryOffset(0)
                        .setSymmetryMeter(symmetryMeterId)
                        .setOffTime(20)
                        .setGrid(GridVoltage.V_230_HZ_50)
                        .setPhaseLimit(16 * 230)
                        .setPowerLimit(32 * 230)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 3)
                        .input(CHARGE_POWER, 30 * 230)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 3)
                        .input(CHARGE_POWER, 30 * 230)
                        .input(METER_POWER, 40 * 230)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 3)
                        .input(CHARGE_POWER, 30 * 230)
                        .input(METER_POWER, 40 * 230)
                )

        ;

    }

    @Test
    public void priorityWithNonPriorityTest() throws Throwable {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);

        new ComponentTest(test)
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(this.evcs)
                .addComponent(this.nonPriority)
                .addComponent(new DummyAsymmetricMeter(meterId))
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setEvcss(new String[]{evcsId, nonPriorityId})
                        .setUseMeter(false)
                        .setSymmetryOffset(0)
                        .setSymmetryMeter(symmetryMeterId)
                        .setMeter(meterId)
                        .setSymmetry(true)
                        .setOffTime(20)
                        .setGrid(GridVoltage.V_230_HZ_50)
                        .setPhaseLimit(16 * 230)
                        .setPowerLimit(32 * 230)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 3)
                        .input(CHARGE_POWER, 30 * 230)
                        .input(NON_PRIORITY_PHASES, 3)
                        .input(NON_PRIORITY_CHARGE_POWER, 30 * 230)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 3)
                        .input(CHARGE_POWER, 30 * 230)
                        .input(NON_PRIORITY_PHASES, 3)
                        .input(NON_PRIORITY_CHARGE_POWER, 30 * 230)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 3)
                        .input(CHARGE_POWER, 30 * 230)
                        .input(NON_PRIORITY_PHASES, 3)
                        .input(NON_PRIORITY_CHARGE_POWER, 30 * 230)
                )
        ;
    }

}