package io.openems.edge.energy.api;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.Call;

/**
 * The global Energy Schedule optimizer singleton.
 */
public interface EnergyScheduler extends OpenemsComponent {

	public static final String SINGLETON_SERVICE_PID = "Core.Energy";
	public static final String SINGLETON_COMPONENT_ID = "_energy";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIMULATIONS_PER_QUARTER(Doc.of(OpenemsType.INTEGER));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Handles a GetScheduleRequest.
	 * 
	 * <p>
	 * TODO remove after v1
	 * 
	 * @param call the JsonApi {@link Call}
	 * @param id   the Component-ID of the Controller
	 * @return the GetScheduleResponse
	 */
	public JsonrpcResponse handleGetScheduleRequestV1(Call<JsonrpcRequest, JsonrpcResponse> call, String id);
}
