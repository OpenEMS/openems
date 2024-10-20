package io.openems.edge.predictor.lstmmodel.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/*
 * url = http://localhost:8084/jsonrpc
 * {
 *   "method": "componentJsonApi",
 *   "params": {
 *      "componentId": "predictor0",
 *      "payload": {
 *          "method": "getLstmPrediction",
 *          "params": {
 *             "id": "edge0"
 *         }
 *     }
 *   }
 * }
 */
public class GetPredictionRequest extends JsonrpcRequest {

	public static final String METHOD = "getLstmPrediction";

	/**
	 * get predictions.
	 * 
	 * @param r the request
	 * @return new prediction
	 * @throws on error
	 */
	public static GetPredictionRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetPredictionRequest(r);
	}

	public GetPredictionRequest() {
		super(METHOD);
	}

	private GetPredictionRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}
}
