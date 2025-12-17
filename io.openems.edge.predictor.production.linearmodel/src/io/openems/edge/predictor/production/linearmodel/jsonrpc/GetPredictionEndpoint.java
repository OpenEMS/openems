package io.openems.edge.predictor.production.linearmodel.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.CollectorUtils.toSortedMap;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonObject;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.StringPathParser;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.production.linearmodel.jsonrpc.GetPredictionEndpoint.Response;

/**
 * JSON-RPC Request for the "getPrediction" method.
 *
 * <p>
 * Example request:
 * 
 * <pre>
 * {
 *   "method": "getPrediction",
 *   "params": {}
 * }
 * </pre>
 */
public class GetPredictionEndpoint implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "getPrediction";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(Prediction prediction) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link PredictionResponse}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> {
						var predictionResult = json.getJsonObjectPath("predictionResult") //
								.collect(new StringPathParser.StringParserZonedDateTime(), //
										toSortedMap(//
												t -> t.getKey().get(), //
												t -> t.getValue().getAsInt()));
						return new Response(Prediction.from(ImmutableSortedMap.copyOfSorted(predictionResult)));
					}, obj -> {
						var quartersMap = obj.prediction().toMapWithAllQuarters();
						var jsonPrediction = quartersMap.entrySet().stream()//
								.collect(toJsonObject(//
										e -> e.getKey().toString(), //
										e -> new JsonPrimitive(e.getValue())));
						return buildJsonObject()//
								.add("predictionResult", jsonPrediction)//
								.build();
					});
		}
	}
}
