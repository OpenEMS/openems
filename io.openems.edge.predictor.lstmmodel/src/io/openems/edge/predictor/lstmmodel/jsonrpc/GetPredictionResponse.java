package io.openems.edge.predictor.lstmmodel.jsonrpc;

import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;

public class GetPredictionResponse extends JsonrpcResponseSuccess {

	private JsonArray prediction;
	private SortedMap<ZonedDateTime, Integer> predictionResult;

	public GetPredictionResponse(JsonArray prediction) {
		this(UUID.randomUUID(), prediction);
	}

	public GetPredictionResponse(UUID id, JsonArray prediction) {
		super(id);
		this.prediction = prediction != null ? prediction : new JsonArray();
		this.predictionResult = null;
	}

	public GetPredictionResponse(UUID id, SortedMap<ZonedDateTime, Integer> predictionResult) {
		super(id);
		this.predictionResult = predictionResult;
		this.prediction = new JsonArray();
		if (predictionResult != null) {
			predictionResult.values().forEach(value -> {
				this.prediction.add(value != null ? new JsonPrimitive(value) : JsonNull.INSTANCE);
			});
		}
	}

	@Override
	public JsonObject getResult() {
		JsonObjectBuilder result = JsonUtils.buildJsonObject() //
				.add("prediction", this.prediction) //
				.add("size", new JsonPrimitive(this.prediction.size()));

		if (this.predictionResult != null) {
			result.add("TimeValueMap", new JsonPrimitive(this.predictionResult.toString()));
		} else {
			result.add("timeValueMap", JsonNull.INSTANCE);
		}
		return result.build();
	}
}