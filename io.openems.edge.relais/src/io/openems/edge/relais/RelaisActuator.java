package io.openems.edge.relais;


import io.openems.edge.bridgei2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Actuator.Relais.I2cRegister")
public class RelaisActuator extends AbstractOpenemsComponent implements ActuatorRelais, OpenemsComponent {
	@Reference
	protected ConfigurationAdmin cm;

	protected RelaisActuator() {
		super(ActuatorRelais.ChannelId.values(),
				OpenemsComponent.ChannelId.values());
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected I2cBridge i2cBridge;

	@Activate
	void activate(ComponentContext context, Config config) {
			super.activate(context, config.service_pid(), config.id(), config.enabled());
			if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "I2Cregister", config.spiI2c_id())) {
				return;
			}
			//this.shiftregister.addTask(config.id(),new ShiftregisterTask(config.position(), !config.isOpener(),this.getRelaisChannel()));


	}

	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

}
