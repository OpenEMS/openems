package io.openems.edge.ess.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface OffGridSwitch extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		GRID_DETECTOR(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.NONE).text("Grid connection status detection")),

		MAIN_CONTACTOR(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.NONE).text("Main contactor open or close ?")),

		GROUNDING(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.NONE).text("Grounding set or not ?"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	public default BooleanReadChannel getGridcDetectorChannel() {
		return this.channel(ChannelId.GRID_DETECTOR);
	}

	public default Value<Boolean> getGridDetector() {
		return this.getGridcDetectorChannel().value();
	}

	public default void _setGridDetector(Boolean value) {
		this.getGridcDetectorChannel().setNextValue(value);
	}

	
	public default BooleanReadChannel getMainContactorChannel() {
		return this.channel(ChannelId.MAIN_CONTACTOR);
	}

	public default Value<Boolean> getMainContactor() {
		return this.getMainContactorChannel().value();
	}

	public default void _setMainContactor(Boolean value) {
		this.getMainContactorChannel().setNextValue(value);
	}
	
	
	public default BooleanReadChannel getGroundingChannel() {
		return this.channel(ChannelId.GROUNDING);
	}

	public default Value<Boolean> getGrounding() {
		return this.getGroundingChannel().value();
	}

	public default void _setGrounding(Boolean value) {
		this.getGroundingChannel().setNextValue(value);
	}

}
