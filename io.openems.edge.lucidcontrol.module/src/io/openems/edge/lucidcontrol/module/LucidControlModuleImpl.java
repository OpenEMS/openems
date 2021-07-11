package io.openems.edge.lucidcontrol.module;

import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This Class implements a LucidControlModule, e.g. holds the Path and the Voltage for a concrete Device, attached to
 * the I/O of the Device.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Module.Lucid.Control")
public class LucidControlModuleImpl extends AbstractOpenemsComponent implements OpenemsComponent {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LucidControlBridge lucidControlBridge;

    public LucidControlModuleImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
        this.lucidControlBridge.removeModule(super.id());
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        boolean idChanged = super.id().equals(config.id()) == false;
        if (idChanged) {
            this.lucidControlBridge.removeModule(super.id());
        }
        super.modified(context, config.id(), config.alias(), config.enabled());
        if (idChanged) {
            this.activationOrModifiedRoutine(config);
        }
    }

    /**
     * This Method runs the basic setup needed by the activation or modified method.
     *
     * @param config the config of this class.
     */
    private void activationOrModifiedRoutine(Config config) {
        this.lucidControlBridge.addPath(config.id(), config.path());
        this.lucidControlBridge.addVoltage(config.id(), config.voltage());
    }
}
