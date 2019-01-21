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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.PvInverter.FixPowerLimit", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PvInverterFixPowerLimit extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PvInverterFixPowerLimit.class);

	@Reference
	protected ComponentManager componentManager;

	private String pvInverterId;

	/**
	 * the configured Power Limit
	 */
	private int powerLimit = 0;

	public PvInverterFixPowerLimit() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());

		this.pvInverterId = config.pvInverter_id();
		this.powerLimit = config.powerLimit();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		try {
			SymmetricPvInverter pvInverter = this.componentManager.getComponent(this.pvInverterId);
			pvInverter.getActivePowerLimit().setNextWriteValue(this.powerLimit);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to set ActivePowerLimit on Inverter: " + e.getMessage());
		}
	}

}
