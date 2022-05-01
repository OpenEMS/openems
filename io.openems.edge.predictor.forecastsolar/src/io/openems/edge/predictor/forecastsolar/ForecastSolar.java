package io.openems.edge.predictor.forecastsolar;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

public interface ForecastSolar extends OpenemsComponent, Predictor24Hours {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PREDICTED_18H_AGO(Doc.of(OpenemsType.INTEGER)), //
		PREDICTED_14H_AGO(Doc.of(OpenemsType.INTEGER)), //
		PREDICTED_10H_AGO(Doc.of(OpenemsType.INTEGER)), //
		PREDICTED_6H_AGO(Doc.of(OpenemsType.INTEGER)), //
		PREDICTED_2H_AGO(Doc.of(OpenemsType.INTEGER)), //
		ACTUAL(Doc.of(OpenemsType.INTEGER)), //
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
