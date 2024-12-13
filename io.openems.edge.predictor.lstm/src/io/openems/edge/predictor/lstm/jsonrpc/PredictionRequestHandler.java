package io.openems.edge.predictor.lstm.jsonrpc;

import java.util.UUID;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.manager.PredictorManager;

public class PredictionRequestHandler {

	/**
	 * Handle {@link GetPredictionRequest}; return {@link GetPredictionResponse}.
	 * 
	 * @param requestId         the id
	 * @param predictionManager the {@link PredictorManager}
	 * @param channelAddress    the {@link ChannelAddress}
	 * @return the new prediction
	 */
	public static GetPredictionResponse handlerGetPredictionRequest(UUID requestId, PredictorManager predictionManager,
			ChannelAddress channelAddress) {
		var sortedMap = predictionManager.getPrediction(channelAddress).toMapWithAllQuarters();
		return new GetPredictionResponse(requestId, sortedMap);
	}
}
