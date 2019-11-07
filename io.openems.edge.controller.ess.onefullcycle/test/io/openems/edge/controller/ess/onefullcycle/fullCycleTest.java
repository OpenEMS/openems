package io.openems.edge.controller.ess.onefullcycle;

import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class fullCycleTest {

    @SuppressWarnings("all")
    private static class MyConfig extends AbstractComponentConfig implements Config {
        private final String ess0_id;
        private int power;
        private CycleOrder cycleOrder;

        public MyConfig(String id, String ess0_id, CycleOrder cycleOrder, int power) {
            super(Config.class, id);
            this.ess0_id = ess0_id;
            this.power = power;
            this.cycleOrder = cycleOrder;
        }

        @Override
        public String ess_id() {
            return ess0_id;
        }

        @Override
        public CycleOrder cycleorder() {
            return this.cycleOrder;
        }

        @Override
        public String anyDateTime() {
            return anyDateTime();
        }

        @Override
        public int power() {
            return this.power;
        }

        @Override
        public boolean isAnyDateTimeEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFixedDayTimeEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int hour() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public DayOfWeek dayOfWeek() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Test
    public void test() throws Exception {
        // Initialize mocked Clock
        final TimeLeapClock clock = new TimeLeapClock(ZoneOffset.UTC);

        // initialize the controller
        EssOneFullCycle controller = new EssOneFullCycle(clock);
        // Add referenced services
        DummyComponentManager componentManager = new DummyComponentManager();
        controller.componentManager = componentManager;

        ManagedSymmetricEss ess0 = new DummyManagedSymmetricEss("ess0");
//        LocalDate setDate = LocalDate.of(2019, 9, 19);
//        LocalTime setTime = LocalTime.of(13, 50);
//        LocalTime nowTime = LocalTime.now();
        ChannelAddress cycleOrder = new ChannelAddress("ctrlOneFullCycle0", "CycleOrder");
        ChannelAddress ess0Soc = new ChannelAddress("ess0", "Soc");
        ChannelAddress activePower = new ChannelAddress("ess0", "ActivePower");
//        ChannelAddress maxChargePower = new ChannelAddress("ess0", "AllowedChargePower");
        ChannelAddress maxDischargePower = new ChannelAddress("ess0", "AllowedDischargePower");
        MyConfig myconfig = new MyConfig("ctrl1", ess0.id(), CycleOrder.START_WITH_DISCHARGE, 10000);
        controller.activate(null, myconfig);

        // Build and run test
        new ControllerTest(controller, componentManager, ess0) //
                .next(new TestCase()//
                        .timeleap(clock, 1, ChronoUnit.MONTHS)//
                        .input(ess0Soc, 100)//
                        .input(maxDischargePower, 10000)//
                        .input(cycleOrder, CycleOrder.START_WITH_DISCHARGE) //
                        .output(activePower, 10000))//
                .run();
    }

}