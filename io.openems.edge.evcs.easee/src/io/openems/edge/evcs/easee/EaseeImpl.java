package io.openems.edge.evcs.easee;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.timer.TimerByTime;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.easee.api.Easee;
import io.openems.edge.evcs.easee.bridge.EaseeBridge;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;

/**
 * This Provides the Easee EVCS over the Easee Cloud implementation via AMQP.
 * This Component will communicate with the EVCS to provide information about the current state, and provide instructions and commands for the charging process.
 * The Easee Interface contains the raw information from the EVCS that will then be translated in the WriteHandler into the Evcs/ManagedEvcs Interface, so OpenEms can understand it.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Easee", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class EaseeImpl extends AbstractOpenemsComponent implements OpenemsComponent, ManagedEvcs, Easee, Evcs, EventHandler {

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    @Reference
    TimerByTime timerByTime;

    private final Logger log = LoggerFactory.getLogger(EaseeImpl.class);
    private int minPower;
    private int maxPower;
    private int[] phases;
    private EaseeWriteHandler writeHandler;
    private EaseeReadHandler readHandler;
    private EaseeBridge easeeBridge;
    private boolean everConnected;
    private EvcsPower evcsPower;
    private static final int EASEE_MINIMUM_HARDWARE_POWER = 6 * GridVoltage.V_230_HZ_50.getValue();
    private static final int EASEE_MAXIMUM_HARDWARE_POWER = 32 * GridVoltage.V_230_HZ_50.getValue();


    public EaseeImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                Easee.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        this.minPower = config.minCurrent();
        this.maxPower = config.maxCurrent();
        this.phases = config.phases();
        if (!this.checkPhases()) {
            throw new ConfigurationException("Phase Configuration is not valid!", "Configuration must only contain 1,2 and 3.");
        }
        super.activate(context, config.id(), config.alias(), config.enabled());
        this._setMinimumHardwarePower(EASEE_MINIMUM_HARDWARE_POWER);
        this._setMaximumPower(this.maxPower);
        this._setMaximumHardwarePower(EASEE_MAXIMUM_HARDWARE_POWER);
        this._setMinimumPower(this.minPower);
        this._setPowerPrecision(GridVoltage.V_230_HZ_50.getValue());
        this._setIsPriority(config.priority());
        this._setChargePower(0);
        try {
            this.setMaximumChargeCurrent(0);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Unable to initialize MaximumChargeCurrent. This should not have happened.");
        }
        this.readHandler = new EaseeReadHandler(this);
        this.writeHandler = new EaseeWriteHandler(this);
        this.easeeBridge = new EaseeBridge(this,config.chargerSerial(), config.username(), config.password(),timerByTime);
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Checks if the Phase Configuration of the Config is valid.
     *
     * @return true if valid
     */
    private boolean checkPhases() {
        String phases = Arrays.toString(this.phases);
        return phases.contains("1") && phases.contains("2") && phases.contains("3") && this.phases.length == 3;
    }

    @Override
    public int[] getPhaseConfiguration() {
        return this.phases;
    }

    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }

    @Override
    public void handleEvent(Event event) {
        this.writeHandler.run();
        boolean connected = this.easeeBridge.run();
        if (!connected && !this.everConnected) {
            this.log.info("Could not establish connection to easee Cloud! Check credentials and connection!");
        } else if (!connected) {
            this.log.info("Connection lost to easee Cloud!");
        } else if (!this.everConnected) {
            this.log.info("Connection successfully established to easee Cloud.");
            this.everConnected = true;
        }
        try {
            this.readHandler.run();
        } catch (Throwable throwable) {
            this.log.error("Read Handler was unable to run!");
        }
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getChargePower().get() + " W | L1 " + this.getCurrentL1() + " A | L2 " + this.getCurrentL2() + " A | L3 " + this.getCurrentL3() + " A";
    }

    /**
     * Returns the minimum Software Power.
     *
     * @return minPower
     */
    public int getMinPower() {
        return this.minPower;
    }

    /**
     * Returns the maximum Software Power.
     *
     * @return maxPower
     */
    public int getMaxPower() {
        return this.maxPower;
    }

    /**
     * Returns the sum of the current on all phases.
     *
     * @return currentSum
     */
    public float getCurrentSum() {
        return this.getCurrentL1() + this.getCurrentL2() + this.getCurrentL3();
    }


    /**
     * Updates the Apache Felix Config with the current Access- and RefreshToken, in case of a restart or for information.
     *
     * @param accessToken  Current AccessToken retrieved from the Cloud
     * @param refreshToken Current RefreshToken retrieved from the Cloud
     */
    public void updateConfig(String accessToken, String refreshToken) {
        try {
            Configuration config = this.cm.getConfiguration(this.servicePid(), "?");

            Dictionary<String, Object> properties = config.getProperties();
            properties.put("accessToken", accessToken);
            properties.put("refreshToken", refreshToken);
            config.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Getter for the ID of the Component.
     * (Internal Method, only for EaseeBridge)
     *
     * @return ID as String.
     */
    public String getSuperId() {
        return super.id();
    }

    /**
     * Getter of the ComponentManager.
     * (Internal Method, only for EaseeBridge)
     *
     * @return ComponentManager
     */
    public ComponentManager getCpm() {
        return this.cpm;
    }

    /**
     * Simple Method for Outputting a Reconnect Message in the debug console.
     *
     * @param counter Number of reconnect Attempts
     */
    public void reconnectSuccessful(int counter) {
        this.log.info("Reconnect Successful after " + counter + " attempts");
    }

    /**
     * Deletes the Username and Password from the Apache Felix config.
     */
    public void deleteCredentials() {
        try {
            Configuration config = this.cm.getConfiguration(this.servicePid(), "?");

            Dictionary<String, Object> properties = config.getProperties();
            properties.put("username", "");
            properties.put("password", "");
            config.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
