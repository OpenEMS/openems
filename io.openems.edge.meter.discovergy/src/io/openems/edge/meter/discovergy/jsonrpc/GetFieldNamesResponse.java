package io.openems.edge.meter.discovergy.jsonrpc;

import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'getFieldNames'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "fieldNames": [
 *     	 {@link Field#getName()}
 *     ]
 *   }
 * }
 * </pre>
 */
public class GetFieldNamesResponse extends JsonrpcResponseSuccess {

	private final Set<Field> fields;

	public GetFieldNamesResponse(Set<Field> fields) {
		this(UUID.randomUUID(), fields);
	}

	public GetFieldNamesResponse(UUID id, Set<Field> fields) {
		super(id);
		this.fields = fields;
	}

	@Override
	public JsonObject getResult() {
		var fieldNames = new JsonArray();
		for (Field field : this.fields) {
			fieldNames.add(field.n());
		}
		return JsonUtils.buildJsonObject() //
				.add("fieldNames", fieldNames) //
				.build();
	}

	/**
	 * Gets the {@link Field}s.
	 *
	 * @return a set of Fields
	 */
	public Set<Field> getFields() {
		return this.fields;
	}

}
