package io.openems.backend.common.metadata;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AppCenterAddDeinstallInstanceHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddInstallInstanceHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddRegisterKeyHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddUnregisterKeyHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterGetInstalledAppsRequest;
import io.openems.common.jsonrpc.request.AppCenterGetPossibleAppsRequest;
import io.openems.common.jsonrpc.request.AppCenterGetRegisteredKeysRequest;
import io.openems.common.jsonrpc.request.AppCenterIsKeyApplicableRequest;
import io.openems.common.jsonrpc.request.AppCenterRequest;
import io.openems.common.jsonrpc.response.AppCenterGetInstalledAppsResponse;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse;
import io.openems.common.jsonrpc.response.AppCenterGetRegisteredKeysResponse;
import io.openems.common.jsonrpc.response.AppCenterIsKeyApplicableResponse;

public final class AppCenterHandler {

	private static Logger LOG = LoggerFactory.getLogger(AppCenterHandler.class);

	private AppCenterHandler() {
		super();
	}

	/**
	 * Handles a user rpc request regarding app center keys.
	 * 
	 * @param metadata the metadata to handle the request with
	 * @param request  the {@link AppCenterRequest}
	 * @param user     the user
	 * @param edgeId   the edge id
	 * @return the {@link CompletableFuture}
	 * @throws OpenemsNamedException on error
	 */
	public static CompletableFuture<? extends JsonrpcResponseSuccess> handleUserRequest(//
			final AppCenterMetadata.UiData metadata, //
			final AppCenterRequest request, //
			final User user, //
			final String edgeId //
	) throws OpenemsNamedException {
		Objects.requireNonNull(metadata, "No AppCenter Metadata provided.");

		CompletableFuture<? extends JsonrpcResponseSuccess> resultFuture = null;

		switch (request.getPayload().getMethod()) {
		case AppCenterAddRegisterKeyHistoryRequest.METHOD:
			resultFuture = handle(metadata, request, //
					AppCenterAddRegisterKeyHistoryRequest::from, //
					(m, r) -> m.sendAddRegisterKeyHistory(edgeId, r.appId, r.key, user));
			break;

		case AppCenterAddUnregisterKeyHistoryRequest.METHOD:
			resultFuture = handle(metadata, request, //
					AppCenterAddUnregisterKeyHistoryRequest::from, //
					(m, r) -> m.sendAddUnregisterKeyHistory(edgeId, r.appId, r.key, user));
			break;

		case AppCenterGetRegisteredKeysRequest.METHOD:
			resultFuture = handle(metadata, request, //
					AppCenterGetRegisteredKeysRequest::from, //
					(m, r) -> m.sendGetRegisteredKeys(edgeId, r.appId), //
					AppCenterGetRegisteredKeysResponse::new);
			break;

		}

		return resultFuture != null ? resultFuture : handleGeneric(metadata, request, edgeId);
	}

	/**
	 * Handles a edge rpc request regarding app center keys.
	 * 
	 * @param metadata the metadata to handle the request with
	 * @param request  the {@link AppCenterRequest}
	 * @param edgeId   the edge id
	 * @return the {@link CompletableFuture}
	 * @throws OpenemsNamedException on error
	 */
	public static CompletableFuture<? extends JsonrpcResponseSuccess> handleEdgeRequest(//
			final AppCenterMetadata.EdgeData metadata, //
			final AppCenterRequest request, //
			final String edgeId //
	) throws OpenemsNamedException {
		Objects.requireNonNull(metadata, "No AppCenter Metadata provided.");

		CompletableFuture<? extends JsonrpcResponseSuccess> resultFuture = null;

		switch (request.getPayload().getMethod()) {
		case AppCenterAddInstallInstanceHistoryRequest.METHOD:
			resultFuture = handle(metadata, request, //
					AppCenterAddInstallInstanceHistoryRequest::from, //
					(m, r) -> m.sendAddInstallAppInstanceHistory(r.key, edgeId, r.appId, r.instanceId, r.userId));
			break;

		case AppCenterAddDeinstallInstanceHistoryRequest.METHOD:
			resultFuture = handle(metadata, request, //
					AppCenterAddDeinstallInstanceHistoryRequest::from, //
					(m, r) -> m.sendAddDeinstallAppInstanceHistory(edgeId, r.appId, r.instanceId, r.userId));
			break;

		case AppCenterGetInstalledAppsRequest.METHOD:
			resultFuture = handle(metadata, request, //
					AppCenterGetInstalledAppsRequest::from, //
					(m, r) -> m.sendGetInstalledApps(edgeId), //
					AppCenterGetInstalledAppsResponse::from);
			break;
		}

		return resultFuture != null ? resultFuture : handleGeneric(metadata, request, edgeId);
	}

	/**
	 * Handles a generic rpc request regarding app center keys.
	 * 
	 * @param metadata the metadata to handle the request with
	 * @param request  the {@link AppCenterRequest}
	 * @param edgeId   the edge id
	 * @return the {@link CompletableFuture}
	 * @throws OpenemsNamedException on error
	 */
	public static CompletableFuture<? extends JsonrpcResponseSuccess> handleGeneric(//
			final AppCenterMetadata metadata, //
			final AppCenterRequest request, //
			final String edgeId //
	) throws OpenemsNamedException {
		switch (request.getPayload().getMethod()) {
		case AppCenterIsKeyApplicableRequest.METHOD:
			return handle(metadata, request, //
					AppCenterIsKeyApplicableRequest::from, //
					(m, r) -> m.sendIsKeyApplicable(r.key, edgeId, r.appId), //
					AppCenterIsKeyApplicableResponse::from);

		case AppCenterGetPossibleAppsRequest.METHOD:
			return handle(metadata, request, //
					AppCenterGetPossibleAppsRequest::from, //
					(m, r) -> m.sendGetPossibleApps(r.key, edgeId), //
					AppCenterGetPossibleAppsResponse::from);
		}
		return null;
	}

	private static <METADATA extends AppCenterMetadata, //
			REQUEST, //
			RESULT, //
			RESPONSE extends JsonrpcResponseSuccess> //
	CompletableFuture<? extends JsonrpcResponseSuccess> handle(//
			final METADATA metadata, //
			final AppCenterRequest request, //
			final ThrowingFunction<JsonrpcRequest, REQUEST, OpenemsNamedException> requestMapper, //
			final ThrowingBiFunction<METADATA, REQUEST, RESULT, OpenemsNamedException> metadataCall, //
			final ThrowingBiFunction<UUID, RESULT, RESPONSE, OpenemsNamedException> resultMapper //
	) throws OpenemsNamedException {
		var result = metadataCall.apply(metadata, requestMapper.apply(request.getPayload()));
		if (result == null) {
			if (resultMapper == null) {
				LOG.warn("Got no result for request " + request.getPayload().getMethod() + " but expected one!");
			}
			return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.id));
		}
		return CompletableFuture.completedFuture(resultMapper.apply(request.id, result));
	}

	private static <METADATA extends AppCenterMetadata, //
			REQUEST, //
			RESULT, //
			RESPONSE extends JsonrpcResponseSuccess> //
	CompletableFuture<? extends JsonrpcResponseSuccess> handle(//
			final METADATA metadata, //
			final AppCenterRequest request, //
			final ThrowingFunction<JsonrpcRequest, REQUEST, OpenemsNamedException> requestMapper, //
			final ThrowingBiConsumer<METADATA, REQUEST, OpenemsNamedException> metadataCall //
	) throws OpenemsNamedException {
		return handle(metadata, request, requestMapper, (t, u) -> {
			metadataCall.accept(t, u);
			return null;
		}, null);
	}

}
