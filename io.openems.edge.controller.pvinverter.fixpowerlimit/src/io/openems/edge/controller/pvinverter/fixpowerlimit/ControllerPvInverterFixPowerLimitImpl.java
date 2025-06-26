package io.openems.edge.controller.pvinverter.fixpowerlimit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.PvInverter.FixPowerLimit", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerPvInverterFixPowerLimitImpl extends AbstractOpenemsComponent
		implements ControllerPvInverterFixPowerLimit, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerPvInverterFixPowerLimitImpl.class);

	@Reference
	private ComponentManager componentManager;

	private String pvInverterId;
	/** The configured Power Limit. */
	private int powerLimit = 0;

	public ControllerPvInverterFixPowerLimitImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerPvInverterFixPowerLimit.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.pvInverterId = config.pvInverter_id();
		this.powerLimit = config.powerLimit();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		// Reset limit
		ManagedSymmetricPvInverter pvInverter;
		try {
			pvInverter = this.componentManager.getComponent(this.pvInverterId);
			pvInverter.setActivePowerLimit(null);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
		}

		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricPvInverter pvInverter = this.componentManager.getComponent(this.pvInverterId);
		pvInverter.setActivePowerLimit(this.powerLimit);
	}

}
