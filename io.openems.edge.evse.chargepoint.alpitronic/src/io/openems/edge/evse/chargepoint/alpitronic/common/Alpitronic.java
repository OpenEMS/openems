package io.openems.edge.evse.chargepoint.alpitronic.common;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.KILOWATT_HOURS;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.VOLT_AMPERE_REACTIVE;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;

public interface Alpitronic extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Apply charge power limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power
		 */
		APPLY_CHARGE_POWER_LIMIT(Doc.of(INTEGER)//
				.unit(WATT)//
				.accessMode(READ_WRITE)//
				.persistencePriority(HIGH)),

		// For older versions this register returned 0 so we calculate activepower
		// ourselves with voltage and current (this channel is for testing/debugging)
		RAW_CHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)),

		CHARGED_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)),

		CHARGED_ENERGY(Doc.of(DOUBLE)//
				.unit(KILOWATT_HOURS)),

		EV_SOC(Doc.of(INTEGER)//
				.unit(PERCENT)),

		CONNECTOR_TYPE(Doc.of(SelectedConnector.values())), //

		EV_MAX_CHARGING_POWER(Doc.of(INTEGER)//
				.unit(WATT)),

		EV_MIN_CHARGING_POWER(Doc.of(INTEGER)//
				.unit(WATT)),

		/**
		 * Maximum possible inductive VAR, e. g. 1500 VAR
		 */
		VAR_REACTIVE_MAX(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)),

		/**
		 * Maximum possible capacitive VAR, e. g. -1500 VAR
		 */
		VAR_REACTIVE_MIN(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)),

		SETPOINT_REACTIVE_POWER(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.accessMode(WRITE_ONLY)),

		RAW_CHARGE_POWER_SET(Doc.of(INTEGER)//
				.unit(WATT)),;

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
	 * Gets the Channel for {@link ChannelId#APPLY_CHARGE_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyChargePowerLimitChannel() {
		return this.channel(ChannelId.APPLY_CHARGE_POWER_LIMIT);
	}

	/**
	 * Sets the charge power limit of the EVCS in [W] on
	 * {@link ChannelId#APPLY_CHARGE_POWER_LIMIT} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setApplyChargePowerLimit(Integer value) throws OpenemsNamedException {
		this.getApplyChargePowerLimitChannel().setNextWriteValue(value);
	}
}
