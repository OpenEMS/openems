package io.openems.edge.relais;


import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.relaisBoard.RelaisBoardImpl;
import io.openems.edge.relaisboardmcp.Mcp;
import io.openems.edge.relaisboardmcp.Mcp23008;
import io.openems.edge.relaisboardmcp.McpChannelRegister;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;



@Designate(ocd = Config.class, factory = true)
@Component(name = "ConsolinnoRelais",
		configurationPolicy = ConfigurationPolicy.REQUIRE,
immediate = true)
public class RelaisActuator extends AbstractOpenemsComponent implements ActuatorRelais, OpenemsComponent {

	private Mcp allocatedMcp;
	private int position;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
    protected ComponentManager cpm;

	public RelaisActuator() {
		super(OpenemsComponent.ChannelId.values(),
				ActuatorRelais.ChannelId.values());
	}

	private boolean relaisValue = false;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
		super.activate(context,config.id(), config.alias(), config.enabled());
//			if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "I2Cregister", config.spiI2c_id())) {
//				return;
//			}
			allocateRelaisValue(config.relaisType());
			this.position = config.position();
			if (cpm.getComponent(config.relaisBoard_id()) instanceof RelaisBoardImpl) {
				RelaisBoardImpl relaisBoard = cpm.getComponent(config.relaisBoard_id());
				if (relaisBoard.getId().equals(config.relaisBoard_id())) {
					if (relaisBoard.getMcp() instanceof Mcp23008) {
					Mcp23008 mcp = (Mcp23008) relaisBoard.getMcp();
					allocatedMcp = mcp;
						mcp.setPosition(config.position(), this.relaisValue);
						mcp.addToDefault(config.position(), this.relaisValue);
							mcp.addTask(config.id(), new RelaisActuatorTask(mcp, config.position(),
									this.relaisValue, this.getRelaisChannel(),
									config.relaisBoard_id()));
					}
				}

			}
	}


	private void allocateRelaisValue(String relaisType) {
		switch (relaisType) {

			case "Closer":
			case "Reverse":
				this.relaisValue = false;
				break;
			default:
				this.relaisValue = true;
}
	}

	@Deactivate
	public void deactivate() {
		super.deactivate();
		if(allocatedMcp instanceof Mcp23008) {
			((Mcp23008) allocatedMcp).removeTask(this.id());
		}
	}

	@Override
	public String debugLog() {
		return "Status of " + super.id() + " alias: " + super.alias() + " will be " + this.getRelaisChannel().getNextWriteValue();
	}
}
