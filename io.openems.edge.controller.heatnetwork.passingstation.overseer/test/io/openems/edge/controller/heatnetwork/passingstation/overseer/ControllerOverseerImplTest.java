package io.openems.edge.controller.heatnetwork.passingstation.overseer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.heatnetwork.passingstation.api.DummyControllerPassing;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.relay.api.Relay;
import io.openems.edge.relay.api.test.DummyRelay;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ControllerOverseerImplTest {

    private static class MyConfig extends AbstractComponentConfig implements Config {

        private final String id;
        private final String alias;
        private final boolean enabled;
        private final String service_pid;
        private String allocated_Passing_Controller;
        private final int minTemperature;
        private final int toleratedTemperatureRange;
        private String []allocatedTemperatureSensor;

        MyConfig(String id, String alias, boolean enabled, String service_pid, String allocated_Passing_Controller, int minTemperature, int toleratedTemperatureRange, String [] allocatedTemperatureSensor) {
            super(Config.class, id);
            this.id = id;
            this.alias = alias;
            this.enabled = enabled;
            this.service_pid = service_pid;
            this.allocated_Passing_Controller = allocated_Passing_Controller;
            this.minTemperature = minTemperature;
            this.toleratedTemperatureRange = toleratedTemperatureRange;
            this.allocatedTemperatureSensor = allocatedTemperatureSensor;
        }

        @Override
        public String service_pid() {
            return this.service_pid;
        }

        @Override
        public String allocated_Passing_Controller() {
            return this.allocated_Passing_Controller;
        }

        @Override
        public int min_Temperature() {
            return this.minTemperature;
        }

        @Override
        public int tolerated_Temperature_Range() {
            return this.toleratedTemperatureRange;
        }

        @Override
        public String[] allocated_Temperature_Sensor() {
            return this.allocatedTemperatureSensor;
        }
    }

    private static ControllerOverseerImpl overseer;
    private static DummyComponentManager cpm;
    private Thermometer allocatedThermometer;
    private ChannelAddress thermometer;
    private DummyControllerPassing passing;
    private ChannelAddress passingOnOff;
    private ChannelAddress passingMinTemp;
    private ChannelAddress passingNoError;
    private Relay allocatedRelays;
    private ChannelAddress relaysOnOff;
    private ChannelAddress relaysIsCloser;
    private MyConfig config;
    private Thermometer errorT;
    private Relay errorR;

    @Before
    public void setUp() throws Exception {

        overseer = new ControllerOverseerImpl();
        cpm = new DummyComponentManager();
        overseer.cpm = cpm;

        config = new MyConfig("ControllerOverseer0", "", true, "", "ControllerPassing0",
                400, 20, new String[]{"TemperatureSensor8"});
        String temperatureSensor = config.allocatedTemperatureSensor[0];
        allocatedThermometer = new DummyThermometer(temperatureSensor);
        passing = new DummyControllerPassing(config.allocated_Passing_Controller());
        allocatedRelays = new DummyRelay("Relays1");
        passingOnOff = new ChannelAddress(config.allocated_Passing_Controller(), "OnOff");
        passingMinTemp = new ChannelAddress(config.allocated_Passing_Controller(), "MinTemperature");
        passingNoError = new ChannelAddress(config.allocated_Passing_Controller(), "NoError");
        relaysOnOff = new ChannelAddress("Relays1", "OnOff");
        relaysIsCloser = new ChannelAddress("Relays1", "IsCloser");
        thermometer = new ChannelAddress(temperatureSensor, "Temperature");
        errorR = new DummyRelay("Relays20");
        errorT = new DummyThermometer("TemperatureSensor20");

        cpm.addComponent(allocatedThermometer);
        cpm.addComponent(passing);
        cpm.addComponent(allocatedRelays);
        cpm.addComponent(errorR);
        cpm.addComponent(errorT);
    }

    @Test
    public void testHeatingNotReachedNoError() {
        try {

            overseer.activate(null, config);
            overseer.activate(null, config);
            overseer.passing.getMinTemperature().setNextValue(config.minTemperature);
            overseer.passing.noError().setNextValue(true);

            AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, allocatedThermometer, passing, allocatedRelays, overseer)
                    .next(
                            new TestCase()
                                    .input(passingOnOff, false)
                                    .input(passingMinTemp, 400)
                                    .input(passingNoError, true)
                                    .input(relaysOnOff, false)
                                    .input(relaysIsCloser, true)
                                    .input(thermometer, 100)
                                    .output(passingOnOff, true)
                    );

            //TODO controllerTest.run()

        } catch (Exception e) {
            fail();
        }

        assertTrue(true);
    }

    @Test
    public void testHeatingReachedNoError() {

        try {
            overseer.activate(null, config);
            overseer.activate(null, config);
            overseer.passing.getMinTemperature().setNextValue(config.minTemperature);
            overseer.passing.noError().setNextValue(true);

            AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, allocatedThermometer, passing, allocatedRelays, overseer)
                    .next(
                            new TestCase()
                                    .input(passingOnOff, true)
                                    .input(passingMinTemp, 320)
                                    .input(passingNoError, true)
                                    .input(relaysOnOff, false)
                                    .input(relaysIsCloser, true)
                                    .input(thermometer, 400)
                                    .output(passingOnOff, true)
                    );
            //TODO controllerTest.run()
            overseer.deactivate();

        } catch (Exception e) {
            fail();
        }

        assertTrue(true);
    }

    @Test(expected = OpenemsException.class)
    public void testHeatingError() throws Exception {
        overseer.activate(null, config);
        overseer.activate(null, config);
        overseer.passing.getMinTemperature().setNextValue(config.minTemperature);
        overseer.passing.noError().setNextValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, allocatedThermometer, passing, allocatedRelays, overseer)
                .next(
                        new TestCase()
                                .input(passingOnOff, false)
                                .input(passingMinTemp, 400)
                                .input(passingNoError, false)
                                .input(relaysOnOff, false)
                                .input(relaysIsCloser, true)
                                .input(thermometer, 100)

                );
        //TODO controllerTest.run()
    }

    @Test(expected = RuntimeException.class)
    public void testHeatingPassingNull() throws Exception {
        overseer.activate(null, config);
        overseer.activate(null, config);
        overseer.passing.getMinTemperature().setNextValue(config.minTemperature);
        overseer.passing.noError().setNextValue(true);
        overseer.passing = null;

        AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, allocatedThermometer, passing, allocatedRelays, overseer)
                .next(
                        new TestCase()
                                .input(passingOnOff, false)
                                .input(passingMinTemp, 400)
                                .input(passingNoError, false)
                                .input(relaysOnOff, false)
                                .input(relaysIsCloser, true)
                                .input(thermometer, 100)

                );
        //TODO controllerTest.run()
    }


    @Test(expected = ConfigurationException.class)
    public void testHeatingConfigurationErrorPassing() throws Exception {
        config.allocated_Passing_Controller = "Relays20";
        overseer.activate(null, config);
        overseer.activate(null, config);
        overseer.passing.getMinTemperature().setNextValue(config.minTemperature);
        overseer.passing.noError().setNextValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, allocatedThermometer, passing, allocatedRelays, overseer)
                .next(
                        new TestCase()
                                .input(passingOnOff, false)
                                .input(passingMinTemp, 400)
                                .input(passingNoError, false)
                                .input(relaysOnOff, false)
                                .input(relaysIsCloser, true)
                                .input(thermometer, 100)

                );
        //TODO controllerTest.run()
    }

    @Test(expected = ConfigurationException.class)
    public void testHeatingConfigurationErrorThermo() throws Exception {
        config.allocatedTemperatureSensor = new String[]{"Relays20"};
        overseer.activate(null, config);
        overseer.activate(null, config);
        overseer.passing.getMinTemperature().setNextValue(config.minTemperature);
        overseer.passing.noError().setNextValue(true);

        AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, allocatedThermometer, passing, allocatedRelays, overseer)
                .next(
                        new TestCase()
                                .input(passingOnOff, false)
                                .input(passingMinTemp, 400)
                                .input(passingNoError, false)
                                .input(relaysOnOff, false)
                                .input(relaysIsCloser, true)
                                .input(thermometer, 100)

                );
        //TODO controllerTest.run()
    }

}
