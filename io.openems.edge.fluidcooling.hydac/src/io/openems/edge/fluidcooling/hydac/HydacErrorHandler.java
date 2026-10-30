package io.openems.edge.fluidcooling.hydac;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.HIGH_PRESSURE_FAULT;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.HIGH_PRESSURE_WARNING;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.INVERTER_COMPRESSOR_FAULT;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.INVERTER_COMPRESSOR_WARNING;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.LOW_PRESSURE_FAULT;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.LOW_PRESSURE_WARNING;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.OVER_TEMPERATURE_FAULT_60_MIN;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.OVER_TEMPERATURE_WARNING_30_MIN;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.TEMPERATURE_RELATED_FAULT;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.UNDER_TEMPERATURE_WARNING_30_MIN;

import java.time.Clock;
import java.time.Duration;

public class HydacErrorHandler {

	private final Hydac hydac;
	private final Clock clock;

	private final ConditionTimeout overTemperatureWarningTimeout = new ConditionTimeout(Duration.ofMinutes(30));
	private final ConditionTimeout overTemperatureFaultTimeout = new ConditionTimeout(Duration.ofMinutes(60));
	private final ConditionTimeout underTemperatureWarningTimeout = new ConditionTimeout(Duration.ofMinutes(30));

	public HydacErrorHandler(Hydac hydac, Clock clock) {
		this.hydac = hydac;
		this.clock = clock;
	}

	/**
	 * Handles every warning/error related to the temperature, pressure and inverter
	 * compressor. This method should be called in every cycle.
	 */
	public void update() {
		this.handleTemperatureRelatedFault();
		this.handleInverterCompressor();
		this.handleLowPressure();
		this.handleHighPressure();
		this.handleOverTemperatureWarning();
		this.handleOverTemperatureFault();
		this.handleUnderTemperatureWarning();
	}

	private void handleTemperatureRelatedFault() {
		setValue(this.hydac, TEMPERATURE_RELATED_FAULT, this.hydac.getInverterCompressorFault().get()
				|| this.hydac.getLowPressureFault().get() || this.hydac.getHighPressureFault().get());
	}

	private void handleInverterCompressor() {
		var inverterErrorCompressor = this.hydac.getInverterErrorCompressor().orElse(false);
		var overTemperatureAlarm = this.hydac.getOverTemperatureAlarm().get();
		setValue(this.hydac, INVERTER_COMPRESSOR_WARNING, inverterErrorCompressor && !overTemperatureAlarm);
		setValue(this.hydac, INVERTER_COMPRESSOR_FAULT, inverterErrorCompressor && overTemperatureAlarm);
	}

	private void handleLowPressure() {
		var overTemperatureAlarm = this.hydac.getOverTemperatureAlarm().get();
		var alarm = this.hydac.getLowPressureAlarm().orElse(false);

		setValue(this.hydac, LOW_PRESSURE_WARNING, alarm && !overTemperatureAlarm);
		setValue(this.hydac, LOW_PRESSURE_FAULT, alarm && overTemperatureAlarm);
	}

	private void handleHighPressure() {
		var overTemperatureAlarm = this.hydac.getOverTemperatureAlarm().get();
		var highPressureAlarm = this.hydac.getHighPressureAlarm().orElse(false);

		setValue(this.hydac, HIGH_PRESSURE_WARNING, highPressureAlarm && !overTemperatureAlarm);
		setValue(this.hydac, HIGH_PRESSURE_FAULT, highPressureAlarm && overTemperatureAlarm);
	}

	private void handleOverTemperatureWarning() {
		var active = this.hydac.getOverTemperatureAlarm().get() && !this.hydac.getTemperatureRelatedFault().get();

		setValue(this.hydac, OVER_TEMPERATURE_WARNING_30_MIN,
				this.overTemperatureWarningTimeout.update(active, this.clock));
	}

	private void handleOverTemperatureFault() {
		var active = this.hydac.getOverTemperatureAlarm().get() && !this.hydac.getTemperatureRelatedFault().get();

		setValue(this.hydac, OVER_TEMPERATURE_FAULT_60_MIN,
				this.overTemperatureFaultTimeout.update(active, this.clock));
	}

	private void handleUnderTemperatureWarning() {
		var active = this.hydac.getUnderTemperatureAlarm().get();

		setValue(this.hydac, UNDER_TEMPERATURE_WARNING_30_MIN,
				this.underTemperatureWarningTimeout.update(active, this.clock));
	}
}
