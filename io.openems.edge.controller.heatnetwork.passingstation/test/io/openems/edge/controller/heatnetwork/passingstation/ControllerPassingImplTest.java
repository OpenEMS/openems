package io.openems.edge.controller.heatnetwork.passingstation;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.heatsystem.components.test.DummyPump;
import io.openems.edge.heatsystem.components.test.DummyValve;
import io.openems.edge.pwm.api.Pwm;
import io.openems.edge.pwm.api.test.DummyPwm;
import io.openems.edge.relay.api.Relay;
import io.openems.edge.relay.api.test.DummyRelay;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import static org.junit.Assert.fail;


public class ControllerPassingImplTest {

    private static class ConfigOfPassing extends AbstractComponentConfig implements Config {

        private final String id;
        private final String alias;
        private final boolean enabled;
        private final String service_pid;
        private String primary_Forward_Sensor;
        private final String primary_Rewind_Sensor;
        private final String secundary_Forward_Sensor;
        private final String secundary_Rewind_Sensor;
        private String valveId;
        private final String pumpId;
        private int heating_Time;


        public ConfigOfPassing(String id, String alias, boolean enabled, String service_pid, String primary_Forward_Sensor, String primary_Rewind_Sensor, String secundary_Forward_Sensor, String secundary_Rewind_Sensor, String valveId,
                               String pumpId) {
            super(Config.class, id);
            this.id = id;
            this.alias = alias;
            this.enabled = enabled;
            this.service_pid = service_pid;
            this.primary_Forward_Sensor = primary_Forward_Sensor;
            this.primary_Rewind_Sensor = primary_Rewind_Sensor;
            this.secundary_Forward_Sensor = secundary_Forward_Sensor;
            this.secundary_Rewind_Sensor = secundary_Rewind_Sensor;
            this.valveId = valveId;
            this.pumpId = pumpId;
        }


        @Override
        public String service_pid() {
            return this.service_pid;
        }

        @Override
        public String primary_Forward_Sensor() {
            return this.primary_Forward_Sensor;
        }

        @Override
        public String primary_Rewind_Sensor() {
            return primary_Rewind_Sensor;
        }

        @Override
        public String secundary_Forward_Sensor() {
            return secundary_Forward_Sensor;
        }

        @Override
        public String secundary_Rewind_Sensor() {
            return secundary_Rewind_Sensor;
        }

        @Override
        public String valve_id() {
            return valveId;
        }

        @Override
        public String pump_id() {
            return pumpId;
        }

        @Override
        public int heating_Time() {
            return this.heating_Time;
        }
    }

    private static ControllerPassingImpl passing;
    private static DummyComponentManager cpm;
    private Thermometer primaryForward;
    private Thermometer primaryRewind;
    private Thermometer secundaryForward;
    private Thermometer secundaryRewind;
    private Thermometer testForFailR;
    private Relay pumpRelays;
    private Relay valveOpen;
    private Relay valveClose;
    private Relay testForFailT;
    private Pwm pwm;
    private ConfigOfPassing config;
    private ChannelAddress pF;
    private ChannelAddress pR;
    private ChannelAddress sF;
    private ChannelAddress sR;
    private ChannelAddress p;
    private ChannelAddress pO;
    private ChannelAddress vO;
    private ChannelAddress vOC;
    private ChannelAddress vC;
    private ChannelAddress vCc;
    private Valve valve;
    private Pump pump;
    private ChannelAddress noError;
    private ChannelAddress minTemperature;
    private ChannelAddress mainActivate;

    @Before
    public void setUp() throws Exception {
        passing = new ControllerPassingImpl();
        cpm = new DummyComponentManager();
        passing.cpm = cpm;

        config = new ConfigOfPassing("ControllerPassing0", "Uebergabestation", true, "",
                "TemperatureSensor0", "TemperatureSensor1",
                "TemperatureSensor2", "TemperatureSensor3",
                "Valve0", "Pump0");

        primaryForward = new DummyThermometer(config.primary_Forward_Sensor());
        primaryRewind = new DummyThermometer(config.primary_Rewind_Sensor());
        secundaryForward = new DummyThermometer(config.secundary_Forward_Sensor());
        secundaryRewind = new DummyThermometer(config.secundary_Rewind_Sensor());
        valveClose = new DummyRelay("Relays0");
        valveOpen = new DummyRelay("Relays1");
        pumpRelays = new DummyRelay("Relays2");
        pwm = new DummyPwm("PwmDevice0");
        testForFailR = new DummyThermometer("TemperatureSensor4");
        testForFailT = new DummyRelay("Relays4");


        valve = new DummyValve(valveOpen, valveClose, "Valve0", 1);


        pF = new ChannelAddress(config.primary_Forward_Sensor(), "Temperature");
        pR = new ChannelAddress(config.primary_Rewind_Sensor(), "Temperature");
        sF = new ChannelAddress(config.secundary_Forward_Sensor(), "Temperature");
        sR = new ChannelAddress(config.secundary_Rewind_Sensor(), "Temperature");

        //valveOpen Channel
        vO = new ChannelAddress(valveOpen.id(), "OnOff");
        vOC = new ChannelAddress(valveOpen.id(), "IsCloser");

        //valveClosing Channel
        vC = new ChannelAddress(valveClose.id(), "OnOff");
        vCc = new ChannelAddress(valveClose.id(), "IsCloser");

        //PumpRelays channel
        p = new ChannelAddress(pumpRelays.id(), "OnOff");
        pO = new ChannelAddress(pumpRelays.id(), "IsCloser");

        //
        //PassingStation
        noError = new ChannelAddress(config.id(), "NoError");
        minTemperature = new ChannelAddress(config.id(), "MinTemperature");
        mainActivate = new ChannelAddress(config.id(), "OnOff");


        cpm.addComponent(primaryForward);
        cpm.addComponent(primaryRewind);
        cpm.addComponent(secundaryForward);
        cpm.addComponent(secundaryRewind);
        cpm.addComponent(valveOpen);
        cpm.addComponent(valveClose);
        cpm.addComponent(pumpRelays);
        cpm.addComponent(valve);
        cpm.addComponent(testForFailT);
        cpm.addComponent(testForFailR);

        valveClose.isCloser().setNextValue(true);
        valveOpen.isCloser().setNextValue(true);
        pumpRelays.isCloser().setNextValue(true);

    }

    /*	7 TestCases

     * 1. Everythings working fine On
       2. Everythings working fine first On then off
     * 3. Too Hot --> Everythings fine Except T SR + Buffer >0 T SF
     * 4. Valve Defect Exception
     * 5. Heat To Low
     * 6. Configuration Exception
     * 6.1 Configuration Exception Thermometer
     * 6.2 Configuration Exception Relays
     *
     */

    @Test
    public void testEverythingsFineOnPumpIsRelays() {
        try {
            pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
            cpm.addComponent(pump);
            primaryRewind.getTemperature().setNextValue(200);
            primaryForward.getTemperature().setNextValue(700);
            passing.activate(null, config);
            passing.activate(null, config);
            passing.getMinTemperature().setNextValue(500);
            passing.getOnOff().setNextWriteValue(true);

            AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                    secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                    new TestCase()
                            .input(pF, 700)
                            .input(pR, 400)
                            .input(sF, 650)
                            .input(sR, 500)
                            .input(vO, false)
                            .input(vOC, true)
                            .input(vC, false)
                            .input(vCc, true)
                            .input(p, false)
                            .input(pO, true)
                            .input(noError, true)
                            .input(mainActivate, true)
                            .input(minTemperature, 500)
            );
            passing.activate(null, config);
            int count = 0;
            while (count < 2) {
                controllerTest.run();
                Thread.sleep(1000);
                count++;
            }
        } catch (Exception e) {
            fail();
        }
        if (!passing.noError().getNextValue().get()) {
            fail();
        }
        passing.deactivate();
        //Bc of waiting time outputs can't be controlled, but as long no exception is thrown everythings fine
        Assert.assertTrue(true);


    }

    @Test
    public void testEverythingsFineOnRelaysOpener() {
        try {
            pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
            cpm.addComponent(pump);
            primaryRewind.getTemperature().setNextValue(200);
            primaryForward.getTemperature().setNextValue(700);
            passing.activate(null, config);
            passing.activate(null, config);
            passing.getMinTemperature().setNextValue(500);
            passing.getOnOff().setNextWriteValue(true);

            AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                    secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                    new TestCase()
                            .input(pF, 700)
                            .input(pR, 400)
                            .input(sF, 650)
                            .input(sR, 500)
                            .input(vO, false)
                            .input(vOC, false)
                            .input(vC, false)
                            .input(vCc, false)
                            .input(p, false)
                            .input(pO, false)
                            .input(noError, true)
                            .input(mainActivate, true)
                            .input(minTemperature, 500)
            );
            passing.activate(null, config);
            int count = 0;
            while (count < 2) {
                controllerTest.run();
                Thread.sleep(1000);
                count++;
            }
        } catch (Exception e) {
            fail();
        }
        passing.deactivate();
        //Bc of waiting time outputs can't be controlled, but as long no exception is thrown everythings fine
        Assert.assertTrue(true);


    }


    @Test
    public void testEverythingsFineOffWithWaitingTime() {
        try {
            pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
            cpm.addComponent(pump);

            primaryRewind.getTemperature().setNextValue(200);
            primaryForward.getTemperature().setNextValue(700);
            passing.activate(null, config);
            passing.activate(null, config);
            passing.getMinTemperature().setNextWriteValue(500);
            passing.getOnOff().setNextWriteValue(false);

            AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                    secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                    new TestCase()
                            .input(pF, 700)
                            .input(pR, 400)
                            .input(sF, 650)
                            .input(sR, 500)
                            .input(vO, false)
                            .input(vOC, true)
                            .input(vC, false)
                            .input(vCc, true)
                            .input(p, false)
                            .input(pO, true)
                            .input(noError, true)
                            .input(mainActivate, true)
                            .input(minTemperature, 500)
            );
            passing.activate(null, config);
            int count = 0;
            while (count < 20) {
                controllerTest.run();
                Thread.sleep(1000);
                count++;
            }
        } catch (Exception e) {
            fail();
        }
        passing.deactivate();
        //Bc of waiting time outputs can't be controlled, but as long no exception is thrown everythings fine
        Assert.assertTrue(true);


    }

    @Test
    public void testEverythingsFineOff() {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);

        try {
            pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
            cpm.addComponent(pump);
            primaryRewind.getTemperature().setNextValue(200);
            passing.activate(null, config);
            passing.activate(null, config);
            passing.getMinTemperature().setNextWriteValue(500);
            passing.getOnOff().setNextWriteValue(true);

            AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                    secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                    new TestCase()
                            .input(pF, 700)
                            .input(pR, 400)
                            .input(sF, 650)
                            .input(sR, 500)
                            .input(vO, false)
                            .input(vOC, true)
                            .input(vC, false)
                            .input(vCc, true)
                            .input(p, false)
                            .input(pO, true)
                            .input(noError, true)
                            .input(mainActivate, true)
                            .input(minTemperature, 500)
            );
            passing.activate(null, config);
            controllerTest.run();
            passing.getOnOff().setNextWriteValue(false);
            valve.getIsBusy().setNextValue(false);
            int count = 0;
            while (count < 2) {

                controllerTest.run();
                Thread.sleep(1000);
                count++;
            }
        } catch (Exception e) {
            fail();
        }
        //Bc of waiting time outputs can't be controlled, but as long no exception is thrown everythings fine
        Assert.assertTrue(true);


    }

    @Test(expected = io.openems.common.exceptions.NoHeatNeededException.class)
    public void testTooHot() throws Exception {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);
        primaryRewind.getTemperature().setNextValue(200);
        passing.activate(null, config);
        passing.activate(null, config);
        passing.getMinTemperature().setNextWriteValue(500);
        passing.getOnOff().setNextWriteValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                new TestCase()
                        .input(pF, 900)
                        .input(pR, 800)
                        .input(sF, 650)
                        .input(sR, 650)
                        .input(vO, false)
                        .input(vOC, true)
                        .input(vC, false)
                        .input(vCc, true)
                        .input(p, false)
                        .input(pO, true)
                        .input(noError, true)
                        .input(mainActivate, true)
                        .input(minTemperature, 500)
        );
        passing.activate(null, config);
        int count = 0;
        while (count < 12) {
            controllerTest.run();
            Thread.sleep(10000);
            count++;
        }

    }

    @Test(expected = io.openems.common.exceptions.NoHeatNeededException.class)
    public void testTooHotPumpOpener() throws Exception {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);
        primaryForward.getTemperature().setNextValue(700);
        primaryRewind.getTemperature().setNextValue(200);
        passing.activate(null, config);
        passing.activate(null, config);
        passing.getMinTemperature().setNextWriteValue(500);
        passing.getOnOff().setNextWriteValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                new TestCase()
                        .input(pF, 900)
                        .input(pR, 800)
                        .input(sF, 650)
                        .input(sR, 650)
                        .input(vO, false)
                        .input(vOC, true)
                        .input(vC, false)
                        .input(vCc, true)
                        .input(p, false)
                        .input(pO, false)
                        .input(noError, true)
                        .input(mainActivate, true)
                        .input(minTemperature, 500)
        );
        passing.activate(null, config);
        int count = 0;
        while (count < 15) {
            controllerTest.run();
            Thread.sleep(10000);
            count++;
        }

    }

    @Test(expected = io.openems.common.exceptions.HeatToLowException.class)
    public void testHeatToLow() throws Exception {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);
        primaryForward.getTemperature().setNextValue(700);
        primaryRewind.getTemperature().setNextValue(200);
        passing.activate(null, config);
        passing.activate(null, config);
        passing.getMinTemperature().setNextWriteValue(500);
        passing.getOnOff().setNextWriteValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                new TestCase()
                        .input(pF, 400)
                        .input(pR, 400)
                        .input(sF, 200)
                        .input(sR, 200)
                        .input(vO, false)
                        .input(vOC, true)
                        .input(vC, false)
                        .input(vCc, true)
                        .input(p, false)
                        .input(pO, true)
                        .input(noError, true)
                        .input(mainActivate, true)
                        .input(minTemperature, 500)
        );
        passing.activate(null, config);
        int count = 0;
        while (count < 15) {
            controllerTest.run();
            Thread.sleep(10000);
            count++;
        }
    }

    @Test(expected = io.openems.common.exceptions.ValveDefectException.class)
    public void testValveDefect() throws Exception {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);
        config.heating_Time = 1;
        primaryForward.getTemperature().setNextValue(700);
        primaryRewind.getTemperature().setNextValue(200);
        passing.activate(null, config);
        passing.activate(null, config);
        passing.getMinTemperature().setNextWriteValue(500);
        passing.getOnOff().setNextWriteValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                new TestCase()
                        .input(pF, 400)
                        .input(pR, 200)
                        .input(sF, 200)
                        .input(sR, 200)
                        .input(vO, false)
                        .input(vOC, true)
                        .input(vC, false)
                        .input(vCc, true)
                        .input(p, false)
                        .input(pO, true)
                        .input(noError, true)
                        .input(mainActivate, true)
                        .input(minTemperature, 500)
        );
        passing.activate(null, config);
        int count = 0;
        while (count < 15) {
            controllerTest.run();
            Thread.sleep(10000);
            count++;
        }

    }

    @Test(expected = ConfigurationException.class)
    public void testThermometerException() throws Exception {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);
        primaryForward.getTemperature().setNextValue(700);
        config.primary_Forward_Sensor = "Relays4";
        primaryRewind.getTemperature().setNextValue(200);
        passing.activate(null, config);
        passing.activate(null, config);
        passing.getMinTemperature().setNextWriteValue(500);
        passing.getOnOff().setNextWriteValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                new TestCase()
                        .input(pF, 400)
                        .input(pR, 400)
                        .input(sF, 200)
                        .input(sR, 200)
                        .input(vO, false)
                        .input(vOC, true)
                        .input(vC, false)
                        .input(vCc, true)
                        .input(p, false)
                        .input(pO, true)
                        .input(noError, true)
                        .input(mainActivate, true)
                        .input(minTemperature, 500)
        );
        passing.activate(null, config);
        int count = 0;
        while (count < 8) {
            controllerTest.run();
            Thread.sleep(1000);
            count++;
        }
    }

    @Test(expected = ConfigurationException.class)
    public void testRelaysException() throws Exception {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);
        config.valveId = "TemperatureSensor4";
        primaryForward.getTemperature().setNextValue(700);
        primaryRewind.getTemperature().setNextValue(200);
        passing.activate(null, config);
        passing.activate(null, config);
        passing.getMinTemperature().setNextWriteValue(500);
        passing.getOnOff().setNextWriteValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                new TestCase()
                        .input(pF, 400)
                        .input(pR, 400)
                        .input(sF, 200)
                        .input(sR, 200)
                        .input(vO, false)
                        .input(vOC, true)
                        .input(vC, false)
                        .input(vCc, true)
                        .input(p, false)
                        .input(pO, true)
                        .input(noError, true)
                        .input(mainActivate, true)
                        .input(minTemperature, 500)
        );
        passing.activate(null, config);
        int count = 0;
        while (count < 8) {
            controllerTest.run();
            Thread.sleep(1000);
            count++;
        }
    }

    @Test(expected = OpenemsError.OpenemsNamedException.class)
    public void testComponentManager() throws Exception {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Relays");
        cpm.addComponent(pump);
        primaryForward.getTemperature().setNextValue(700);
        config.valveId = "Relays6";
        primaryRewind.getTemperature().setNextValue(200);
        passing.activate(null, config);
        passing.activate(null, config);
        passing.getMinTemperature().setNextWriteValue(500);
        passing.getOnOff().setNextWriteValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                new TestCase()
                        .input(pF, 400)
                        .input(pR, 400)
                        .input(sF, 200)
                        .input(sR, 200)
                        .input(vO, false)
                        .input(vOC, true)
                        .input(vC, false)
                        .input(vCc, true)
                        .input(p, false)
                        .input(pO, true)
        );
        int count = 0;
        while (count < 8) {
            controllerTest.run();
            Thread.sleep(1000);
            count++;
        }
    }

    @Test
    public void testEverythingsFineOnPumpIsBoth() {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Both");
        cpm.addComponent(pump);
        try {
            primaryRewind.getTemperature().setNextValue(200);
            primaryForward.getTemperature().setNextValue(700);
            passing.activate(null, config);
            passing.activate(null, config);
            passing.getMinTemperature().setNextWriteValue(500);
            passing.getOnOff().setNextWriteValue(true);

            AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                    secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                    new TestCase()
                            .input(pF, 700)
                            .input(pR, 400)
                            .input(sF, 650)
                            .input(sR, 500)
                            .input(vO, false)
                            .input(vOC, true)
                            .input(vC, false)
                            .input(vCc, true)
                            .input(p, false)
                            .input(pO, true)
                            .input(noError, true)
                            .input(mainActivate, true)
                            .input(minTemperature, 500)
            );
            passing.activate(null, config);
            int count = 0;
            while (count < 2) {
                controllerTest.run();
                Thread.sleep(1000);
                count++;
            }
        } catch (Exception e) {
            fail();
        }
        passing.deactivate();
        //Bc of waiting time outputs can't be controlled, but as long no exception is thrown everythings fine
        Assert.assertTrue(true);


    }


    @Test
    public void testEverythingsFineOnPumpIsPwm() {
        pump = new DummyPump("Pump0", pumpRelays, pwm, "Pwm");
        cpm.addComponent(pump);
        try {
            primaryRewind.getTemperature().setNextValue(200);
            primaryForward.getTemperature().setNextValue(700);
            passing.activate(null, config);
            passing.activate(null, config);
            passing.getMinTemperature().setNextWriteValue(500);
            passing.getOnOff().setNextWriteValue(true);

            AbstractComponentTest controllerTest = new ControllerTest(passing, cpm, primaryForward, primaryRewind, secundaryForward,
                    secundaryRewind, valveOpen, valveClose, pumpRelays, passing).next(
                    new TestCase()
                            .input(pF, 700)
                            .input(pR, 400)
                            .input(sF, 650)
                            .input(sR, 500)
                            .input(vO, false)
                            .input(vOC, true)
                            .input(vC, false)
                            .input(vCc, true)
                            .input(p, false)
                            .input(pO, true)
                            .input(noError, true)
                            .input(mainActivate, true)
                            .input(minTemperature, 500)
            );
            passing.activate(null, config);
            int count = 0;
            while (count < 2) {
                controllerTest.run();
                Thread.sleep(1000);
                count++;
            }
        } catch (Exception e) {
            fail();
        }
        passing.deactivate();
        //Bc of waiting time outputs can't be controlled, but as long no exception is thrown everythings fine
        Assert.assertTrue(true);


    }

}
