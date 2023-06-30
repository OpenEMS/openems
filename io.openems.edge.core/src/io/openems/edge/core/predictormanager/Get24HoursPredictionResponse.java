package io.openems.edge.core.predictormanager;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;

/**
 * Wraps a JSON-RPC Response to "get24HoursPrediction" Request.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "componentId/channelId": [
 *         value1, value2,... // 96 values; one value per 15 minutes
 *     ]
 *   }
 * }
 * </pre>
 */
public class Get24HoursPredictionResponse extends JsonrpcResponseSuccess {

	private final Map<ChannelAddress, Prediction24Hours> predictions;

	public Get24HoursPredictionResponse(UUID id, Map<ChannelAddress, Prediction24Hours> predictions) {
		super(id);
		this.predictions = predictions;
	}

	@Override
	public JsonObject getResult() {
		var j = new JsonObject();
		for (Entry<ChannelAddress, Prediction24Hours> entry : this.predictions.entrySet()) {
			var values = new JsonArray();
			for (Integer value : entry.getValue().getValues()) {
				values.add(value);
			}
			j.add(entry.getKey().toString(), values);
		}
		return j;
	}

	/**
	 * Gets the {@link Prediction24Hours}s per {@link ChannelAddress}.
	 *
	 * @return a map of Predictions
	 */
	public Map<ChannelAddress, Prediction24Hours> getPredictions() {
		return this.predictions;
	}

}
