package io.openems.edge.predictor.lstmmodel.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

public class GetPredictionResponse extends JsonrpcResponseSuccess {

	private final JsonArray prediction;

	public GetPredictionResponse(JsonArray prediction) {
		this(UUID.randomUUID(), prediction);
	}

	public GetPredictionResponse(UUID id, JsonArray prediction) {
		super(id);
		this.prediction = prediction;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("prediction", this.prediction) //
				.build();
	}

}
