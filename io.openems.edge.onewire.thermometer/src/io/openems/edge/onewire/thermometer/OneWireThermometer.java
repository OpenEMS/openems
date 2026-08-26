package io.openems.edge.onewire.thermometer;

import static io.openems.common.channel.Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;

public interface OneWireThermometer extends Thermometer, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(5, TRUE_VALUES_IN_A_ROW_TO_SET_TRUE));

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
