package io.openems.edge.consolinno.modbus.configurator.pwm.configurator;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.consolinno.modbus.configurator.api.LeafletConfigurator;
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

@Designate(ocd = io.openems.edge.consolinno.modbus.configurator.pwm.configurator.Config.class, factory = true)
@Component(name = "io.openems.edge.consolinno.modbus.configurator.pwm.configurator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class PwmConfiguratorImpl extends AbstractOpenemsComponent implements OpenemsComponent {


    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LeafletConfigurator lc;

    public PwmConfiguratorImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    private int moduleNumber;
    private static final int DEFAULT_PWM_FREQUENCY = 200;

    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException {
        int frequency = config.frequency();
        this.moduleNumber = config.moduleNumber();
        if (this.moduleNumber > 0 && this.moduleNumber < 9) {
            this.lc.setPwmConfiguration(this.moduleNumber, frequency);
        } else {
            throw new ConfigurationException("ModuleNumber out of Bounds. Please check the Config",
                    "The ModuleNumber must be between 1 and 8");
        }
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    public void modified(ComponentContext context, Config config) throws ConfigurationException {
        this.lc.setPwmConfiguration(this.moduleNumber, DEFAULT_PWM_FREQUENCY);
        int frequency = config.frequency();
        this.moduleNumber = config.moduleNumber();
        if (this.moduleNumber > 0 && this.moduleNumber < 9) {
            this.lc.setPwmConfiguration(this.moduleNumber, frequency);
        } else {
            throw new ConfigurationException("ModuleNumber out of Bounds. Please check the Config",
                    "The ModuleNumber must be between 1 and 8");
        }
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    @Deactivate
    public void deactivate() {
        this.lc.setPwmConfiguration(this.moduleNumber, DEFAULT_PWM_FREQUENCY);
        super.deactivate();
    }

}
