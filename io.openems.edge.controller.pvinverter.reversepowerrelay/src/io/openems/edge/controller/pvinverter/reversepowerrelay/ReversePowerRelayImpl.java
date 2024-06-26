package io.openems.edge.controller.pvinverter.reversepowerrelay;


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
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.common.channel.BooleanReadChannel;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.PvInverter.ReversePowerRelay", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ReversePowerRelayImpl extends AbstractOpenemsComponent
		implements ReversePowerRelay, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ReversePowerRelayImpl.class);

	@Reference
	private ComponentManager componentManager;

	private String pvInverterId;

	private int powerLimit30Percent = 0;
	private int powerLimit60Percent = 0;

	private ChannelAddress inputChannelAddress0Percent = null;
	private ChannelAddress inputChannelAddress30Percent = null;
	private ChannelAddress inputChannelAddress60Percent = null;
	private ChannelAddress inputChannelAddress100Percent = null;

	private ManagedSymmetricPvInverter pvInverter;

	public ReversePowerRelayImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ReversePowerRelay.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.pvInverterId = config.pvInverter_id();

		this.powerLimit30Percent = config.powerLimit30();
		this.powerLimit60Percent = config.powerLimit60();

		try {
			this.inputChannelAddress0Percent = ChannelAddress.fromString(config.inputChannelAddress0Percent());
			this.inputChannelAddress30Percent = ChannelAddress.fromString(config.inputChannelAddress30Percent());

			this.inputChannelAddress60Percent = ChannelAddress.fromString(config.inputChannelAddress60Percent());
			this.inputChannelAddress100Percent = ChannelAddress.fromString(config.inputChannelAddress100Percent());

		} catch (OpenemsNamedException e) {
			this.log.error("Error parsing channel addresses", e);
		}
	}

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

	private void setPvLimit(Integer powerLimit) {

		try {
			this.pvInverter = this.componentManager.getComponent(this.pvInverterId);
			if (this.pvInverter != null) {
				this.pvInverter.setActivePowerLimit(powerLimit);
				if (powerLimit != null) {
					this.log.warn("Setting PV limit: " + powerLimit + "W for " + this.pvInverterId);
				} else {
					this.log.info("No limit for " + this.pvInverterId);
				}

			}
		} catch (OpenemsNamedException e) {
			this.log.error("Error setting PV limit", e);
		}

	}

	private Boolean getChannelValue(ChannelAddress address) throws OpenemsNamedException {
		if (address == null) {
			return null;
		}
		try {
			BooleanReadChannel channel = this.componentManager.getChannel(address);
			return channel.value().orElse(null);
		} catch (OpenemsNamedException e) {
			this.log.error("Error reading channel value", e);
			return null;
		}
	}

	@Override
	public void run() throws OpenemsNamedException {

		try {
			Boolean value0Percent = this.getChannelValue(this.inputChannelAddress0Percent);
			Boolean value30Percent = this.getChannelValue(this.inputChannelAddress30Percent);
			Boolean value60Percent = this.getChannelValue(this.inputChannelAddress60Percent);
			Boolean value100Percent = this.getChannelValue(this.inputChannelAddress100Percent);

			if (value0Percent == null || value30Percent == null || value60Percent == null || value100Percent == null) {
				this.log.warn("Skipping logic in run() due to null channel values");
				return;
			}
			//
			if (value0Percent == true) {
				this.setPvLimit(0);
			} else if (value0Percent == false && value30Percent == true && value60Percent == false
					&& value100Percent == false) {
				this.setPvLimit(this.powerLimit30Percent);
			} else if (value0Percent == false && value30Percent == false && value60Percent == true
					&& value100Percent == false) {
				this.setPvLimit(this.powerLimit60Percent);
			} else if (value0Percent == false && value30Percent == false && value60Percent == false
					&& value100Percent == true) {
				this.setPvLimit(null);
			} else {
				this.setPvLimit(0);
			}

		} catch (OpenemsNamedException e) {
			this.log.error("No values from modbus channels yet", e);
			return;
		}

	}
}
