package io.openems.edge.lucidcontrol.module;

import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.lucidcontrol.module.api.LucidControlModule;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Module.Lucid.Control")
public class LucidControlModuleImpl extends AbstractOpenemsComponent implements OpenemsComponent, LucidControlModule {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LucidControlBridge lucidControlBridge;

    private String path;
    private String voltage;

    public LucidControlModuleImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        lucidControlBridge.addPath(config.id(), config.path());
        lucidControlBridge.addVoltage(config.id(), config.voltage());
        this.path = config.path();
        this.voltage = config.voltage();
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        lucidControlBridge.removeModule(super.id());
    }


    @Override
    public String getVoltage() {
        return this.voltage;
    }

    @Override
    public String getId() {
        return super.id();
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
