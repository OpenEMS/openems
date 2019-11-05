package io.openems.edge.relais;


import io.openems.edge.bridgei2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.relaisBoard.RelaisBoardImpl;
import io.openems.edge.relaisboardmcp.Mcp;
import io.openems.edge.relaisboardmcp.Mcp23008;
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

	@Reference
	protected ConfigurationAdmin cm;

	protected RelaisActuator() {
		super(ActuatorRelais.ChannelId.values(),
				OpenemsComponent.ChannelId.values());
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected I2cBridge i2cBridge;

	private boolean relaisValue = false;

	@Activate
	void activate(ComponentContext context, Config config) {
			super.activate(context, config.service_pid(), config.id(), config.enabled());
//			if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "I2Cregister", config.spiI2c_id())) {
//				return;
//			}
			allocateRelaisValue(config.relaisType());

		setPositionOfMcp(config.relaisBoard_id());
		if (allocatedMcp != null) {
			if (allocatedMcp instanceof Mcp23008) {
				((Mcp23008) allocatedMcp).setPosition(config.position(), this.relaisValue);
				((Mcp23008) allocatedMcp).addToDefault(config.position(), this.relaisValue);
				this.i2cBridge.addTask(config.id(), new RelaisActuatorTask(allocatedMcp,config.position(),
										!relaisValue, this.isOnOrOff(),this.getRelaisChannel(),
												config.relaisBoard_id()));
			}
		}
	}

	private void setPositionOfMcp(String boardId) {
		for (Mcp existingMcp : i2cBridge.getMcpList()) {
				if (existingMcp instanceof Mcp23008) {
					if (((Mcp23008) existingMcp).getParentCircuitBoard().equals(boardId)) {
					this.allocatedMcp = existingMcp;
					break;
				}
				return;
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
		this.isOnOrOff().setNextValue(relaisValue);
		i2cBridge.removeTask(this.id());
	}

	@Override
	public String debugLog() {
		return "Status of " + super.id() + " alias: " + super.alias() + " is " + this.isOnOrOff().value().asString();
	}
}
