package io.openems.edge.ess.saxpower.ess;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface SaxPower extends ManagedSinglePhaseEss, ManagedAsymmetricEss, ManagedSymmetricEss, SinglePhaseEss,
		AsymmetricEss, SymmetricEss, OpenemsComponent, ModbusComponent, ModbusSlave {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// Address 40030
		POWER_SCALE_FACTOR(Doc.of(INTEGER)),

		// Address 40049
		POWER_TARGET(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)
				.text("Power setpoint in 0.01 %; -10000 = full charge, +10000 = full discharge")),

		// Address 40050
		TIMEOUT(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)//
				.unit(SECONDS)),

		// Address 40051
		CONTROL_MODE(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)),

		// Address 40053
		REFERENCE_MAXIMUM_POWER(Doc.of(INTEGER)//
				.unit(WATT)),

		// 4098
		MAX_CHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)),

		// 4099
		MAX_DISCHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#POWER_TARGET}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getTargetPowerChannel() {
		return this.channel(ChannelId.POWER_TARGET);
	}

	/**
	 * Set the Power Target. See {@link ChannelId#POWER_TARGET}.
	 *
	 * @param value the next value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	default void setPowerTarget(Integer value) throws OpenemsError.OpenemsNamedException {
		this.getTargetPowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getTimeoutChannel() {
		return this.channel(ChannelId.TIMEOUT);
	}

	/**
	 * Gets the Timeout. See {@link ChannelId#TIMEOUT}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Integer> getTimeout() {
		return this.getTimeoutChannel().value();
	}

	/**
	 * Set the Timeout. See {@link ChannelId#TIMEOUT}.
	 *
	 * @param value the next value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	default void setTimeout(Integer value) throws OpenemsError.OpenemsNamedException {
		this.getTimeoutChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getControlModeChannel() {
		return this.channel(ChannelId.CONTROL_MODE);
	}

	/**
	 * Gets the Control Mode. See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Integer> getControlMode() {
		return this.getControlModeChannel().value();
	}

	/**
	 * Set the Control Mode. See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @param value the next value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	default void setControlMode(Integer value) throws OpenemsError.OpenemsNamedException {
		this.getControlModeChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REFERENCE_MAXIMUM_POWER}.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getReferenceMaximumPowerChannel() {
		return this.channel(ChannelId.REFERENCE_MAXIMUM_POWER);
	}

	/**
	 * Gets the reference value for 100 % power in [W]. See
	 * {@link ChannelId#REFERENCE_MAXIMUM_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Integer> getReferenceMaximumPower() {
		return this.getReferenceMaximumPowerChannel().value();
	}

	@Override
	default void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsError.OpenemsNamedException {
		ManagedSinglePhaseEss.super.applyPower(activePowerL1, reactivePowerL1, activePowerL2, reactivePowerL2,
				activePowerL3, reactivePowerL3);
	}
}