package io.openems.edge.consolinno.leaflet.core.pwm.configurator;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Configurator sets the Frequency of the Consolinno Pwm Module.
 * This will affect the Devices connected to the PWM Module.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Pwm.Configurator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class PwmConfiguratorImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    @Reference
    protected ComponentManager cpm;

    public PwmConfiguratorImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    private int moduleNumber;
    //Frequency in Hz
    private static final int DEFAULT_PWM_FREQUENCY = 200;
    private static final int MIN_MODULE_NUMBER = 1;
    private static final int MAX_MODULE_NUMBER = 8;
    private LeafletCore lc;
    private String leafletId;
    private final Logger log = LoggerFactory.getLogger(PwmConfiguratorImpl.class);
    private int frequency;

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        try {
            this.lc = this.cpm.getComponent(config.leafletId());
            this.leafletId = config.leafletId();
            int frequency = config.frequency();
            this.moduleNumber = config.moduleNumber();
            this.frequency = frequency;
            if (this.moduleNumber >= MIN_MODULE_NUMBER && this.moduleNumber <= MAX_MODULE_NUMBER) {
                this.lc.setPwmConfiguration(this.moduleNumber, frequency);
            } else {
                throw new ConfigurationException("ModuleNumber out of Bounds. Please check the Config",
                        "The ModuleNumber must be between" + MIN_MODULE_NUMBER + " and " + MAX_MODULE_NUMBER);
            }
        } catch (Exception e) {
            this.log.error("The LeafletCore doesn't exist or the system is starting.");
            this.frequency = config.frequency();
            this.leafletId = config.leafletId();
            this.moduleNumber = config.moduleNumber();
        }
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        try {
            this.lc = this.cpm.getComponent(config.leafletId());
        } catch (Exception e) {
            this.log.error("The LeafletCore doesn't exist! Check Config!");
        }
        this.lc.setPwmConfiguration(this.moduleNumber, DEFAULT_PWM_FREQUENCY);
        int frequency = config.frequency();
        this.moduleNumber = config.moduleNumber();
        if (this.moduleNumber >= MIN_MODULE_NUMBER && this.moduleNumber <= MAX_MODULE_NUMBER) {
            this.lc.setPwmConfiguration(this.moduleNumber, frequency);
        } else {
            throw new ConfigurationException("ModuleNumber out of Bounds. Please check the Config",
                    "The ModuleNumber must be between" + MIN_MODULE_NUMBER + " and " + MAX_MODULE_NUMBER);
        }
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    @Deactivate
    protected void deactivate() {
        this.lc.setPwmConfiguration(this.moduleNumber, DEFAULT_PWM_FREQUENCY);
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.lc == null) {
            try {
                this.lc = this.cpm.getComponent(this.leafletId);
                if (this.moduleNumber >= MIN_MODULE_NUMBER && this.moduleNumber <= MAX_MODULE_NUMBER) {
                    this.lc.setPwmConfiguration(this.moduleNumber, this.frequency);
                } else {
                    throw new ConfigurationException("ModuleNumber out of Bounds. Please check the Config",
                            "The ModuleNumber must be between" + MIN_MODULE_NUMBER + " and " + MAX_MODULE_NUMBER);
                }
            } catch (Exception e) {
                this.log.error("The LeafletCore doesn't exist or the system is starting.");
            }
        }
    }
}
