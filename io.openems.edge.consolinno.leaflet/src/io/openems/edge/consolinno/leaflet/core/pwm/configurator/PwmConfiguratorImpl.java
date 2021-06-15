package io.openems.edge.consolinno.leaflet.core.pwm.configurator;

import io.openems.edge.common.component.AbstractOpenemsComponent;
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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This Configurator sets the Frequency of the Consolinno Pwm Module.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Pwm.Configurator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class PwmConfiguratorImpl extends AbstractOpenemsComponent implements OpenemsComponent {


    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LeafletCore lc;

    public PwmConfiguratorImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    private int moduleNumber;
    //Frequency in Hz
    private static final int DEFAULT_PWM_FREQUENCY = 200;
    private static final int MIN_MODULE_NUMBER = 1;
    private static final int MAX_MODULE_NUMBER = 8;

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        int frequency = config.frequency();
        this.moduleNumber = config.moduleNumber();
        if (this.moduleNumber >= MIN_MODULE_NUMBER && this.moduleNumber <= MAX_MODULE_NUMBER) {
            this.lc.setPwmConfiguration(this.moduleNumber, frequency);
        } else {
            throw new ConfigurationException("ModuleNumber out of Bounds. Please check the Config",
                    "The ModuleNumber must be between" + MIN_MODULE_NUMBER + " and " + MAX_MODULE_NUMBER);
        }
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
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

}
