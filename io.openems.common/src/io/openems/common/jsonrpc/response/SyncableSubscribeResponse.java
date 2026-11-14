package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SyncableSubscribeRequest;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link SyncableSubscribeRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "entry": {@link SyncableEntry}
 *   }
 * }
 * </pre>
 */
public class SyncableSubscribeResponse extends JsonrpcResponseSuccess {

	/**
	 * Creates a {@link SyncableSubscribeResponse} from a generic
	 * {@link JsonrpcResponseSuccess} by deserializing the payload.
	 *
	 * @param r the incoming JsonrpcResponse
	 * @return the parsed SyncableSubscribeResponse
	 * @throws OpenemsError.OpenemsNamedException if required params are missing or
	 *                                            the entry cannot be deserialized
	 */
	public static SyncableSubscribeResponse from(JsonrpcResponseSuccess r) {
		var entryObj = JsonUtils.getAsOptionalJsonObject(r.getResult(), "entry");
		var entry = entryObj.map(SyncableEntry.serializer()::deserialize).orElse(null);
		return new SyncableSubscribeResponse(r.getId(), entry);
	}

	private final SyncableEntry<?> entry;

	public SyncableSubscribeResponse(UUID id, SyncableEntry<?> entry) {
		super(id);
		this.entry = entry;
	}

	public SyncableEntry<?> getEntry() {
		return this.entry;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("subscribed", true) //
				.add("entry", this.entry, SyncableEntry.serializer()) //
				.build();
	}
}
