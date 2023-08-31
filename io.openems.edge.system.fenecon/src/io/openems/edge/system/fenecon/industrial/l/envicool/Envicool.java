package io.openems.edge.system.fenecon.industrial.l.envicool;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.system.fenecon.industrial.l.Config;
import io.openems.edge.system.fenecon.industrial.l.SystemFeneconIndustrialL;

public class Envicool {

	private static final Logger LOG = LoggerFactory.getLogger(Envicool.class);

	private static final int MINIMUM_TEMP = 10;
	private static final int MAXIMUM_TEMP = 35;

	public enum Mode {
		ENABLED, DISABLED
	}

	public static record Context(Mode mode, int coolingSetPoint, int heatingSetPoint, List<Battery> batteries,
			EnumWriteChannel setBmsModeControlChannel, EnumWriteChannel setRunTimeControlModeChannel,
			IntegerWriteChannel setCoolingPointChannel, IntegerWriteChannel setHeatingPointChannel,
			IntegerWriteChannel setMonitorAndIssueMinTempChannel,
			IntegerWriteChannel setMonitorAndIssueMaxTempChannel) {

		/**
		 * Build a Envicool {@link Context} from a {@link SystemFeneconIndustrialL}
		 * {@link Config}.
		 * 
		 * @param config                           the {@link SystemFeneconIndustrialL}
		 *                                         {@link Config}
		 * @param batteries                        a list of {@link Battery}s
		 * @param setBmsModeControlChannel         the setBmsModeControlChannel
		 * @param setRunTimeControlModeChannel     the setRunTimeControlModeChannel
		 * @param setCoolingPointChannel           the setCoolingPointChannel
		 * @param setHeatingPointChannel           the setHeatingPointChannel
		 * @param setMonitorAndIssueMinTempChannel the setMonitorAndIssueMinTempChannel
		 * @param setMonitorAndIssueMaxTempChannel the setMonitorAndIssueMaxTempChannel
		 * @return a {@link Context}
		 */
		public static Context from(io.openems.edge.system.fenecon.industrial.l.Config config, List<Battery> batteries,
				EnumWriteChannel setBmsModeControlChannel, EnumWriteChannel setRunTimeControlModeChannel,
				IntegerWriteChannel setCoolingPointChannel, IntegerWriteChannel setHeatingPointChannel,
				IntegerWriteChannel setMonitorAndIssueMinTempChannel,
				IntegerWriteChannel setMonitorAndIssueMaxTempChannel) {
			int coolingSetPoint = config.acCoolingSetPoint();
			if (coolingSetPoint > MAXIMUM_TEMP || coolingSetPoint < MINIMUM_TEMP) {
				LOG.warn("The Cooling setpoint Temperature [" //
						+ config.acCoolingSetPoint() //
						+ "] should be between [" + MINIMUM_TEMP + " °C] and [" + MAXIMUM_TEMP + " °C]");
				coolingSetPoint = TypeUtils.fitWithin(MINIMUM_TEMP, MAXIMUM_TEMP, coolingSetPoint);
			}

			int heatingSetPoint = config.acHeatingSetPoint();
			if (heatingSetPoint > MAXIMUM_TEMP || heatingSetPoint < MINIMUM_TEMP) {
				LOG.warn("The Heating setpoint Temperature [" //
						+ config.acCoolingSetPoint() //
						+ "] should be between [" + MINIMUM_TEMP + " °C] and [" + MAXIMUM_TEMP + " °C]");
				heatingSetPoint = TypeUtils.fitWithin(MINIMUM_TEMP, MAXIMUM_TEMP, heatingSetPoint);
			}

			if (coolingSetPoint <= heatingSetPoint) {
				LOG.warn("The Cooling setpoint Temperature [" //
						+ coolingSetPoint //
						+ "] should be greater then, the Heating setpoint Temperature [" //
						+ heatingSetPoint);
				coolingSetPoint = MAXIMUM_TEMP;
				heatingSetPoint = MINIMUM_TEMP;
			}

			return new Context(config.acMode(), coolingSetPoint, heatingSetPoint, batteries, //
					setBmsModeControlChannel, setRunTimeControlModeChannel, //
					setCoolingPointChannel, setHeatingPointChannel, //
					setMonitorAndIssueMinTempChannel, setMonitorAndIssueMaxTempChannel);
		}
	}

	private Context context;

	/**
	 * Called on Component activate.
	 * 
	 * @param context the {@link Context}
	 */
	public void activate(Context context) {
		this.context = context;
	}

	/**
	 * Called on AFTER_PROCESS_IMAGE event.
	 */
	public void onAfterProcessImage() {
		try {
			switch (this.context.mode) {
			case DISABLED -> this.handleDisabled();
			case ENABLED -> this.handleEnabled();
			}
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Handles AC ENABLED mode.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void handleEnabled() throws OpenemsNamedException {
		setIfChanged(this.context.setCoolingPointChannel, this.context.coolingSetPoint);
		setIfChanged(this.context.setHeatingPointChannel, this.context.heatingSetPoint);

		// RuntimeControlMode.MONITOR_TEMP
		setIfChanged(this.context.setRunTimeControlModeChannel, RuntimeControlMode.MONITOR_TEMP);
		// BmsModeControl.FULLY_AUTOMATIC
		setIfChanged(this.context.setBmsModeControlChannel, BmsModeControl.FULLY_AUTOMATIC);

		var minBatTemp = this.getMinTemp().orElse(Short.MAX_VALUE /* temp not available */);
		setIfChanged(this.context.setMonitorAndIssueMinTempChannel, minBatTemp);

		var maxBatTemp = this.getMaxTemp().orElse(Short.MAX_VALUE /* temp not available */);
		setIfChanged(this.context.setMonitorAndIssueMaxTempChannel, maxBatTemp);
	}

	/**
	 * Handles AC DISABLED mode.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void handleDisabled() throws OpenemsNamedException {
		setIfChanged(this.context.setBmsModeControlChannel, BmsModeControl.STOP);
	}

	private static void setIfChanged(IntegerWriteChannel channel, int value) throws OpenemsNamedException {
		if (Objects.equals(channel.value().get(), value)) {
			return;
		}
		LOG.info("Setting Channel [" + channel.address() + "] to [" + value + "]");
		channel.setNextWriteValue(value);
	}

	private static void setIfChanged(EnumWriteChannel channel, OptionsEnum value) throws OpenemsNamedException {
		if (Objects.equals(channel.value().get(), value.getValue())) {
			return;
		}
		LOG.info("Setting Channel [" + channel.address() + "] to [" + value + "]");
		channel.setNextWriteValue(value);
	}

	/**
	 * Return the Max temperature for the list of battery inverters.
	 * 
	 * @return OptionalInt of max temperature.
	 */
	private OptionalInt getMaxTemp() {
		return this.context.batteries.stream()//
				.filter(b -> b.getMaxCellTemperature().isDefined())//
				.mapToInt(b -> b.getMaxCellTemperature().get())//
				.max();
	}

	/**
	 * Return the Max temperature for the list of battery inverters.
	 * 
	 * @return OptionalInt of max temperature.
	 */
	private OptionalInt getMinTemp() {
		return this.context.batteries.stream()//
				.filter(b -> b.getMinCellTemperature().isDefined())//
				.mapToInt(b -> b.getMinCellTemperature().get())//
				.min();
	}

}