package io.openems.edge.heatsystem.components.pump;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heatsystem.components.ConfigurationType;
import io.openems.edge.heatsystem.components.HeatsystemComponent;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.pwm.api.Pwm;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This simple Pump can be configured and used by either a Pwm or a Relay or Both.
 * It works with Channels as well. You still need to configure if the Pump is controlled by a Relay, Pwm or Both.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatsystemComponent.Pump",
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)
public class PumpImpl extends AbstractOpenemsComponent implements OpenemsComponent, Pump, EventHandler {
    private final Logger log = LoggerFactory.getLogger(PumpImpl.class);

    private Relay relay;
    private Pwm pwm;

    private WriteChannel<?> relayChannel;
    private WriteChannel<?> pwmChannel;
    private boolean isRelay = false;
    private boolean isPwm = false;
    private ConfigurationType configurationType;

    @Reference
    ComponentManager cpm;

    public PumpImpl() {
        super(OpenemsComponent.ChannelId.values(), HeatsystemComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.allocateComponents(config.configType(), config.pump_Type(), config.pump_Relays(), config.pump_Pwm());
        this.getIsBusyChannel().setNextValue(false);
        this.getPowerLevelChannel().setNextValue(0);
        this.getLastPowerLevelChannel().setNextValue(0);
        if (config.disableOnActivation()) {
            this.deactivateDevices();
        }
    }

    /**
     * Deactivates the Devices. It sets the Relay to false and the Pwm to 0.
     */
    private void deactivateDevices() {
        if (this.isRelay) {
            this.controlRelay(false);
        }
        if (this.isPwm) {
            this.controlPwm(0);
        }
    }


    /**
     * Allocates the components.
     *
     * @param configurationType The Configuration Type -> either Channel or Device
     * @param pump_type         is the pump controlled via relay, pwm or both.
     * @param pump_relay        the unique id of the relays controlling the pump.
     * @param pump_pwm          unique id of the pwm controlling the pump.
     */
    private void allocateComponents(ConfigurationType configurationType, String pump_type, String pump_relay, String pump_pwm)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.configurationType = configurationType;
        switch (pump_type) {
            case "Relay":
                this.isRelay = true;
                break;
            case "Pwm":
                this.isPwm = true;
                break;

            case "Both":
            default:
                this.isRelay = true;
                this.isPwm = true;
                break;
        }

        if (this.isRelay) {
            this.configureRelay(pump_relay);
        }
        if (this.isPwm) {
            this.configurePwm(pump_pwm);
        }

    }

    /**
     * Configures the Pwm either by Device or Channel.
     *
     * @param pump_pwm the Pwm Id or ChannelAddress
     * @throws OpenemsError.OpenemsNamedException if either the Address or Device by the pump_pwm couldn't be found
     * @throws ConfigurationException             if either the Channel is not a WriteChannel or the Device is not a Pwm.
     */
    private void configurePwm(String pump_pwm) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (this.configurationType) {
            case CHANNEL:
                ChannelAddress pwmAddress = ChannelAddress.fromString(pump_pwm);
                Channel<?> pwmChannelByAddress = this.cpm.getChannel(pwmAddress);
                if (pwmChannelByAddress instanceof WriteChannel<?>) {
                    this.pwmChannel = (WriteChannel<?>) pwmChannelByAddress;
                } else {
                    throw new ConfigurationException("Configure Pump in : " + super.id(), "Channel is not a WriteChannel");
                }
                break;
            case DEVICE:
                OpenemsComponent pwmComponent = this.cpm.getComponent(pump_pwm);
                if (pwmComponent instanceof Pwm) {
                    this.pwm = (Pwm) pwmComponent;
                    //reset pwm to 0; so pump is on activation off
                    this.pwm.getWritePwmPowerLevelChannel().setNextWriteValue(0);
                } else {
                    throw new ConfigurationException(pump_pwm, "Allocated Pwm, not a (configured) pwm-device.");
                }
                break;
        }
    }

    /**
     * Configures the Relay either by Device or Channel (Boolean).
     *
     * @param pump_relay the Relay allocated to the pump.
     * @throws OpenemsError.OpenemsNamedException thrown if the Id of Device/Channel couldn't be found.
     * @throws ConfigurationException             if the id of the Device is not an instance of a relay.
     */

    private void configureRelay(String pump_relay) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (this.configurationType) {
            case CHANNEL:
                ChannelAddress relayAddress = ChannelAddress.fromString(pump_relay);
                Channel<?> relayChannelByAddress = this.cpm.getChannel(relayAddress);
                if (relayChannelByAddress instanceof WriteChannel<?>) {
                    this.relayChannel = (WriteChannel<?>) relayChannelByAddress;
                } else {
                    throw new ConfigurationException("Configure Relay in : " + super.id(), "Channel is not a WriteChannel");
                }
                break;
            case DEVICE:
                OpenemsComponent relayComponent = this.cpm.getComponent(pump_relay);
                if (relayComponent instanceof Relay) {
                    this.relay = (Relay) relayComponent;
                } else {
                    throw new ConfigurationException(pump_relay, "Allocated relay, not a (configured) relay-device.");
                }
                break;
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.isPwm = false;
        this.isRelay = false;
        this.allocateComponents(config.configType(), config.pump_Type(), config.pump_Relays(), config.pump_Pwm());
    }


    /**
     * Deactivates the pump.
     * if the relays is a closer --> false is written --> open
     * if the relays is an opener --> true is written --> open
     * --> no voltage.
     */
    @Deactivate
    public void deactivate() {
        super.deactivate();
        this.deactivateDevices();
    }

    /**
     * Called internally or by other Components. Tells the calling device if the HeatsystemComponent is ready to apply any Changes.
     *
     * @return a Boolean
     */

    @Override
    public boolean readyToChange() {
        return true;
    }

    /**
     * Changes the powervalue by percentage.
     * <p>
     * If the Pump is only a relays --> if negative --> controlyRelays false, else true
     * If it's in addition a pwm --> check if the powerlevel - percentage <= 0
     * --> pump is idle --> relays off and pwm is 0.f %
     * Otherwise it's calculating the new Power-level and writing
     * the old power-level in the LastPowerLevel Channel
     * </p>
     *
     * @param percentage to adjust the current powerLevel.
     * @return successful boolean
     */
    @Override
    public boolean changeByPercentage(double percentage) {

        if (this.isRelay) {
            if (this.isPwm) {
                double powerLevel = this.getPowerLevelValue();
                //deactivate
                if ((powerLevel + percentage <= 0)) {
                    if (this.controlRelay(false) && this.controlPwm(0)) {
                        this.getLastPowerLevelChannel().setNextValue(powerLevel);
                        this.getPowerLevelChannel().setNextValue(0);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    //activate relay, set Pwm Later
                    if (this.controlRelay(true) == false) {
                        return false;
                    }
                }
            } else {
                //set relay if only relay
                return this.controlRelay((percentage <= 0) == false);
            }
        }
        //sets pwm
        if (this.isPwm) {
            double currentPowerLevel;
            currentPowerLevel = this.getPowerLevelValue();
            currentPowerLevel += percentage;
            currentPowerLevel = currentPowerLevel > 100 ? 100
                    : currentPowerLevel < 0 ? 0 : currentPowerLevel;

            if (this.controlPwm(currentPowerLevel)) {
                this.getLastPowerLevelChannel().setNextValue(this.getPowerLevelValue());
                this.getPowerLevelChannel().setNextValue(currentPowerLevel);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the Relay(Channel) to either true or false, depending on the {@link #changeByPercentage(double percentage)}.
     *
     * @param activate if the relay should be active or not
     * @return true on success.
     */
    private boolean controlRelay(boolean activate) {

        switch (this.configurationType) {
            case CHANNEL:
                try {
                    this.relayChannel.setNextWriteValueFromObject(activate);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write into Channel; Pump: " + super.id() + "Channel : " + this.relayChannel.toString());
                    return false;
                }
                break;
            case DEVICE:
                try {
                    this.relay.getRelaysWriteChannel().setNextWriteValueFromObject(activate);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write into Channel; Pump: " + super.id() + "Device: " + this.relay.id());
                    return false;
                }
                break;
        }
        return true;
    }

    /**
     * Sets the Pwm Value Depending on the Unit of the Channel. Called by {@link #changeByPercentage(double)}
     *
     * @param percent the Percent set to this Pump
     * @return true on success
     */
    private boolean controlPwm(double percent) {
        int multiplier = 1;
        int maxValue = 100;
        Unit unit;
        if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
            unit = this.pwmChannel.channelDoc().getUnit();
        } else {
            unit = this.pwm.getWritePwmPowerLevelChannel().channelDoc().getUnit();
        }
        if (unit == Unit.THOUSANDTH) {
            multiplier = 10;
        }
        int percentToApply = (int) (percent * multiplier);
        percentToApply = percentToApply > maxValue * multiplier ? maxValue : Math.max(percentToApply, 0);

        switch (this.configurationType) {
            case CHANNEL:
                try {
                    this.pwmChannel.setNextWriteValueFromObject(percentToApply);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't apply PwmValue for Pump: " + super.id() + " Value: " + percentToApply);
                    return false;
                }
                break;
            case DEVICE:
                try {
                    this.pwm.getWritePwmPowerLevelChannel().setNextWriteValueFromObject(percentToApply);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't apply PwmValue for Pump: " + super.id() + " Value: " + percentToApply);
                    return false;
                }
                break;
        }
        return true;
    }

    /**
     * Sets the PowerLevel of the Pump. Values between 0-100% can be applied.
     *
     * @param percent the PowerLevel the Pump should be set to.
     */

    @Override
    public void setPowerLevel(double percent) {
        if (percent >= 0) {
            double changeByPercent = percent - getPowerLevelValue();
            this.changeByPercentage(changeByPercent);
        }
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            if (this.getResetValueAndResetChannel()) {
                this.setPowerLevel(0);
            } else if (this.getForceFullPowerAndResetChannel()) {
                this.setPowerLevel(100);
            } else if (this.setPointPowerLevelChannel().getNextValue().isDefined()) {
                this.setPowerLevel(this.setPointPowerLevelChannel().getNextValue().get());
            }
        }
    }
}
