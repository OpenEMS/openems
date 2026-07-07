package io.openems.common.jsonrpc.response;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.session.Role;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link GetEdgesRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "edges": {@link EdgeMetadata}[]
 *   }
 * }
 * </pre>
 */
public class GetEdgesResponse extends JsonrpcResponseSuccess {

	public record EdgeMetadata(//
			String id, //
			String comment, //
			String producttype, //
			SemanticVersion version, //
			Role role, //
			boolean isOnline, //
			ZonedDateTime lastmessage, //
			ZonedDateTime firstSetupProtocol, //
			Level sumState, //
			JsonObject settings // nullable
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetEdgesResponse.EdgeMetadata}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetEdgesResponse.EdgeMetadata> serializer() {
			return jsonObjectSerializer(GetEdgesResponse.EdgeMetadata.class, //
					json -> new GetEdgesResponse.EdgeMetadata(//
							json.getString("id"), //
							json.getString("comment"), //
							json.getString("producttype"), //
							json.getSemanticVersion("version"), //
							json.getEnum("role", Role.class), //
							json.getBoolean("isOnline"), //
							json.getZonedDateTime("lastmessage"), //
							json.getZonedDateTimeOrNull("firstSetupProtocol"), //
							json.getEnum("sumState", Level.class), //
							json.getNullableJsonObjectPath("settings").getOrNull()),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("id", obj.id()) //
							.addProperty("comment", obj.comment()) //
							.addProperty("producttype", obj.producttype()) //
							.addProperty("version", obj.version().toString()) //
							.addProperty("role", obj.role()) //
							.addProperty("isOnline", obj.isOnline()) //
							.addProperty("lastmessage", obj.lastmessage()) //
							.addProperty("firstSetupProtocol", obj.firstSetupProtocol()) //
							.addProperty("sumState", obj.sumState()) //
							.addProperty("sumState", obj.sumState()) //
							.addIfNotNull("settings", obj.settings()) //
							.build());
		}

		/**
		 * Converts a collection of EdgeMetadatas to a JsonArray.
		 *
		 * <pre>
		 * [{
		 *   "id": String,
		 *   "comment": String,
		 *   "producttype": String,
		 *   "version": String,
		 *   "role": {@link Role},
		 *   "isOnline": boolean,
		 *   "lastmessage": ZonedDateTime
		 * }]
		 * </pre>
		 *
		 * @param metadatas the EdgeMetadatas
		 * @return a JsonArray
		 */
		public static JsonArray toJson(List<EdgeMetadata> metadatas) {
			return metadatas.stream() //
					.map(EdgeMetadata::toJsonObject) //
					.collect(JsonUtils.toJsonArray());
		}

		protected JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("id", this.id) //
					.addProperty("comment", this.comment) //
					.addProperty("producttype", this.producttype) //
					.addProperty("version", this.version.toString()) //
					.add("role", this.role.asJson()) //
					.addProperty("isOnline", this.isOnline) //
					.addPropertyIfNotNull("lastmessage", this.lastmessage) //
					.addPropertyIfNotNull("sumState", this.sumState) //
					.addPropertyIfNotNull("firstSetupProtocol", this.firstSetupProtocol) //
					.addIfNotNull("settings", this.settings) //
					.build();
		}
	}

	public final List<EdgeMetadata> edgeMetadata;

	public GetEdgesResponse(UUID id, List<EdgeMetadata> edgeMetadata) {
		super(id);
		this.edgeMetadata = edgeMetadata;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("edges", EdgeMetadata.toJson(this.edgeMetadata)) //
				.build();
	}

}
