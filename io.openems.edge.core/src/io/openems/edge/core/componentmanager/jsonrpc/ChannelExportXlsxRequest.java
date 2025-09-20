package io.openems.edge.core.componentmanager.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Exports Channels with current value and metadata to an Excel (xlsx) file.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "channelExportXlsx",
 *   "params": {
 *   	"componentId": string
 *   }
 * }
 * </pre>
 */
public class ChannelExportXlsxRequest extends JsonrpcRequest {

	public static final String METHOD = "channelExportXlsx";

	/**
	 * Create {@link ChannelExportXlsxRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link ChannelExportXlsxRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static ChannelExportXlsxRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var componentId = JsonUtils.getAsString(p, "componentId");
		return new ChannelExportXlsxRequest(r, componentId);
	}

	private final String componentId;

	public ChannelExportXlsxRequest(String componentId) {
		super(METHOD);
		this.componentId = componentId;
	}

	private ChannelExportXlsxRequest(JsonrpcRequest request, String componentId) {
		super(request, METHOD);
		this.componentId = componentId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.build();
	}

	/**
	 * Gets the Component-ID.
	 *
	 * @return Component-ID
	 */
	public String getComponentId() {
		return this.componentId;
	}

}
