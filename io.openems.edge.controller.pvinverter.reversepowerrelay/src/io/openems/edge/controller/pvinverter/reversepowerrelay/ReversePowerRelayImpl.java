package io.openems.edge.controller.pvinverter.reversepowerrelay;

import java.util.Optional;

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

	private Config config;

	private String pvInverterId;

	private int powerLimit30Percent = 0;
	private int powerLimit60Percent = 0;

	private ChannelAddress inputChannelAddress0Percent = null;
	private ChannelAddress inputChannelAddress30Percent = null;
	private ChannelAddress inputChannelAddress60Percent = null;
	private ChannelAddress inputChannelAddress100Percent = null;

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
		this.config = config;

		this.powerLimit30Percent = (int) Math.round(config.powerLimit100() * 0.3);
		this.powerLimit60Percent = (int) Math.round(config.powerLimit100() * 0.6);

		try {
			this.inputChannelAddress0Percent = ChannelAddress.fromString(config.inputChannelAddress0Percent());
			this.inputChannelAddress30Percent = ChannelAddress.fromString(config.inputChannelAddress30Percent());

			this.inputChannelAddress60Percent = ChannelAddress.fromString(config.inputChannelAddress60Percent());
			this.inputChannelAddress100Percent = ChannelAddress.fromString(config.inputChannelAddress100Percent());

		} catch (OpenemsNamedException e) {
			this.log.error("Error parsing channel addresses", e);
		}
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
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
			ManagedSymmetricPvInverter pvInverter = this.componentManager.getComponent(this.pvInverterId);

			if (pvInverter != null) {

				if (powerLimit != null) {
					this.log.warn("Setting PV limit: " + powerLimit + "W for " + this.pvInverterId);
				} else {
					this.log.info("No limit for " + this.pvInverterId);
				}

				pvInverter.setActivePowerLimit(powerLimit);

			}
		} catch (OpenemsNamedException e) {
			this.log.error("Error setting PV limit", e);
		}

	}

	private Optional<Boolean> getChannelValue(ChannelAddress address) {
		if (address == null) {
			return Optional.empty();
		}
		try {
			BooleanReadChannel channel = this.componentManager.getChannel(address);
			return channel.value().asOptional();
		} catch (OpenemsNamedException e) {
			this.log.error("Error reading channel value", e);
			return Optional.empty();
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		Optional<Boolean> value0PercentOpt = this.getChannelValue(this.inputChannelAddress0Percent);
		Optional<Boolean> value30PercentOpt = this.getChannelValue(this.inputChannelAddress30Percent);
		Optional<Boolean> value60PercentOpt = this.getChannelValue(this.inputChannelAddress60Percent);
		Optional<Boolean> value100PercentOpt = this.getChannelValue(this.inputChannelAddress100Percent);

		this.logDebug(this.log, "\nInput 0%->" + value0PercentOpt + "\nInput 30%->" + value30PercentOpt + "\nInput 60%->"
				+ value60PercentOpt + "\nInput 100%->" + value100PercentOpt);
		try {

			if (!value0PercentOpt.isPresent() || !value30PercentOpt.isPresent() || !value60PercentOpt.isPresent()
					|| !value100PercentOpt.isPresent()) {
				this.log.warn("Skipping logic in run() due to missing channel values");
				this.setPvLimit(0);
				return;
			}
			// Extract boolean values from Optional
			Boolean value0Percent = value0PercentOpt.get();
			Boolean value30Percent = value30PercentOpt.get();
			Boolean value60Percent = value60PercentOpt.get();
			Boolean value100Percent = value100PercentOpt.get();

			// make decisions based on modbus inputs
			if (value0Percent) {
				this.setPvLimit(0);
			} else if (value30Percent && !value60Percent && !value100Percent) {
				this.setPvLimit(this.powerLimit30Percent);
			} else if (value60Percent && !value30Percent && !value100Percent) {
				this.setPvLimit(this.powerLimit60Percent);
			} else if (value100Percent) {
				this.setPvLimit(null);
			} else {
				this.setPvLimit(0);
			}

		} catch (Exception e) {
			this.log.error("No values from modbus channels yet", e);
			return;
		}

	}

	public String getPvInverterId() {
		return this.pvInverterId;
	}

}
