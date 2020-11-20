package io.openems.edge.core.host;

import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

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
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.GetNetworkConfigRequest;
import io.openems.edge.core.host.jsonrpc.GetNetworkConfigResponse;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfigRequest;

/**
 * The Host-Component handles access to the host computer and operating system.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Core.Host", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.HOST_ID, //
				"enabled=true" //
		})
public class HostImpl extends AbstractOpenemsComponent implements Host, OpenemsComponent, JsonApi {

	@Reference
	protected ConfigurationAdmin cm;

	// only systemd-network is implemented currently
	protected final OperatingSystem operatingSystem;

	private final DiskSpaceWorker diskSpaceWorker;
	private final NetworkConfigurationWorker networkConfigurationWorker;
	private final UsbConfigurationWorker usbConfigurationWorker;

	protected Config config;

	public HostImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Host.ChannelId.values() //
		);

		// Initialize correct Operating System handler
		if (System.getProperty("os.name").startsWith("Windows")) {
			this.operatingSystem = new OperatingSystemWindows();
		} else {
			this.operatingSystem = new OperatingSystemDebianSystemd(this);
		}

		this.diskSpaceWorker = new DiskSpaceWorker(this);
		this.networkConfigurationWorker = new NetworkConfigurationWorker(this);
		this.usbConfigurationWorker = new UsbConfigurationWorker(this);
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext, Config config)
			throws OpenemsException {
		super.activate(componentContext, OpenemsConstants.HOST_ID, "Host", true);

		this.config = config;

		// Start the Workers
		this.diskSpaceWorker.activate(this.id());
		this.networkConfigurationWorker.activate(this.id());
		this.usbConfigurationWorker.activate(this.id());

		this.networkConfigurationWorker.triggerNextRun();
		this.usbConfigurationWorker.triggerNextRun();
	}

	@Deactivate
	protected void deactivate() {
		// Stop the Workers
		this.diskSpaceWorker.deactivate();
		this.networkConfigurationWorker.deactivate();
		this.usbConfigurationWorker.deactivate();

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

		// Notify NetworkConfigurationWorker about the change
		this.networkConfigurationWorker.triggerNextRun();

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

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}
}
