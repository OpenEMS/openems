package io.openems.edge.core.host;

import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.edge.common.channel.Doc;
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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DISK_IS_FULL(Doc.of(Level.WARNING) //
				.text("Disk is full")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	// only systemd-network is implemented currently
	private final OperatingSystem operatingSystem;

	private final HostWorker hostWorker;

	public Host() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
		this.operatingSystem = new OperatingSystemDebianSystemd(this);
		this.hostWorker = new HostWorker(this);
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext) throws OpenemsException {
		super.activate(componentContext, OpenemsConstants.HOST_ID, "Host", true);

		// Start the Host Worker
		this.hostWorker.activate(this.id());
	}

	@Deactivate
	protected void deactivate() {
		// Stop the Host Worker
		this.hostWorker.deactivate();

		super.deactivate();
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.ADMIN);

		switch (request.getMethod()) {

		case GetNetworkConfigRequest.METHOD:
			return this.handleGetNetworkConfigRequest(user, GetNetworkConfigRequest.from(request));

		case SetNetworkConfigRequest.METHOD:
			return this.handleSetNetworkConfigRequest(user, SetNetworkConfigRequest.from(request));

		case ExecuteSystemCommandRequest.METHOD:
			return this.handleExecuteCommandRequest(user, ExecuteSystemCommandRequest.from(request));

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

	/**
	 * Handles a ExecuteCommandRequest.
	 * 
	 * @param user    the User
	 * @param request the ExecuteCommandRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleExecuteCommandRequest(User user,
			ExecuteSystemCommandRequest request) throws OpenemsNamedException {
		return this.operatingSystem.handleExecuteCommandRequest(request);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}
}
