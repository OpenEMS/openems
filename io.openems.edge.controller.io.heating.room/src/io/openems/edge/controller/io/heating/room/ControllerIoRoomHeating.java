package io.openems.edge.controller.io.heating.room;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.ElectricityMeter;

public interface ControllerIoRoomHeating extends Controller, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FLOOR_ACTUAL(Doc.of(OpenemsType.INTEGER)), //
		FLOOR_TARGET(Doc.of(OpenemsType.INTEGER)), //
		AMBIENT_ACTUAL(Doc.of(OpenemsType.INTEGER)), //
		AMBIENT_TARGET(Doc.of(OpenemsType.INTEGER)), //
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
	 * Gets the Channel for {@link ChannelId#FLOOR_ACTUAL}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getFloorActualChannel() {
		return this.channel(ChannelId.FLOOR_ACTUAL);
	}

	/**
	 * Gets the Floor Actual Temperature in [deci degC]. See
	 * {@link ChannelId#FLOOR_ACTUAL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFloorActual() {
		return this.getFloorActualChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FLOOR_ACTUAL}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFloorActual(Integer value) {
		this.getFloorActualChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#FLOOR_TARGET}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getFloorTargetChannel() {
		return this.channel(ChannelId.FLOOR_TARGET);
	}

	/**
	 * Gets the Floor Target Temperature in [deci degC]. See
	 * {@link ChannelId#FLOOR_TARGET}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFloorTarget() {
		return this.getFloorTargetChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FLOOR_TARGET}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFloorTarget(Integer value) {
		this.getFloorTargetChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AMBIENT_ACTUAL}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAmbientActualChannel() {
		return this.channel(ChannelId.AMBIENT_ACTUAL);
	}

	/**
	 * Gets the Ambient Actual Temperature in [deci degC]. See
	 * {@link ChannelId#AMBIENT_ACTUAL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAmbientActual() {
		return this.getAmbientActualChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#AMBIENT_ACTUAL}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAmbientActual(Integer value) {
		this.getAmbientActualChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AMBIENT_TARGET}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAmbientTargetChannel() {
		return this.channel(ChannelId.AMBIENT_TARGET);
	}

	/**
	 * Gets the Ambient Target Temperature in [deci degC]. See
	 * {@link ChannelId#AMBIENT_TARGET}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAmbientTarget() {
		return this.getAmbientTargetChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#AMBIENT_TARGET}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAmbientTarget(Integer value) {
		this.getAmbientTargetChannel().setNextValue(value);
	}

}
