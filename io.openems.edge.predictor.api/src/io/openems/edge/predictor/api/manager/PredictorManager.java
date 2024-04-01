package io.openems.edge.predictor.api.manager;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.prediction.Prediction;

public interface PredictorManager extends OpenemsComponent {

	public static final String SINGLETON_SERVICE_PID = "Core.PredictorManager";
	public static final String SINGLETON_COMPONENT_ID = "_predictorManager";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
	 * Gets the {@link Prediction} by the best matching {@link Predictor} for the
	 * given {@link ChannelAddress}.
	 *
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link Prediction}; {@link Prediction#EMPTY_PREDICTION} if no Predictor
	 *         matches the Channel-Address
	 */
	public Prediction getPrediction(ChannelAddress channelAddress);
}
