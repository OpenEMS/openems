package io.openems.common.bridge.http.api;

public class EndpointFetcherEvents {

	public record RequestStartEvent(//
			long requestId, //
			BridgeHttp.Endpoint endpoint //
	) {

	}

	public static final BridgeHttpEventDefinition<RequestStartEvent> REQUEST_START//
			= new BridgeHttpEventDefinition<>();

	public record RequestFinishedEvent(//
			long requestId, //
			BridgeHttp.Endpoint endpoint //
	) {

	}

	public static final BridgeHttpEventDefinition<RequestFinishedEvent> REQUEST_FINISHED//
			= new BridgeHttpEventDefinition<>();

	public record RequestSuccessEvent(//
			long requestId, //
			HttpResponse<String> result, //
			BridgeHttp.Endpoint endpoint //
	) {

	}

	public static final BridgeHttpEventDefinition<RequestSuccessEvent> REQUEST_SUCCESS//
			= new BridgeHttpEventDefinition<>();

	public record RequestFailedEvent(//
			long requestId, //
			Exception error, //
			BridgeHttp.Endpoint endpoint //
	) {

	}

	public static final BridgeHttpEventDefinition<RequestFailedEvent> REQUEST_FAILED//
			= new BridgeHttpEventDefinition<>();

}
