package io.openems.edge.controller.io.analog;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.io.api.AnalogOutput;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.IO.Analog", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerIoAnalogImpl extends AbstractOpenemsComponent
		implements ControllerIoAnalog, Controller, OpenemsComponent, TimedataProvider {

	/*
	 * Debounce, to avoid calculations based on deprecated consumption values in
	 * automatic mode.
	 */
	private static final int DEFAULT_DEBOUNCE_SEC = 3;

	private final Logger log = LoggerFactory.getLogger(ControllerIoAnalogImpl.class);

	private final CalculateEnergyFromPower calculateCumulatedEnergy = new CalculateEnergyFromPower(this,
			ControllerIoAnalog.ChannelId.CUMULATED_ACTIVE_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private AnalogOutput analogOutput;

	private Instant nextTarget = Instant.MIN;
	private Clock clock;
	private int lastOutputPower;
	private Config config = null;

	public ControllerIoAnalogImpl() {
		this(Clock.systemDefaultZone());
	}

	public ControllerIoAnalogImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerIoAnalog.ChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "analogOutput",
				config.analogOutput_id())) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		switch (this.config.mode()) {
		case ON -> this.setOutput(this.config.manualTarget());
		case OFF -> this.setOutput(0);
		case AUTOMATIC -> this.automaticMode();
		}
	}

	private void automaticMode() throws OpenemsNamedException {

		// TODO: hysteresis

		if (this.nextTarget.isBefore(Instant.now(this.clock))) {

			var gridActivePower = this.sum.getGridActivePower().getOrError();

			// Surplus used by the ess
			var essDischargePower = this.sum.getEssDischargePower().orElse(0);
			essDischargePower = Math.max(essDischargePower, 0);

			int excessPower;
			if (gridActivePower > 0) {
				excessPower = 0;
			} else {

				var usedPower = calculateUsedPower(this.config.maximumPower(),
						this.analogOutput.getDebugSetOutputPercent().orElse(0f), this.config.powerBehaviour());
				excessPower = gridActivePower * -1 - essDischargePower + usedPower;
			}

			excessPower = TypeUtils.fitWithin(0, this.config.maximumPower(), excessPower);
			this.setOutput(excessPower);
			this.nextTarget = Instant.now(this.clock).plus(DEFAULT_DEBOUNCE_SEC, ChronoUnit.SECONDS);
		} else {
			// Set previous output
			this.setOutput(this.lastOutputPower);
		}
	}

	/**
	 * Calculate the current power depending on the current settings.
	 * 
	 * <p>
	 * Attention: Even if the "Power Behaviour" is defining the hardware behavior,
	 * the real consumption depends on the device itself and we have to assume that
	 * the unit behaves in a similar way. For the exact values, a separate meter
	 * would be needed.
	 * 
	 * @param maximumPower         maximum power of the device
	 * @param currentOutputPercent current output in %
	 * @param powerBehavior        the power behavior as {@link PowerBehavior}
	 * @return power used by the device
	 */
	protected static int calculateUsedPower(int maximumPower, float currentOutputPercent, PowerBehavior powerBehavior) {

		var factor = currentOutputPercent / (float) AnalogOutput.SET_OUTPUT_ACCURACY;
		return powerBehavior.calculatePowerFromFactor.apply(maximumPower, factor);
	}

	/**
	 * Calculate the set point depending on the current settings.
	 * 
	 * <p>
	 * Attention: Even if the "Power Behaviour" is defining the hardware behavior,
	 * the real consumption depends on the device itself and we have to assume that
	 * the unit behaves in a similar way and is using the calculated power.
	 * 
	 * @param maximumPower  maximum power of the device
	 * @param targetPower   target power
	 * @param powerBehavior power behavior
	 * @return current set point in %
	 */
	protected static float calculateSetPointFromPower(int maximumPower, int targetPower, PowerBehavior powerBehavior) {

		var factor = powerBehavior.calculateFactorFromPower.apply(maximumPower, targetPower);
		return factor * (float) AnalogOutput.SET_OUTPUT_ACCURACY;
	}

	/**
	 * Helper function to set the output & updates the cumulated active energy
	 * channel.
	 *
	 * @param power target power value
	 * @throws OpenemsNamedException on error
	 */
	private void setOutput(int power) throws OpenemsNamedException {

		// Update the cumulated Energy.
		this.calculateCumulatedEnergy.update(power);

		var outputPercent = calculateSetPointFromPower(this.config.maximumPower(), power, this.config.powerBehaviour());
		var currentValue = this.analogOutput.getDebugSetOutputPercent();
		if (!currentValue.isDefined() || currentValue.get() != outputPercent) {
			this.logInfo(this.log, "Set output [" + this.config.analogOutput_id() + "] to " + power + "W.");
			this.analogOutput.setOutputPercent(outputPercent);
			this.lastOutputPower = power;
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
