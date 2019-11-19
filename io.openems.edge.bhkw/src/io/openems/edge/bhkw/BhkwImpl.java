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
    //bhkwType only for purposes coming in future
    private BhkwType bhkwType;
    @Reference
    protected ComponentManager cpm;

    public BhkwImpl() {
        super(OpenemsComponent.ChannelId.values(), PowerLevel.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        switch (config.bhkwType()) {
            case "EM_6_15":
                this.bhkwType = BhkwType.Vito_EM_6_15;
                break;
            case "EM_9_20":
                this.bhkwType = BhkwType.Vito_EM_9_20;
                break;
            case "EM_20_39":
                this.bhkwType = BhkwType.Vito_EM_20_39;
                break;
            case "EM_20_39_70":
                this.bhkwType = BhkwType.Vito_EM_20_39_RL_70;
                break;
            case "EM_50_81":
                this.bhkwType = BhkwType.Vito_EM_50_81;
                break;
            case "EM_70_115":
                this.bhkwType = BhkwType.Vito_EM_70_115;
                break;
            case "EM_100_167":
                this.bhkwType = BhkwType.Vito_Em_100_167;
                break;
            case "EM_140_207":
                this.bhkwType = BhkwType.Vito_EM_140_207;
                break;
            case "EM_199_263":
                this.bhkwType = BhkwType.Vito_EM_199_263;
                break;
            case "EM_199_293":
                this.bhkwType = BhkwType.Vito_EM_199_293;
                break;
            case "EM_238_363":
                this.bhkwType = BhkwType.Vito_EM_238_363;
                break;
            case "EM_363_498":
                this.bhkwType = BhkwType.Vito_EM_363_498;
                break;
            case "EM_401_549":
                this.bhkwType = BhkwType.Vito_EM_401_549;
                break;
            case "EM_530_660":
                this.bhkwType = BhkwType.Vito_EM_530_660;
                break;
            case "BM_36_66":
                this.bhkwType = BhkwType.Vito_BM_36_66;
                break;
            case "BM_55_88":
                this.bhkwType = BhkwType.Vito_BM_55_88;
                break;
            case "BM_190_238":
                this.bhkwType = BhkwType.Vito_BM_190_238;
                break;
            case "BM_366_437":
                this.bhkwType = BhkwType.Vito_BM_366_437;
                break;

            default:
                break;

        }

        if (cpm.getComponent(config.gaspedalId()) instanceof Gaspedal) {
            Gaspedal gaspedal = cpm.getComponent(config.gaspedalId());
            if (gaspedal.getId().equals(config.gaspedalId())) {
                //TODO Temporary till Controller is implemented
                int temp = 100;
                this.getPowerLevelChannel().setNextValue(temp);
                if (gaspedal.getMcp() instanceof Mcp4728) {
                    mcp = gaspedal.getMcp();
                    ((Mcp4728) mcp).addTask(super.id(), new BhkwTask(super.id(), config.position(), config.minLimit(), config.maxLimit(), config.percentageRange(), 4096.f, this.getPowerLevelChannel()));
                }
            }
        }

    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        if (this.mcp instanceof Mcp4728) {
            ((Mcp4728) this.mcp).removeTask(super.id());
        }
    }


    @Override
    public String debugLog() {
        if (this.getPowerLevelChannel().getNextValue().get() != null) {
            if (bhkwType != null) {
                return "Bhkw: " + this.bhkwType.getName() + "is at " + this.getPowerLevelChannel().getNextValue().get();
            } else {
                return "Bhkw is at " + this.getPowerLevelChannel().getNextValue().get();
            }
        }
        return "Percentage Level at 0";
    }

}
