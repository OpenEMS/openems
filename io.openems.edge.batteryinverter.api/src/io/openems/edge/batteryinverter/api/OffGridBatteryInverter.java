package io.openems.edge.batteryinverter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;

@ProviderType
public interface OffGridBatteryInverter
		extends ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, StartStoppable, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Off-Grid-Frequency.
		 *
		 * <p>
		 * In Off-Grid Mode the Battery-Inverter should generate this frequency.
		 *
		 * <ul>
		 * <li>Interface: {@link OffGridBatteryInverter}
		 * <li>Type: Integer
		 * <li>Unit: Hz
		 * <li>Range: 40-60
		 * </ul>
		 */
		OFF_GRID_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.HERTZ)),

		/**
		 * Inverter-State.
		 *
		 * <ul>
		 * <li>Interface: {@link OffGridBatteryInverter}
		 * <li>Type: Boolean
		 * </ul>
		 */
		INVERTER_STATE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Is System ON?")), //
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
	 * Gets the Channel for {@link ChannelId#FREQUENCY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getOffGridFrequencyChannel() {
		return this.channel(ChannelId.OFF_GRID_FREQUENCY);
	}

	/**
	 * Gets the * {@link ChannelId#OFF_GRID_FREQUENCY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOffGridFrequency() {
		return this.getOffGridFrequencyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#OFF_GRID_FREQUENCY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOffGridFrequency(Integer value) {
		this.getOffGridFrequencyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#OFF_GRID_FREQUENCY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOffGridFrequency(int value) {
		this.getOffGridFrequencyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OFF_GRID_FREQUENCY}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetOffGridFrequencyChannel() {
		return this.channel(ChannelId.OFF_GRID_FREQUENCY);
	}

	/**
	 * Sets an Off Grid Frequency set point in [Hz]. See
	 * {@link ChannelId#SET_ACTIVE_POWER_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setTargetOffGridFrequency(Integer value) throws OpenemsNamedException {
		this.getSetOffGridFrequencyChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_STATE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInverterStateChannel() {
		return this.channel(ChannelId.INVERTER_STATE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#INVERTER_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getInverterState() {
		return this.getInverterStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#INVERTER_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setInverterState(Object value) {
		this.getInverterStateChannel().setNextValue(value);
	}

	public static enum TargetGridMode {
		GO_ON_GRID, GO_OFF_GRID;
	}

	/**
	 * Tells the Battery-Inverter to go to ON_GRID or OFF_GRID mode. Be sure to call
	 * this method before you call {@link #start()}.
	 *
	 * @param targetGridMode the {@link TargetGridMode}
	 */
	public void setTargetGridMode(TargetGridMode targetGridMode);
}
