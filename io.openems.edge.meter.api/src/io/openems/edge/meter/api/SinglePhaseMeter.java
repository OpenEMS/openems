package io.openems.edge.meter.api;

import java.util.function.Function;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;

@ProviderType
public interface SinglePhaseMeter extends ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Phase this Meter is connected to.
	 *
	 * @return the Phase
	 */
	public SinglePhase getPhase();

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#ACTIVE_POWER_L1},
	 * {@link ChannelId#ACTIVE_POWER_L2} or {@link ChannelId#ACTIVE_POWER_L3} from
	 * {@link ChannelId#ACTIVE_POWER} by evaluating the configured
	 * {@link SinglePhase} via {@link SinglePhaseMeter#getPhase()}.
	 *
	 * @param meter the {@link SinglePhaseMeter}
	 */
	public static void calculateSinglePhaseFromActivePower(SinglePhaseMeter meter) {
		SinglePhaseMeter.calculateSinglePhaseFromActivePower(meter, SinglePhaseMeter::getPhase);
	}

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Use this method if it is not known at compile time, that the
	 * {@link ElectricityMeter} is a {@link SinglePhaseMeter}, i.e. it is not
	 * implementing {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#ACTIVE_POWER_L1},
	 * {@link ChannelId#ACTIVE_POWER_L2} or {@link ChannelId#ACTIVE_POWER_L3} from
	 * {@link ChannelId#ACTIVE_POWER} by evaluating the provided
	 * {@link SinglePhase}.
	 *
	 * @param <METER>       type that extends {@link ElectricityMeter}
	 * @param meter         a {@link ElectricityMeter}
	 * @param phaseProvider a provider for {@link SinglePhase}
	 */
	public static <METER extends ElectricityMeter> void calculateSinglePhaseFromActivePower(METER meter,
			Function<METER, SinglePhase> phaseProvider) {
		meter.getActivePowerChannel().onSetNextValue(value -> {
			var phase = phaseProvider.apply(meter);
			meter.getActivePowerL1Channel().setNextValue(phase == SinglePhase.L1 ? value : null);
			meter.getActivePowerL2Channel().setNextValue(phase == SinglePhase.L2 ? value : null);
			meter.getActivePowerL3Channel().setNextValue(phase == SinglePhase.L3 ? value : null);
		});
	}

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#REACTIVE_POWER_L1},
	 * {@link ChannelId#REACTIVE_POWER_L2} or {@link ChannelId#REACTIVE_POWER_L3}
	 * from {@link ChannelId#REACTIVE_POWER} by evaluating the configured
	 * {@link SinglePhase} via {@link SinglePhaseMeter#getPhase()}.
	 *
	 * @param meter the {@link SinglePhaseMeter}
	 */
	public static void calculateSinglePhaseFromReactivePower(SinglePhaseMeter meter) {
		SinglePhaseMeter.calculateSinglePhaseFromReactivePower(meter, SinglePhaseMeter::getPhase);
	}

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Use this method if it is not known at compile time, that the
	 * {@link ElectricityMeter} is a {@link SinglePhaseMeter}, i.e. it is not
	 * implementing {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#REACTIVE_POWER_L1},
	 * {@link ChannelId#REACTIVE_POWER_L2} or {@link ChannelId#REACTIVE_POWER_L3}
	 * from {@link ChannelId#REACTIVE_POWER} by evaluating the provided
	 * {@link SinglePhase}.
	 *
	 * @param <METER>       type that extends {@link ElectricityMeter}
	 * @param meter         a {@link ElectricityMeter}
	 * @param phaseProvider a provider for {@link SinglePhase}
	 */
	public static <METER extends ElectricityMeter> void calculateSinglePhaseFromReactivePower(METER meter,
			Function<METER, SinglePhase> phaseProvider) {
		meter.getActivePowerChannel().onSetNextValue(value -> {
			var phase = phaseProvider.apply(meter);
			meter.getReactivePowerL1Channel().setNextValue(phase == SinglePhase.L1 ? value : null);
			meter.getReactivePowerL2Channel().setNextValue(phase == SinglePhase.L2 ? value : null);
			meter.getReactivePowerL3Channel().setNextValue(phase == SinglePhase.L3 ? value : null);
		});
	}

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#CURRENT_L1},
	 * {@link ChannelId#CURRENT_L2} or {@link ChannelId#CURRENT_L3} from
	 * {@link ChannelId#CURRENT} by evaluating the configured {@link SinglePhase}
	 * via {@link SinglePhaseMeter#getPhase()}.
	 *
	 * @param meter the {@link SinglePhaseMeter}
	 */
	public static void calculateSinglePhaseFromCurrent(SinglePhaseMeter meter) {
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(meter, SinglePhaseMeter::getPhase);
	}

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Use this method if it is not known at compile time, that the
	 * {@link ElectricityMeter} is a {@link SinglePhaseMeter}, i.e. it is not
	 * implementing {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#CURRENT_L1},
	 * {@link ChannelId#CURRENT_L2} or {@link ChannelId#CURRENT_L3} from
	 * {@link ChannelId#CURRENT} by evaluating the provided {@link SinglePhase}.
	 *
	 * @param <METER>       type that extends {@link ElectricityMeter}
	 * @param meter         a {@link ElectricityMeter}
	 * @param phaseProvider a provider for {@link SinglePhase}
	 */
	public static <METER extends ElectricityMeter> void calculateSinglePhaseFromCurrent(METER meter,
			Function<METER, SinglePhase> phaseProvider) {
		meter.getCurrentChannel().onSetNextValue(value -> {
			var phase = phaseProvider.apply(meter);
			meter.getCurrentL1Channel().setNextValue(phase == SinglePhase.L1 ? value : null);
			meter.getCurrentL2Channel().setNextValue(phase == SinglePhase.L2 ? value : null);
			meter.getCurrentL3Channel().setNextValue(phase == SinglePhase.L3 ? value : null);
		});
	}

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#VOLTAGE_L1},
	 * {@link ChannelId#VOLTAGE_L2} or {@link ChannelId#VOLTAGE_L3} from
	 * {@link ChannelId#VOLTAGE} by evaluating the configured {@link SinglePhase}
	 * via {@link SinglePhaseMeter#getPhase()}.
	 *
	 * @param meter the {@link SinglePhaseMeter}
	 */
	public static void calculateSinglePhaseFromVoltage(SinglePhaseMeter meter) {
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(meter, SinglePhaseMeter::getPhase);
	}

	/**
	 * Initializes Channel listeners for a {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Use this method if it is not known at compile time, that the
	 * {@link ElectricityMeter} is a {@link SinglePhaseMeter}, i.e. it is not
	 * implementing {@link SinglePhaseMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#VOLTAGE_L1},
	 * {@link ChannelId#VOLTAGE_L2} or {@link ChannelId#VOLTAGE_L3} from
	 * {@link ChannelId#VOLTAGE} by evaluating the provided {@link SinglePhase}.
	 *
	 * @param <METER>       type that extends {@link ElectricityMeter}
	 * @param meter         a {@link ElectricityMeter}
	 * @param phaseProvider a provider for {@link SinglePhase}
	 */
	public static <METER extends ElectricityMeter> void calculateSinglePhaseFromVoltage(METER meter,
			Function<METER, SinglePhase> phaseProvider) {
		meter.getVoltageChannel().onSetNextValue(value -> {
			var phase = phaseProvider.apply(meter);
			meter.getVoltageL1Channel().setNextValue(phase == SinglePhase.L1 ? value : null);
			meter.getVoltageL2Channel().setNextValue(phase == SinglePhase.L2 ? value : null);
			meter.getVoltageL3Channel().setNextValue(phase == SinglePhase.L3 ? value : null);
		});
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SinglePhaseMeter.class, accessMode, 100) //
				.build();
	}
}
