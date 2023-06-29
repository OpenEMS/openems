package io.openems.edge.battery.bydcommercial;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bydcommercial.enums.BatteryWorkState;
import io.openems.edge.battery.bydcommercial.enums.PowerCircuitControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BydBatteryBoxCommercialC130 extends Battery, OpenemsComponent, StartStoppable {

	/**
	 * Gets the Channel for {@link ChannelId#POWER_CIRCUIT_CONTROL}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<PowerCircuitControl> getPowerCircuitControlChannel() {
		return this.channel(ChannelId.POWER_CIRCUIT_CONTROL);
	}

	/**
	 * Gets the PreChargeControl, see {@link ChannelId#POWER_CIRCUIT_CONTROL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default PowerCircuitControl getPowerCircuitControl() {
		return this.getPowerCircuitControlChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#POWER_CIRCUIT_CONTROL} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPowerCircuitControl(PowerCircuitControl value) {
		this.getPowerCircuitControlChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#POWER_CIRCUIT_CONTROL} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPowerCircuitControl(PowerCircuitControl value) throws OpenemsNamedException {
		this.getPowerCircuitControlChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttempts() {
		return this.getMaxStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStartAttempts(Boolean value) {
		this.getMaxStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttempts() {
		return this.getMaxStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_ATTEMPTS}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStopAttempts(Boolean value) {
		this.getMaxStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		CLUSTER_RUN_STATE(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_WORK_STATE(Doc.of(BatteryWorkState.values())), //

		// IntegerReadChannels
		POWER_CIRCUIT_CONTROL(Doc.of(PowerCircuitControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		CLUSTER_1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CLUSTER_1_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		CLUSTER_1_MAX_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.text("Range: 1 ~ 256")), //
		CLUSTER_1_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MIN_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.text("Range: 1 ~ 256")), //
		CLUSTER_1_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MAX_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.text("Range: 1 ~ 256")), //
		CLUSTER_1_MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.text("Range: -400 ~ 1500")), //
		CLUSTER_1_MIN_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.text("Range: 1 ~ 256")), //
		CLUSTER_1_MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.text("Range: -400 ~ 1500")), //
		MODULE_QTY(Doc.of(OpenemsType.INTEGER) //
				.text("Range: 1 ~ 256")), //
		TOTAL_VOLTAGE_OF_SINGLE_MODULE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOOHM)), //
		CLUSTER_1_BATTERY_000_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_001_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_002_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_003_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_004_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_005_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_006_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_007_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_008_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_009_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_010_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_011_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_012_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_013_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_014_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_015_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_016_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_017_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_018_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_019_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_020_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_021_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_022_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_023_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_024_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_025_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_026_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_027_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_028_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_029_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_030_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_031_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_032_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_033_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_034_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_035_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_036_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_037_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_038_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_039_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_040_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_041_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_042_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_043_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_044_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_045_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_046_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_047_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_048_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_049_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_050_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_051_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_052_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_053_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_054_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_055_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_056_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_057_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_058_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_059_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_060_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_061_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_062_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_063_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_064_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_065_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_066_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_067_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_068_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_069_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_070_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_071_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_072_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_073_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_074_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_075_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_076_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_077_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_078_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_079_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_080_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_081_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_082_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_083_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_084_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_085_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_086_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_087_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_088_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_089_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_090_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_091_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_092_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_093_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_094_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_095_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_096_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_097_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_098_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_099_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_100_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_101_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_102_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_103_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_104_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_105_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_106_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_107_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_108_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_109_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_110_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_111_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_112_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_113_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_114_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_115_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_116_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_117_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_118_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_119_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_120_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_121_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_122_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_123_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_124_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_125_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_126_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_127_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_128_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_129_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_130_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_131_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_132_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_133_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_134_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_135_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_136_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_137_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_138_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_139_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_140_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_141_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_142_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_143_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_144_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_145_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_146_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_147_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_148_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_149_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_150_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_151_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_152_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_153_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_154_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_155_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_156_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_157_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_158_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_159_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_160_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_161_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_162_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_163_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_164_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_165_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_166_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_167_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_168_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_169_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_170_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_171_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_172_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_173_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_174_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_175_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_176_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_177_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_178_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_179_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_180_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_181_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_182_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_183_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_184_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_185_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_186_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_187_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_188_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_189_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_190_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_191_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_192_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_193_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_194_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_195_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_196_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_197_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_198_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_199_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_200_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_201_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_202_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_203_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_204_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_205_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_206_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_207_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_208_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_209_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_210_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_211_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_212_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_213_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_214_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_215_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_216_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_217_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_218_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_219_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_220_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_221_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_222_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_223_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_224_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_225_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_226_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_227_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_228_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_229_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_230_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_231_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_232_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_233_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_234_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_235_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_236_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_237_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_238_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_239_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //

		CLUSTER_1_BATTERY_00_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_01_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_02_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_03_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_04_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_05_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_06_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_07_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_08_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_09_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_10_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_11_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_12_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_13_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_14_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_15_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_16_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_17_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_18_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_19_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_20_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_21_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_22_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_23_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_24_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_25_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_26_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_27_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_28_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_29_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_30_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_31_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_32_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_33_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_34_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_35_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_36_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_37_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_38_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_39_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_40_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_41_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_42_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_43_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_44_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_45_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_46_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_47_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		// StateChannels
		PRE_ALARM_CELL_VOLTAGE_HIGH(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell Voltage High Alarm Level 1")), //
		PRE_ALARM_CELL_VOLTAGE_LOW(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell Voltage Low Alarm Level 1")), //
		PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Alarm Level 1 Battery Cells Unbalanced")), //
		PRE_ALARM_DISCHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell Discharge Temperature High Alarm Level 1")), //
		PRE_ALARM_DISCHARGE_TEMP_LOW(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell Discharge Temperature Low Alarm Level 1")), //
		PRE_ALARM_CHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell Charge Temperature High Alarm Level 1")), //
		PRE_ALARM_CHARGE_TEMP_LOW(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell Charge Temperature Low Alarm Level 1")), //
		PRE_ALARM_TEMP_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell temperature Diff High Alarm Level 1")), //
		PRE_ALARM_POWER_POLE_HIGH(Doc.of(Level.INFO) //
				.text("Cluster 1 Cell Temperature High Alarm Level 1")), //
		PRE_ALARM_DISCHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
				.text("Cluster 1 Discharge Current High Alarm Level 1")), //
		PRE_ALARM_CHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
				.text("Cluster 1 Charge Current High Alarm Level 1")), //

		LEVEL1_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Voltage High Alarm Level 2")), //
		LEVEL1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Voltage Low Alarm Level 2")), //
		LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Alarm Level 2 Battery Cells Unbalanced")), //
		LEVEL1_DISCHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Discharge Temperature High Alarm Level 2")), //
		LEVEL1_DISCHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Discharge Temperature Low Alarm Level 2")), //
		LEVEL1_CHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Charge Temperature High Alarm Level 2")), //
		LEVEL1_CHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Charge Temperature Low Alarm Level 2")), //
		LEVEL1_TEMP_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Temperature Diff High Alarm Level 2")), //
		LEVEL1_POWER_POLE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 2 Cell Temperature High Alarm Level 2")), //
		LEVEL1_DISCHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 2 Discharge Current High Alarm Level 2")), //
		LEVEL1_CHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 2 Charge Current High Alarm Level 2")), //

		LEVEL2_CELL_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Voltage High Alarm Level 3")), //
		LEVEL2_CELL_VOLTAGE_LOW(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Voltage Low Alarm Level 3")), //
		LEVEL2_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.FAULT) //
				.text("Alarm Level 3 Battery Cells Unbalanced")), //
		LEVEL2_DISCHARGE_TEMP_HIGH(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Discharge Temperature High Alarm Level 3")), //
		LEVEL2_DISCHARGE_TEMP_LOW(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Discharge Temperature Low Alarm Level 3")), //
		LEVEL2_CHARGE_TEMP_HIGH(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Charge Temperature High Alarm Level 3")), //
		LEVEL2_CHARGE_TEMP_LOW(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Charge Temperature Low Alarm Level 3")), //
		LEVEL2_TEMP_DIFF_TOO_BIG(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Temperature Diff High Alarm Level 3")), //
		LEVEL2_POWER_POLE_HIGH(Doc.of(Level.FAULT) //
				.text("Cluster 3 Cell Temperature High Alarm Level 3")), //
		LEVEL2_DISCHARGE_CURRENT_HIGH(Doc.of(Level.FAULT) //
				.text("Cluster 3 Discharge Current High Alarm Level 3")), //
		LEVEL2_CHARGE_CURRENT_HIGH(Doc.of(Level.FAULT) //
				.text("Cluster 3 Charge Current High Alarm Level 3")), //

		ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster1 Total Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_INSULATION_LOW(Doc.of(Level.WARNING) //
				.text("Cluster1 Insulation Low Alarm Level1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_SOC_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 SOC Low Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage High Alarm Level 1")), //
		ALARM_FUSE(Doc.of(Level.FAULT) //
				.text(" Fuse Alarm")), //
		SHIELDED_SWITCH_STATE(Doc.of(Level.WARNING) //
				.text("Shielded switch state")), //
		ALARM_BAU_COMMUNICATION(Doc.of(Level.WARNING) //
				.text("BAU Communication Alarm")), //
		ALARM_INSULATION_CHECK(Doc.of(Level.FAULT) //
				.text("Inuslation Resistance Alarm")), //
		ALARM_CURRENT_SENSOR(Doc.of(Level.WARNING) //
				.text("Current Sensor Alarm")), //
		ALARM_BCU_BMU_COMMUNICATION(Doc.of(Level.WARNING) //
				.text("BCU BMU Communication Alarm")), //
		ALARM_CONTACTOR_ADHESION(Doc.of(Level.FAULT)//
				.text("Contactor Adhesion Alarm ")), //
		ALARM_BCU_NTC(Doc.of(Level.WARNING) //
				.text("BCU NTC Alarm")), //
		ALARM_SLAVE_CONTROL_SUMMARY(Doc.of(Level.WARNING) //
				.text("Slave Control Summary Alarm")), //
		FAILURE_INITIALIZATION(Doc.of(Level.FAULT) //
				.text("Initialization failure")), //
		FAILURE_EEPROM(Doc.of(Level.FAULT) //
				.text("EEPROM fault")), //
		FAILURE_EEPROM2(Doc.of(Level.FAULT) //
				.text("EEPROM2 fault")), //
		FAILURE_INTRANET_COMMUNICATION(Doc.of(Level.FAULT) //
				.text("Intranet communication fault")), //
		FAILURE_TEMP_SAMPLING_LINE(Doc.of(Level.FAULT) //
				.text("Temperature sampling line fault")), //
		FAILURE_BALANCING_MODULE(Doc.of(Level.FAULT) //
				.text("Balancing module fault")), //
		FAILURE_TEMP_SENSOR(Doc.of(Level.FAULT) //
				.text("Temperature sensor fault")), //
		FAILURE_TEMP_SAMPLING(Doc.of(Level.FAULT) //
				.text("Temperature sampling fault")), //
		FAILURE_VOLTAGE_SAMPLING(Doc.of(Level.FAULT) //
				.text("Voltage sampling fault")), //
		FAILURE_VOLTAGE_SAMPLING_LINE(Doc.of(Level.FAULT) //
				.text("Voltage sampling Line fault")), //
		FAILURE_SLAVE_UNIT_INITIALIZATION(Doc.of(Level.FAULT) //
				.text("Failure Slave Unit Initialization")),
		FAILURE_CONNECTING_LINE(Doc.of(Level.FAULT) //
				.text("Connecting Line Failure")), //
		FAILURE_SAMPLING_CHIP(Doc.of(Level.FAULT) //
				.text("Sampling Chip Failure")), //
		FAILURE_CONTACTOR(Doc.of(Level.FAULT) //
				.text("Contactor Failure")), //
		FAILURE_PASSIVE_BALANCE(Doc.of(Level.FAULT) //
				.text("Passive Balance Failure")), //
		FAILURE_PASSIVE_BALANCE_TEMP(Doc.of(Level.FAULT) //
				.text("Passive Balance Temp Failure")), //
		FAILURE_ACTIVE_BALANCE(Doc.of(Level.FAULT) //
				.text("Active Balance Failure")), //
		FAILURE_LTC6803(Doc.of(Level.FAULT) //
				.text("LTC6803 sfault")), //
		FAILURE_CONNECTOR_WIRE(Doc.of(Level.FAULT) //
				.text("connector wire fault")), //
		FAILURE_SAMPLING_WIRE(Doc.of(Level.FAULT) //
				.text("sampling wire fault")), //
		PRECHARGE_TAKING_TOO_LONG(Doc.of(Level.FAULT) //
				.text("precharge time was too long")), //
		NEED_CHARGE(Doc.of(Level.WARNING) //
				.text("Battery Need Charge")), //
		FAULT(Doc.of(Level.FAULT) //
				.text("battery fault state")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		PRE_ALARM_SYSTEM_VOLTAGE_HIGH(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 SYSTEM VOLTAGE HIGH")), //
		PRE_ALARM_SYSTEM_VOLTAGE_LOW(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 SYSTEM VOLTAGE LOW")), //
		PRE_ALARM_SYSTEM_VOLTAGE_UNBALANCED(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 SYSTEM VOLTAGE UNBALANCED")), //
		PRE_ALARM_INSULATION_RESISTANCE_LOWER(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 INSULATION RESISTANCE LOWER")), //
		PRE_ALARM_POS_INSULATION_RESISTANCE_LOWER(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 POS INSULATION RESISTANCE LOWER")), //
		PRE_ALARM_NEG_INSULATION_RESISTANCE_LOWER(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 NEG INSULATION RESISTANCE LOWER")), //
		PRE_ALARM_SYSTEM_SOC_LOWER(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 SYSTEM SOC LOWER")), //
		PRE_ALARM_SYSTEM_SOC_HIGH(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 SYSTEM SOC HIGH")), //
		PRE_ALARM_SOH_LOWER(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 SOH LOWER")), //
		PRE_ALARM_PACK_TEMP_HIGH(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 1 PACK TEMP HIGH")), //
		LEVEL1_SYSTEM_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 SYSTEM VOLTAGE HIGH")), //
		LEVEL1_SYSTEM_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 SYSTEM VOLTAGE LOW")), //
		LEVEL1_SYSTEM_VOLTAGE_UNBALANCED(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 SYSTEM VOLTAGE UNBALANCED")), //
		LEVEL1_INSULATION_RESISTANCE_LOWER(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 INSULATION RESISTANCE LOWER")), //
		LEVEL1_POS_INSULATION_RESISTANCE_LOWER(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 POS INSULATION RESISTANCE LOWER")), //
		LEVEL1_NEG_INSULATION_RESISTANCE_LOWER(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 NEG INSULATION RESISTANCE LOWER")), //
		LEVEL1_SYSTEM_SOC_LOWER(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 SYSTEM SOC LOWER")), //
		LEVEL1_SYSTEM_SOC_HIGH(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 SYSTEM SOC HIGH")), //
		LEVEL1_SOH_LOWER(Doc.of(Level.INFO) //
				.text("ALARM LEVEL 2 SOH LOWER")), //
		LEVEL1_PACK_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 2 PACK TEMP HIGH")), //
		LEVEL2_SYSTEM_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 SYSTEM VOLTAGE HIGH")), //
		LEVEL2_SYSTEM_VOLTAGE_LOW(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 SYSTEM VOLTAGE LOW")), //
		LEVEL2_SYSTEM_VOLTAGE_UNBALANCED(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 SYSTEM VOLTAGE UNBALANCED")), //
		LEVEL2_INSULATION_RESISTANCE_LOWER(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 INSULATION RESISTANCE LOWER")), //
		LEVEL2_POS_INSULATION_RESISTANCE_LOWER(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 POS INSULATION RESISTANCE LOWER")), //
		LEVEL2_NEG_INSULATION_RESISTANCE_LOWER(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 NEG INSULATION RESISTANCE LOWER")), //
		LEVEL2_SYSTEM_SOC_LOWER(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 SYSTEM SOC LOWER")), //
		LEVEL2_SYSTEM_SOC_HIGH(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 SYSTEM SOC HIGH")), //
		LEVEL2_SOH_LOWER(Doc.of(Level.WARNING) //
				.text("ALARM LEVEL 3 SOH LOWER")), //
		LEVEL2_PACK_TEMP_HIGH(Doc.of(Level.FAULT) //
				.text("ALARM LEVEL 3 PACK TEMP HIGH")), //
		SLAVE_11_COMMUNICATION_ERROR(Doc.of(Level.FAULT)//
				.text("Master control and Slave control Communication Fault 1 SLAVE_CTRL_11")), //
		SLAVE_12_COMMUNICATION_ERROR(Doc.of(Level.FAULT)//
				.text("Master control and Slave control Communication Fault 1 SLAVE_CTRL_12")), //
		SLAVE_13_COMMUNICATION_ERROR(Doc.of(Level.FAULT)//
				.text("Master control and Slave control Communication Fault 1 SLAVE_CTRL_13")), //
		SLAVE_14_COMMUNICATION_ERROR(Doc.of(Level.FAULT)//
				.text("Master control and Slave control Communication Fault 1 SLAVE_CTRL_14")), //
		SLAVE_15_COMMUNICATION_ERROR(Doc.of(Level.FAULT)//
				.text("Master control and Slave control Communication Fault 1 SLAVE_CTRL_15")), //
		SLAVE_16_COMMUNICATION_ERROR(Doc.of(Level.FAULT)//
				.text("Master control and Slave control Communication Fault 1 SLAVE_CTRL_16")), //
		SLAVE_17_COMMUNICATION_ERROR(Doc.of(Level.FAULT)//
				.text("Master control and Slave control Communication Fault 1 SLAVE_CTRL_17")), //
		SLAVE_18_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_18")), //
		SLAVE_19_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_19")), //
		SLAVE_20_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_20")), //
		SLAVE_21_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_21")), //
		SLAVE_22_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_22")), //
		SLAVE_23_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_23")), //
		SLAVE_24_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_24")), //
		SLAVE_25_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_25")), //
		SLAVE_26_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_26")), //
		SLAVE_27_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_27")), //
		SLAVE_28_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_28")), //
		SLAVE_29_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_29")), //
		SLAVE_30_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_30")), //
		SLAVE_31_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_31")), //
		SLAVE_32_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Master control and Slave control Communication Fault 2  SLAVE_CTRL_31")), //
		// OpenEMS Faults
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		MAX_START_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of start attempts failed")), //
		MAX_STOP_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of stop attempts failed")), //
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
}
