package io.openems.edge.evcs.heidelberg.connect;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsHeidelbergConnect extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		APPLY_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Maximum charging current limit in mA")),

		// decimal 264 -> hexadecimal 0x108 -> Version V1.0.8
		LAYOUT_VERSION(Doc.of(OpenemsType.INTEGER)),

		RAW_STATE(Doc.of(HeidelbergStates.values())),

		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),

		TEMPERATURE_PCB(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),

		/**
		 * This register represents the status of the input for external lock (see
		 * manual).
		 * 
		 * <p>
		 * 0 = system locked 1 = system unlocked TODO: Check when the state is 1
		 */
		EXTERN_LOCK_STATE(Doc.of(OpenemsType.BOOLEAN)), //

		RAW_MAXIMAL_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),

		RAW_MINIMAL_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),

		/**
		 * WatchDog TimeOut for the Modbus TCP Leader.
		 * 
		 * <p>
		 * Within this period, at least one successful Modbus TCP communication must
		 * have taken place between the Modbus TCP Leader and the Modbus TCP Follower.
		 * Otherwise, the Modbus TCP Follower goes into TimeOut mode.
		 * 
		 * <p>
		 * Default timeout: 15 seconds
		 */
		WATCHDOG_TIMEOUT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).unit(Unit.MILLISECONDS)),

		// Currently unused
		REMOTE_LOCK(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),

		FAILSAFE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),;

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
	 * Gets the Channel for {@link ChannelId#APPLY_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyChargeCurrentLimitChannel() {
		return this.channel(ChannelId.APPLY_CHARGE_CURRENT_LIMIT);
	}

	/**
	 * Sets the write value of the {@link ChannelId#APPLY_CHARGE_CURRENT_LIMIT}
	 * Channel used to set the charge current limit of the EVCS in [mA].
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setApplyChargeCurrentLimit(Integer value) throws OpenemsNamedException {
		this.getApplyChargeCurrentLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCurrentL1Channel() {
		return this.channel(ChannelId.CURRENT_L1);
	}

	/**
	 * Gets the Current on phase L1 in [mA]. See {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL1() {
		return this.getCurrentL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCurrentL2Channel() {
		return this.channel(ChannelId.CURRENT_L2);
	}

	/**
	 * Gets the Current on phase L2 in [mA]. See {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL2() {
		return this.getCurrentL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCurrentL3Channel() {
		return this.channel(ChannelId.CURRENT_L3);
	}

	/**
	 * Gets the Current on phase L3 in [mA]. See {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL3() {
		return this.getCurrentL3Channel().value();
	}
}
