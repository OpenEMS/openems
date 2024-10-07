package io.openems.edge.predictor.lstmmodel.jsonrpc;

import java.util.UUID;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.manager.PredictorManager;

public class PredictionRequestHandler {

	/**
	 * get predictionsReasponse.
	 * 
	 * @param requestId         the id
	 * @param predictionManager the manager
	 * @param channelAddress    the {@link ChannelAddress}
	 * @return the new prediction
	 */
	public static GetPredictionResponse handlerGetPredictionRequest(UUID requestId, PredictorManager predictionManager,
			ChannelAddress channelAddress) {

		var sortedMap = predictionManager.getPrediction(channelAddress).valuePerQuarter;
		return new GetPredictionResponse(requestId, sortedMap);
	}
}