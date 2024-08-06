package io.openems.edge.predictor.lstmmodel.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.manager.PredictorManager;

public class PredictionRequestHandler {

	/**
	 * get predictionsReasponse.
	 * 
	 * @param requestId         the id
	 * @param predictionManager the manager
	 * @param consumption       the consumtpiotn
	 * @return the new predition
	 */
	public static GetPredictionResponse handlerGetPredictionRequest(UUID requestId, PredictorManager predictionManager,
			ChannelAddress consumption) {

		var sortedMap = predictionManager.getPrediction(consumption).valuePerQuarter;

		var predictionArray = new JsonArray();
		sortedMap.values().forEach(value -> predictionArray.add(new JsonPrimitive(value)));
		return new GetPredictionResponse(requestId, predictionArray);
	}
}
