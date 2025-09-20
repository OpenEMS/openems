package io.openems.backend.common.metadata;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AddAppInstanceRequest;
import io.openems.common.jsonrpc.request.AppCenterAddDeinstallInstanceHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddInstallInstanceHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddRegisterKeyHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddUnregisterKeyHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterGetInstalledAppsRequest;
import io.openems.common.jsonrpc.request.AppCenterGetPossibleAppsRequest;
import io.openems.common.jsonrpc.request.AppCenterGetRegisteredKeysRequest;
import io.openems.common.jsonrpc.request.AppCenterInstallAppWithSuppliedKeyRequest;
import io.openems.common.jsonrpc.request.AppCenterIsAppFreeRequest;
import io.openems.common.jsonrpc.request.AppCenterIsKeyApplicableRequest;
import io.openems.common.jsonrpc.request.AppCenterRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.response.AppCenterGetInstalledAppsResponse;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse;
import io.openems.common.jsonrpc.response.AppCenterGetRegisteredKeysResponse;
import io.openems.common.jsonrpc.response.AppCenterIsAppFreeResponse;
import io.openems.common.jsonrpc.response.AppCenterIsKeyApplicableResponse;

public final class AppCenterHandler {

	/**
	 * Handles a user rpc request regarding app center keys.
	 * 
	 * @param metadata         the metadata to handle the request with
	 * @param delegatedRequest the function to delegate the current request to a
	 *                         other {@link EdgeRpcRequest}
	 * @param request          the {@link AppCenterRequest}
	 * @param user             the user
	 * @param edgeId           the edge id
	 * @return the {@link CompletableFuture}
	 * @throws OpenemsNamedException on parse error
	 */
	public static CompletableFuture<? extends JsonrpcResponseSuccess> handleUserRequest(//
			final AppCenterMetadata.UiData metadata, //
			final ThrowingFunction<EdgeRpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>, OpenemsNamedException> delegatedRequest, //
			final AppCenterRequest request, //
			final User user, //
			final String edgeId //
	) throws OpenemsNamedException {
		Objects.requireNonNull(metadata, "No AppCenter Metadata provided.");

		return switch (request.getPayload().getMethod()) {
		case AppCenterAddRegisterKeyHistoryRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterAddRegisterKeyHistoryRequest::from, //
				(m, r) -> m.sendAddRegisterKeyHistory(edgeId, r.appId, r.key, user));

		case AppCenterAddUnregisterKeyHistoryRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterAddUnregisterKeyHistoryRequest::from, //
				(m, r) -> m.sendAddUnregisterKeyHistory(edgeId, r.appId, r.key, user));

		case AppCenterGetRegisteredKeysRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterGetRegisteredKeysRequest::from, //
				(m, r) -> m.sendGetRegisteredKeys(edgeId, r.appId), //
				AppCenterGetRegisteredKeysResponse::new);

		case AppCenterInstallAppWithSuppliedKeyRequest.METHOD -> handleAppCenterInstallAppWithSuppliedKeyRequest(//
				request.getPayload(), //
				delegatedRequest, //
				metadata, //
				user, //
				edgeId //
			);
		case AppCenterIsAppFreeRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterIsAppFreeRequest::from, //
				(m, r) -> m.isAppFree(user, r.appId), //
				AppCenterIsAppFreeResponse::new);

		default -> handleGeneric(metadata, request, edgeId);
		};
	}

	private static CompletableFuture<? extends JsonrpcResponseSuccess> handleAppCenterInstallAppWithSuppliedKeyRequest(//
			final JsonrpcRequest request, //
			final ThrowingFunction<EdgeRpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>, OpenemsNamedException> delegatedRequest, //
			final AppCenterMetadata.UiData metadata, //
			final User user, //
			final String edgeId //
	) throws OpenemsNamedException {
		final var genericInstallRequest = AppCenterInstallAppWithSuppliedKeyRequest.from(request).installRequest;
		final var componentRequest = ComponentJsonApiRequest.from(genericInstallRequest);
		final var installAppRequest = AddAppInstanceRequest.from(componentRequest.getPayload());

		final CompletableFuture<String> keySupplier;
		if (installAppRequest.key == null) {
			keySupplier = metadata.getSuppliableKey(user, edgeId, installAppRequest.appId);
		} else {
			keySupplier = CompletableFuture.completedFuture(installAppRequest.key);
		}

		return keySupplier.thenCompose(key -> {
			final var installRequest = new EdgeRpcRequest(edgeId, //
					new ComponentJsonApiRequest(componentRequest.getComponentId(), //
							new AddAppInstanceRequest(installAppRequest.appId, key, installAppRequest.alias,
									installAppRequest.properties)));
			try {
				return delegatedRequest.apply(installRequest);
			} catch (OpenemsNamedException e) {
				throw new CompletionException(e);
			}
		});
	}

	/**
	 * Handles a edge rpc request regarding app center keys.
	 * 
	 * @param metadata the metadata to handle the request with
	 * @param request  the {@link AppCenterRequest}
	 * @param edgeId   the edge id
	 * @return the {@link CompletableFuture}
	 * @throws OpenemsNamedException on parse error
	 */
	public static CompletableFuture<? extends JsonrpcResponseSuccess> handleEdgeRequest(//
			final AppCenterMetadata.EdgeData metadata, //
			final AppCenterRequest request, //
			final String edgeId //
	) throws OpenemsNamedException {
		Objects.requireNonNull(metadata, "No AppCenter Metadata provided.");

		return switch (request.getPayload().getMethod()) {
		case AppCenterAddInstallInstanceHistoryRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterAddInstallInstanceHistoryRequest::from, //
				(m, r) -> m.sendAddInstallAppInstanceHistory(r.key, edgeId, r.appId, r.instanceId, r.userId));

		case AppCenterAddDeinstallInstanceHistoryRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterAddDeinstallInstanceHistoryRequest::from, //
				(m, r) -> m.sendAddDeinstallAppInstanceHistory(edgeId, r.appId, r.instanceId, r.userId));

		case AppCenterGetInstalledAppsRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterGetInstalledAppsRequest::from, //
				(m, r) -> m.sendGetInstalledApps(edgeId), //
				AppCenterGetInstalledAppsResponse::from);

		default -> handleGeneric(metadata, request, edgeId);
		};
	}

	/**
	 * Handles a generic rpc request regarding app center keys.
	 * 
	 * @param metadata the metadata to handle the request with
	 * @param request  the {@link AppCenterRequest}
	 * @param edgeId   the edge id
	 * @return the {@link CompletableFuture}
	 * @throws OpenemsNamedException on parse error
	 */
	public static CompletableFuture<? extends JsonrpcResponseSuccess> handleGeneric(//
			final AppCenterMetadata metadata, //
			final AppCenterRequest request, //
			final String edgeId //
	) throws OpenemsNamedException {
		return switch (request.getPayload().getMethod()) {
		case AppCenterIsKeyApplicableRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterIsKeyApplicableRequest::from, //
				(m, r) -> m.sendIsKeyApplicable(r.key, edgeId, r.appId), //
				AppCenterIsKeyApplicableResponse::from);

		case AppCenterGetPossibleAppsRequest.METHOD -> handleAsync(metadata, request, //
				AppCenterGetPossibleAppsRequest::from, //
				(m, r) -> m.sendGetPossibleApps(r.key, edgeId), //
				AppCenterGetPossibleAppsResponse::from);

		default -> null;
		};
	}

	private static <METADATA extends AppCenterMetadata, //
			REQUEST, //
			RESULT, //
			RESPONSE extends JsonrpcResponseSuccess> //
	CompletableFuture<? extends JsonrpcResponseSuccess> handleAsync(//
			final METADATA metadata, //
			final AppCenterRequest request, //
			final ThrowingFunction<JsonrpcRequest, REQUEST, OpenemsNamedException> requestMapper, //
			final BiFunction<METADATA, REQUEST, CompletableFuture<RESULT>> metadataCall, //
			final ThrowingBiFunction<UUID, RESULT, RESPONSE, OpenemsNamedException> resultMapper //
	) throws OpenemsNamedException {
		return metadataCall.apply(metadata, requestMapper.apply(request.getPayload())) //
				.thenApply(r -> {
					try {
						return resultMapper.apply(request.id, r);
					} catch (OpenemsNamedException e) {
						throw new CompletionException(e);
					}
				});
	}

	private static <METADATA extends AppCenterMetadata, REQUEST> //
	CompletableFuture<? extends JsonrpcResponseSuccess> handleAsync(//
			final METADATA metadata, //
			final AppCenterRequest request, //
			final ThrowingFunction<JsonrpcRequest, REQUEST, OpenemsNamedException> requestMapper, //
			final BiFunction<METADATA, REQUEST, CompletableFuture<Void>> metadataCall //
	) throws OpenemsNamedException {
		return metadataCall.apply(metadata, requestMapper.apply(request.getPayload())).thenApply(r -> {
			return new GenericJsonrpcResponseSuccess(request.id);
		});
	}

	private AppCenterHandler() {
	}

}
