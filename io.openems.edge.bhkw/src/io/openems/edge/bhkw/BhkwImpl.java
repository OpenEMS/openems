package io.openems.edge.bhkw;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bhkw.task.BhkwTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.gaspedal.Gaspedal;
import io.openems.edge.relaisboardmcp.Mcp;
import io.openems.edge.relaisboardmcp.Mcp4728;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Bhkw")
public class BhkwImpl extends AbstractOpenemsComponent implements OpenemsComponent, PowerLevel {
    private Mcp mcp;
    @Reference
    protected ComponentManager cpm;

    public BhkwImpl() {
        super(OpenemsComponent.ChannelId.values(), PowerLevel.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        if (cpm.getComponent(config.gaspedalId()) instanceof Gaspedal) {
            Gaspedal gaspedal = cpm.getComponent(config.gaspedalId());
            if (gaspedal.getId().equals(config.gaspedalId())) {
                //TODO Temporary till BhkwTypes and Controllers are implemented
                this.getPowerLevelChannel().setNextValue(50);
                if (gaspedal.getMcp() instanceof Mcp4728) {
                    mcp = gaspedal.getMcp();
                    ((Mcp4728) mcp).addTask(super.id(), new BhkwTask(super.id(), config.position(), config.minLimit(), config.maxLimit(), config.percentageRange(), 4096, this.getPowerLevelChannel()));
                }
            }
        }

    }

    @Deactivate
    public void deactivate() {
        //TODO
        super.deactivate();
    }


    @Override
    public String debugLog() {
        if (this.getPowerLevelChannel().getNextValue().get() != null) {
            return "Percentage Level at " + this.getPowerLevelChannel().getNextValue().get();
        }
        return "Percentage Level at 0";
    }

}
