package io.openems.edge.controller.ess.reactivepowervoltagecharacteristic;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssReactivePowerVoltageCharacteristic extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CALCULATED_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		VOLTAGE_RATIO(Doc.of(OpenemsType.FLOAT)), //
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
	 * Gets the Channel for {@link ChannelId#CALCULATED_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCalculatedPowerChannel() {
		return this.channel(ChannelId.CALCULATED_POWER);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CALCULATED_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCalculatedPower(Integer value) {
		this.getCalculatedPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_RATIO}.
	 *
	 * @return the Channel
	 */
	public default FloatReadChannel getVoltageRatioChannel() {
		return this.channel(ChannelId.VOLTAGE_RATIO);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE_RATIO}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageRatio(Float value) {
		this.getVoltageRatioChannel().setNextValue(value);
	}

}
