package io.openems.edge.evcs.api;

import static io.openems.edge.evcs.api.PhaseRotation.Phase.L1;
import static io.openems.edge.evcs.api.PhaseRotation.Phase.L2;
import static io.openems.edge.evcs.api.PhaseRotation.Phase.L3;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;

import io.openems.edge.meter.api.ElectricityMeter;

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
	 * Sets one of {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} or
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channels based on the
	 * {@link PhaseRotation} of the EVCS.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase of the EVCS, where the voltage was measured
	 * @param value the voltage value
	 */
	public static void setPhaseRotatedVoltageChannel(Evcs evcs, Phase phase, Integer value) {
		switch (evcs.getPhaseRotation().get(phase)) {
		case L1 -> evcs._setVoltageL1(value);
		case L2 -> evcs._setVoltageL2(value);
		case L3 -> evcs._setVoltageL3(value);
		}
	}

	/**
	 * Sets {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} and
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channels based on the
	 * {@link PhaseRotation} of the EVCS.
	 *
	 * @param evcs      the {@link Evcs}
	 * @param voltageL1 the value for L1
	 * @param voltageL2 the value for L2
	 * @param voltageL3 the value for L3
	 */
	public static void setPhaseRotatedVoltageChannels(Evcs evcs, Integer voltageL1, Integer voltageL2,
			Integer voltageL3) {
		setPhaseRotatedVoltageChannel(evcs, L1, voltageL1);
		setPhaseRotatedVoltageChannel(evcs, L2, voltageL2);
		setPhaseRotatedVoltageChannel(evcs, L3, voltageL3);
	}

	/**
	 * Sets one of {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} or
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channels based on the
	 * {@link PhaseRotation} of the EVCS.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase to set the value for
	 * @param value the current value
	 */
	public static void setPhaseRotatedCurrentChannel(Evcs evcs, Phase phase, Integer value) {
		switch (evcs.getPhaseRotation().get(phase)) {
		case L1 -> evcs._setCurrentL1(value);
		case L2 -> evcs._setCurrentL2(value);
		case L3 -> evcs._setCurrentL3(value);
		}
	}

	/**
	 * Sets {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} and
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channels based on the
	 * {@link PhaseRotation} of the EVCS.
	 *
	 * @param evcs      the {@link Evcs}
	 * @param currentL1 the value for L1
	 * @param currentL2 the value for L2
	 * @param currentL3 the value for L3
	 */
	public static void setPhaseRotatedCurrentChannels(Evcs evcs, Integer currentL1, Integer currentL2,
			Integer currentL3) {
		setPhaseRotatedCurrentChannel(evcs, L1, currentL1);
		setPhaseRotatedCurrentChannel(evcs, L2, currentL2);
		setPhaseRotatedCurrentChannel(evcs, L3, currentL3);
	}

	/**
	 * Sets one of {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} or
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channels based on the
	 * {@link PhaseRotation} of the EVCS.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase of the EVCS, where the active power was measured
	 * @param value the voltage value
	 */
	public static void setPhaseRotatedActivePowerChannel(Evcs evcs, Phase phase, Integer value) {
		switch (evcs.getPhaseRotation().get(phase)) {
		case L1 -> evcs._setActivePowerL1(value);
		case L2 -> evcs._setActivePowerL2(value);
		case L3 -> evcs._setActivePowerL3(value);
		}
	}

	/**
	 * Sets the {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} and
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channels based on the
	 * {@link PhaseRotation} of the EVCS.
	 *
	 * @param evcs          the {@link Evcs}
	 * @param activePowerL1 the value for L1
	 * @param activePowerL2 the value for L2
	 * @param activePowerL3 the value for L3
	 */
	public static void setPhaseRotatedActivePowerChannels(Evcs evcs, Integer activePowerL1, Integer activePowerL2,
			Integer activePowerL3) {
		setPhaseRotatedActivePowerChannel(evcs, L1, activePowerL1);
		setPhaseRotatedActivePowerChannel(evcs, L2, activePowerL2);
		setPhaseRotatedActivePowerChannel(evcs, L3, activePowerL3);
	}

	/**
	 * Maps a read value of type {@link Long} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} or
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channel.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase of the EVCS, where the voltage was measured
	 * @return a float consumer.
	 */
	public static Consumer<Long> mapLongToPhaseRotatedVoltageChannel(Evcs evcs, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedVoltageChannel(evcs, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Float} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L1},
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L2} or
	 * {@link ElectricityMeter.ChannelId#VOLTAGE_L3} channel.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase of the EVCS, where the voltage was measured
	 * @return a float consumer.
	 */
	public static Consumer<Float> mapFloatToPhaseRotatedVoltageChannel(Evcs evcs, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedVoltageChannel(evcs, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Float} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} or
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channel.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase of the EVCS, where the current was measured
	 * @return a float consumer.
	 */
	public static Consumer<Float> mapFloatToPhaseRotatedCurrentChannel(Evcs evcs, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedCurrentChannel(evcs, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Long} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#CURRENT_L1},
	 * {@link ElectricityMeter.ChannelId#CURRENT_L2} or
	 * {@link ElectricityMeter.ChannelId#CURRENT_L3} channel.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase of the EVCS, where the current was measured
	 * @return a float consumer.
	 */
	public static Consumer<Long> mapLongToPhaseRotatedCurrentChannel(Evcs evcs, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedCurrentChannel(evcs, phase, intValue);
		};
	}

	/**
	 * Maps a read value of type {@link Long} to the phase rotated
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1},
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2} or
	 * {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3} channel.
	 *
	 * @param evcs  the {@link Evcs}
	 * @param phase the phase of the EVCS, where the active power was measured
	 * @return a float consumer.
	 */
	public static Consumer<Long> mapLongToPhaseRotatedActivePowerChannel(Evcs evcs, Phase phase) {
		return value -> {
			var intValue = value != null ? Math.round(value) : null;
			setPhaseRotatedActivePowerChannel(evcs, phase, intValue);
		};
	}

	public static enum Phase {
		L1, //
		L2, //
		L3 //
	}
}
