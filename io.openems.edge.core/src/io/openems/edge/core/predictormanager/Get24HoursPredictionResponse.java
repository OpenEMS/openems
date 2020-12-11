package io.openems.edge.core.predictormanager;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

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
 *     "values": number[96] // one value per 15 minutes
 *   }
 * }
 * </pre>
 */
public class Get24HoursPredictionResponse extends JsonrpcResponseSuccess {

	private final Integer[] values;

	public Get24HoursPredictionResponse(UUID id, Integer[] values) {
		super(id);
		this.values = values;
	}

	@Override
	public JsonObject getResult() {
		JsonArray values = new JsonArray();
		for (Integer value : this.values) {
			values.add(value);
		}
		JsonObject j = new JsonObject();
		j.add("values", values);
		return j;
	}

}
