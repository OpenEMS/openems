package io.openems.edge.meter.api;

import static io.openems.edge.common.type.Phase.SinglePhase.L1;
import static io.openems.edge.common.type.Phase.SinglePhase.L2;
import static io.openems.edge.common.type.Phase.SinglePhase.L3;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;

import io.openems.edge.common.type.Phase;
import io.openems.edge.common.type.Phase.SinglePhase;

public enum PhaseRotation {
	/**
	 * Device uses standard wiring.
	 *
	 * <ul>
	 * <li>Device L1 is connected to Source L1
	 * <li>Device L2 is connected to Source L2
	 * <li>Device L3 is connected to Source L3
	 * </ul>
	 */
	L1_L2_L3(ImmutableMap.<SinglePhase, SinglePhase>builder()//
			.put(L1, L1) //
			.put(L2, L2) //
			.put(L3, L3) //
			.build()), //
	/**
	 * Device uses rotated wiring.
	 *
	 * <ul>
	 * <li>Device L1 is connected to Source L2
	 * <li>Device L2 is connected to Source L3
	 * <li>Device L3 is connected to Source L1
	 * </ul>
	 */
	L2_L3_L1(ImmutableMap.<SinglePhase, SinglePhase>builder()//
			.put(L1, L2) //
			.put(L2, L3) //
			.put(L3, L1) //
			.build()), //
	/**
	 * Device uses rotated wiring.
	 *
	 * <ul>
	 * <li>Device L1 is connected to Source L3
	 * <li>Device L2 is connected to Source L1
	 * <li>Device L3 is connected to Source L2
	 * </ul>
	 */
	L3_L1_L2(ImmutableMap.<SinglePhase, SinglePhase>builder()//
			.put(L1, L3) //
			.put(L2, L1) //
			.put(L3, L2) //
			.build());

	private final ImmutableMap<SinglePhase, SinglePhase> rotation;

	private PhaseRotation(ImmutableMap<SinglePhase, SinglePhase> rotation) {
		this.rotation = rotation;
	}

	/**
	 * Gets the rotated phase.
	 *
	 * @param phase the input phase.
	 * @return the rotated phase.
	 */
	public SinglePhase get(SinglePhase phase) {
		return this.rotation.get(phase);
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for CURRENT_L1 after applying the
	 * {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for CURRENT_L1
	 */
	public final ElectricityMeter.ChannelId channelCurrentL1() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.CURRENT_L1;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.CURRENT_L2;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.CURRENT_L3;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for CURRENT_L2 after applying the
	 * {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for CURRENT_L2
	 */
	public final ElectricityMeter.ChannelId channelCurrentL2() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.CURRENT_L2;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.CURRENT_L3;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.CURRENT_L1;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for CURRENT_L3 after applying the
	 * {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for CURRENT_L3
	 */
	public final ElectricityMeter.ChannelId channelCurrentL3() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.CURRENT_L3;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.CURRENT_L1;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.CURRENT_L2;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for VOLTAGE_L1 after applying the
	 * {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for VOLTAGE_L1
	 */
	public final ElectricityMeter.ChannelId channelVoltageL1() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.VOLTAGE_L1;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.VOLTAGE_L2;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.VOLTAGE_L3;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for VOLTAGE_L2 after applying the
	 * {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for VOLTAGE_L2
	 */
	public final ElectricityMeter.ChannelId channelVoltageL2() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.VOLTAGE_L2;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.VOLTAGE_L3;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.VOLTAGE_L1;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for VOLTAGE_L3 after applying the
	 * {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for VOLTAGE_L3
	 */
	public final ElectricityMeter.ChannelId channelVoltageL3() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.VOLTAGE_L3;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.VOLTAGE_L1;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.VOLTAGE_L2;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for ACTIVE_POWER_L1 after
	 * applying the {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for ACTIVE_POWER_L1
	 */
	public final ElectricityMeter.ChannelId channelActivePowerL1() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for ACTIVE_POWER_L2 after
	 * applying the {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for ACTIVE_POWER_L2
	 */
	public final ElectricityMeter.ChannelId channelActivePowerL2() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
		};
	}

	/**
	 * Gets the {@link ElectricityMeter.ChannelId} for ACTIVE_POWER_L3 after
	 * applying the {@link PhaseRotation}.
	 *
	 * @return the mapped ChannelId for ACTIVE_POWER_L3
	 */
	public final ElectricityMeter.ChannelId channelActivePowerL3() {
		return switch (this) {
		case L1_L2_L3 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
		case L2_L3_L1 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
		case L3_L1_L2 -> ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
		};
	}

	/**
	 * Sets one of {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} or
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channels based on the
	 * {@link PhaseRotation} of the Electricity meter.
	 *
	 * @param meter the {@link ElectricityMeter}
	 * @param phase the phase of the Electricity meter, where the voltage was
	 *              measured
	 * @param value the voltage value
	 */
	public static void setPhaseRotatedVoltageChannel(ElectricityMeter meter, Phase.SinglePhase phase, Integer value) {
		switch (meter.getPhaseRotation().get(phase)) {
		case L1 -> meter._setVoltageL1(value);
		case L2 -> meter._setVoltageL2(value);
		case L3 -> meter._setVoltageL3(value);
		}
	}

	/**
	 * Sets {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} and
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channels based on the
	 * {@link PhaseRotation} of the Electricity meter.
	 *
	 * @param meter     the {@link ElectricityMeter}
	 * @param voltageL1 the value for L1
	 * @param voltageL2 the value for L2
	 * @param voltageL3 the value for L3
	 */
	public static void setPhaseRotatedVoltageChannels(ElectricityMeter meter, Integer voltageL1, Integer voltageL2,
			Integer voltageL3) {
		setPhaseRotatedVoltageChannel(meter, L1, voltageL1);
		setPhaseRotatedVoltageChannel(meter, L2, voltageL2);
		setPhaseRotatedVoltageChannel(meter, L3, voltageL3);
	}

	/**
	 * Sets one of {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} or
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channels based on the
	 * {@link PhaseRotation} of the Electricity meter.
	 *
	 * @param meter the {@link ElectricityMeter}
	 * @param phase the phase to set the value for
	 * @param value the current value
	 */
	public static void setPhaseRotatedCurrentChannel(ElectricityMeter meter, Phase.SinglePhase phase, Integer value) {
		switch (meter.getPhaseRotation().get(phase)) {
		case L1 -> meter._setCurrentL1(value);
		case L2 -> meter._setCurrentL2(value);
		case L3 -> meter._setCurrentL3(value);
		}
	}

	/**
	 * Sets {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} and
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channels based on the
	 * {@link PhaseRotation} of the Electricity meter.
	 *
	 * @param meter     the {@link ElectricityMeter}
	 * @param currentL1 the value for L1
	 * @param currentL2 the value for L2
	 * @param currentL3 the value for L3
	 */
	public static void setPhaseRotatedCurrentChannels(ElectricityMeter meter, Integer currentL1, Integer currentL2,
			Integer currentL3) {
		setPhaseRotatedCurrentChannel(meter, L1, currentL1);
		setPhaseRotatedCurrentChannel(meter, L2, currentL2);
		setPhaseRotatedCurrentChannel(meter, L3, currentL3);
	}

	/**
	 * Sets one of {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} or
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channels based on the
	 * {@link PhaseRotation} of the Electricity meter.
	 *
	 * @param meter the {@link ElectricityMeter}
	 * @param phase the phase of the Electricity meter, where the active power was
	 *              measured
	 * @param value the voltage value
	 */
	public static void setPhaseRotatedActivePowerChannel(ElectricityMeter meter, Phase.SinglePhase phase,
			Integer value) {
		switch (meter.getPhaseRotation().get(phase)) {
		case L1 -> meter._setActivePowerL1(value);
		case L2 -> meter._setActivePowerL2(value);
		case L3 -> meter._setActivePowerL3(value);
		}
	}

	/**
	 * Sets the {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} and
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channels based on the
	 * {@link PhaseRotation} of the Electricity meter.
	 *
	 * @param meter         the {@link ElectricityMeter}
	 * @param activePowerL1 the value for L1
	 * @param activePowerL2 the value for L2
	 * @param activePowerL3 the value for L3
	 */
	public static void setPhaseRotatedActivePowerChannels(ElectricityMeter meter, Integer activePowerL1,
			Integer activePowerL2, Integer activePowerL3) {
		setPhaseRotatedActivePowerChannel(meter, L1, activePowerL1);
		setPhaseRotatedActivePowerChannel(meter, L2, activePowerL2);
		setPhaseRotatedActivePowerChannel(meter, L3, activePowerL3);
	}

	/**
	 * Maps a read value of type {@link Long} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} or
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channel.
	 *
	 * @param meter the {@link ElectricityMeter}
	 * @param phase the phase of the Electricity meter, where the active power was
	 *              measured
	 * @return a Long consumer.
	 */
	public static Consumer<Long> mapLongToPhaseRotatedActivePowerChannel(ElectricityMeter meter,
			Phase.SinglePhase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedActivePowerChannel(meter, phase, intValue);
		};
	}
}
