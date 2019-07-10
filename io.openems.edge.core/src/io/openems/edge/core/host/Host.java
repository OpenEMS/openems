package io.openems.edge.core.host;

import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

/**
 * The Host-Component handles access to the host computer and operating system.
 */
@Component(//
		name = "Core.Host", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.HOST_ID, //
				"enabled=true" //
		})
public class Host extends AbstractOpenemsComponent implements OpenemsComponent, JsonApi {

	// only systemd-network is implemented currently
	private final OperatingSystem operatingSystem = new OperatingSystemDebianSystemd();

	public Host() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext) throws OpenemsException {
		super.activate(componentContext, OpenemsConstants.HOST_ID, "Host", true);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.ADMIN);

		switch (request.getMethod()) {

		case GetNetworkConfigRequest.METHOD:
			return this.handleGetNetworkConfigRequest(user, GetNetworkConfigRequest.from(request));

		case SetNetworkConfigRequest.METHOD:
			return this.handleSetNetworkConfigRequest(user, SetNetworkConfigRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetNetworkConfigRequest.
	 * 
	 * @param user    the User
	 * @param request the GetNetworkConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetNetworkConfigRequest(User user,
			GetNetworkConfigRequest request) throws OpenemsNamedException {
		NetworkConfiguration config = this.operatingSystem.getNetworkConfiguration();
		GetNetworkConfigResponse response = new GetNetworkConfigResponse(request.getId(), config);
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a SetNetworkConfigRequest.
	 * 
	 * @param user    the User
	 * @param request the SetNetworkConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetNetworkConfigRequest(User user,
			SetNetworkConfigRequest request) throws OpenemsNamedException {
		NetworkConfiguration oldNetworkConfiguration = this.operatingSystem.getNetworkConfiguration();
		this.operatingSystem.handleSetNetworkConfigRequest(oldNetworkConfiguration, request);
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}
}
