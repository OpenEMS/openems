package io.openems.edge.evse.api.chargepoint;

import static io.openems.edge.evse.api.chargepoint.PhaseRotation.Phase.L1;
import static io.openems.edge.evse.api.chargepoint.PhaseRotation.Phase.L2;
import static io.openems.edge.evse.api.chargepoint.PhaseRotation.Phase.L3;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;

import io.openems.edge.meter.api.ElectricityMeter;

// TODO copied from io.openems.edge.evcs.api.PhaseRotation
public enum PhaseRotation {
	/**
	 * EVCS uses standard wiring.
	 *
	 * <ul>
	 * <li>EVCS L1 is connected to Grid L1
	 * <li>EVCS L2 is connected to Grid L2
	 * <li>EVCS L3 is connected to Grid L3
	 * </ul>
	 */
	L1_L2_L3(ImmutableMap.<Phase, Phase>builder() //
			.put(L1, L1) //
			.put(L2, L2) //
			.put(L3, L3) //
			.build()), //
	/**
	 * EVCS uses rotated wiring.
	 *
	 * <ul>
	 * <li>EVCS L1 is connected to Grid L2
	 * <li>EVCS L2 is connected to Grid L3
	 * <li>EVCS L3 is connected to Grid L1
	 * </ul>
	 */
	L2_L3_L1(ImmutableMap.<Phase, Phase>builder() //
			.put(L1, L2) //
			.put(L2, L3) //
			.put(L3, L1) //
			.build()), //
	/**
	 * EVCS uses rotated wiring.
	 *
	 * <ul>
	 * <li>EVCS L1 is connected to Grid L3
	 * <li>EVCS L2 is connected to Grid L1
	 * <li>EVCS L3 is connected to Grid L2
	 * </ul>
	 */
	L3_L1_L2(ImmutableMap.<Phase, Phase>builder() //
			.put(L1, L3) //
			.put(L2, L1) //
			.put(L3, L2) //
			.build());

	private final ImmutableMap<Phase, Phase> rotation;

	private PhaseRotation(ImmutableMap<Phase, Phase> rotation) {
		this.rotation = rotation;
	}

	/**
	 * Gets the rotated phase.
	 *
	 * @param phase the input phase.
	 * @return the rotated phase.
	 */
	public Phase get(Phase phase) {
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
	 * Sets one of {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} or
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channels based on the
	 * {@link PhaseRotation} of the Charge-Point.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase of the Charge-Point, where the voltage was measured
	 * @param value the voltage value
	 */
	public static void setPhaseRotatedVoltageChannel(EvseChargePoint cp, Phase phase, Integer value) {
		switch (cp.getPhaseRotation().get(phase)) {
		case L1 -> cp._setVoltageL1(value);
		case L2 -> cp._setVoltageL2(value);
		case L3 -> cp._setVoltageL3(value);
		}
	}

	/**
	 * Sets {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} and
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channels based on the
	 * {@link PhaseRotation} of the Charge-Point.
	 *
	 * @param cp        the {@link EvseChargePoint}
	 * @param voltageL1 the value for L1
	 * @param voltageL2 the value for L2
	 * @param voltageL3 the value for L3
	 */
	public static void setPhaseRotatedVoltageChannels(EvseChargePoint cp, Integer voltageL1, Integer voltageL2,
			Integer voltageL3) {
		setPhaseRotatedVoltageChannel(cp, L1, voltageL1);
		setPhaseRotatedVoltageChannel(cp, L2, voltageL2);
		setPhaseRotatedVoltageChannel(cp, L3, voltageL3);
	}

	/**
	 * Sets one of {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} or
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channels based on the
	 * {@link PhaseRotation} of the Charge-Point.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase to set the value for
	 * @param value the current value
	 */
	public static void setPhaseRotatedCurrentChannel(EvseChargePoint cp, Phase phase, Integer value) {
		switch (cp.getPhaseRotation().get(phase)) {
		case L1 -> cp._setCurrentL1(value);
		case L2 -> cp._setCurrentL2(value);
		case L3 -> cp._setCurrentL3(value);
		}
	}

	/**
	 * Sets {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} and
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channels based on the
	 * {@link PhaseRotation} of the Charge-Point.
	 *
	 * @param cp        the {@link EvseChargePoint}
	 * @param currentL1 the value for L1
	 * @param currentL2 the value for L2
	 * @param currentL3 the value for L3
	 */
	public static void setPhaseRotatedCurrentChannels(EvseChargePoint cp, Integer currentL1, Integer currentL2,
			Integer currentL3) {
		setPhaseRotatedCurrentChannel(cp, L1, currentL1);
		setPhaseRotatedCurrentChannel(cp, L2, currentL2);
		setPhaseRotatedCurrentChannel(cp, L3, currentL3);
	}

	/**
	 * Sets one of {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} or
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channels based on the
	 * {@link PhaseRotation} of the Charge-Point.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase of the Charge-Point, where the active power was
	 *              measured
	 * @param value the voltage value
	 */
	public static void setPhaseRotatedActivePowerChannel(EvseChargePoint cp, Phase phase, Integer value) {
		switch (cp.getPhaseRotation().get(phase)) {
		case L1 -> cp._setActivePowerL1(value);
		case L2 -> cp._setActivePowerL2(value);
		case L3 -> cp._setActivePowerL3(value);
		}
	}

	/**
	 * Sets the {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} and
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channels based on the
	 * {@link PhaseRotation} of the Charge-Point.
	 *
	 * @param cp            the {@link EvseChargePoint}
	 * @param activePowerL1 the value for L1
	 * @param activePowerL2 the value for L2
	 * @param activePowerL3 the value for L3
	 */
	public static void setPhaseRotatedActivePowerChannels(EvseChargePoint cp, Integer activePowerL1,
			Integer activePowerL2, Integer activePowerL3) {
		setPhaseRotatedActivePowerChannel(cp, L1, activePowerL1);
		setPhaseRotatedActivePowerChannel(cp, L2, activePowerL2);
		setPhaseRotatedActivePowerChannel(cp, L3, activePowerL3);
	}

	/**
	 * Maps a read value of type {@link Long} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} or
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channel.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase of the Charge-Point, where the voltage was measured
	 * @return a float consumer.
	 */
	public static Consumer<Long> mapLongToPhaseRotatedVoltageChannel(EvseChargePoint cp, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedVoltageChannel(cp, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Float} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} or
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channel.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase of the Charge-Point, where the voltage was measured
	 * @return a float consumer.
	 */
	public static Consumer<Float> mapFloatToPhaseRotatedVoltageChannel(EvseChargePoint cp, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedVoltageChannel(cp, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Float} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} or
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channel.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase of the Charge-Point, where the current was measured
	 * @return a float consumer.
	 */
	public static Consumer<Float> mapFloatToPhaseRotatedCurrentChannel(EvseChargePoint cp, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedCurrentChannel(cp, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Long} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} or
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channel.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase of the Charge-Point, where the current was measured
	 * @return a float consumer.
	 */
	public static Consumer<Long> mapLongToPhaseRotatedCurrentChannel(EvseChargePoint cp, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedCurrentChannel(cp, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Long} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} or
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channel.
	 *
	 * @param cp    the {@link EvseChargePoint}
	 * @param phase the phase of the Charge-Point, where the active power was
	 *              measured
	 * @return a float consumer.
	 */
	public static Consumer<Long> mapLongToPhaseRotatedActivePowerChannel(EvseChargePoint cp, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedActivePowerChannel(cp, phase, intValue);
		};
	}

	public static enum Phase {
		L1, //
		L2, //
		L3 //
	}
}
