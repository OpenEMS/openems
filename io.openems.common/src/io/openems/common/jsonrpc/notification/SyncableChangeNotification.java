package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.utils.JsonUtils;

public class SyncableChangeNotification extends JsonrpcNotification {

	public static final String METHOD = "syncable/onchange";

	/**
	 * Creates a {@link SyncableChangeNotification} from a generic
	 * {@link JsonrpcNotification} by deserializing the payload.
	 *
	 * @param n the incoming Jsonrpc notification
	 * @return the parsed SyncableChangeNotification
	 * @throws OpenemsError.OpenemsNamedException if required params are missing or
	 *                                            the entry cannot be deserialized
	 */
	public static SyncableChangeNotification from(JsonrpcNotification n) throws OpenemsError.OpenemsNamedException {
		var j = n.getParams();
		var entry = SyncableEntry.serializer().deserialize(JsonUtils.getAsJsonObject(j, "entry"));
		return new SyncableChangeNotification(entry);
	}

	private final SyncableEntry<?> entry;

	public SyncableChangeNotification(SyncableEntry<?> entry) {
		super(METHOD);
		this.entry = entry;
	}

	public SyncableEntry<?> getEntry() {
		return this.entry;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("entry", this.entry, SyncableEntry.serializer()) //
				.build();
	}
}
