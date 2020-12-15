package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;

public class UpdateSoftwareAdminRequest extends JsonrpcRequest{
	
	public static UpdateSoftwareAdminRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		boolean updateEdge = JsonUtils.getAsOptionalBoolean(p, "updateEdge")
				.orElse(false);
		boolean updateUi = JsonUtils.getAsOptionalBoolean(p, "updateUi")
				.orElse(false);
		return new UpdateSoftwareAdminRequest(r.getId(), updateEdge, updateUi);
	}
	public final static String METHOD = "updateSoftwareAdmin";
	private final boolean updateEdge;
	private final boolean updateUi;
	
	public UpdateSoftwareAdminRequest(UUID id, boolean updateEdge,  boolean updateUi) {
		super(id, METHOD);
		this.updateEdge = updateEdge;
		this.updateUi = updateUi;
	}

	@Override
	public JsonObject getParams() {

		JsonObjectBuilder result = JsonUtils.buildJsonObject() //
				.addProperty("updateEdge", this.updateEdge) //
				.addProperty("updateUi", this.updateUi); //
		return result.build();
		
	}

	public boolean getUpdateUi() {
		return updateUi;
	}

	public boolean getUpdateEdge() {
		return updateEdge;
	}

}
