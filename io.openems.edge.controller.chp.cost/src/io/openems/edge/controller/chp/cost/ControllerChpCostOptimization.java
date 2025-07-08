package io.openems.edge.controller.chp.cost;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerChpCostOptimization extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		SET_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)),

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


	//
	public default void _setPowerPercent(Integer value) throws OpenemsNamedException {
		this.getSetPowerPercentChannel().setNextWriteValue(value);
	}

	public default void _setPowerPercent(int value) throws OpenemsNamedException {
		this.getSetPowerPercentChannel().setNextWriteValue(value);
	}

	public default Value<Integer> getPowerPercent() {
		return this.getPowerPercentChannel().value();
	}

	public default IntegerReadChannel getPowerPercentChannel() {
		return this.channel(ChannelId.SET_POWER_PERCENT);
	}	
	
	public default IntegerWriteChannel getSetPowerPercentChannel() {
		return this.channel(ChannelId.SET_POWER_PERCENT);
	}		
	
}
