package io.openems.edge.core.host;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Role;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemUpdateRequest;
import io.openems.edge.core.host.jsonrpc.GetNetworkConfig;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo;
import io.openems.edge.core.host.jsonrpc.GetSystemUpdateStateRequest;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig;

/**
 * The Host-Component handles access to the host computer and operating system.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = Host.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class HostImpl extends AbstractOpenemsComponent implements Host, OpenemsComponent, ComponentJsonApi {

	private final Logger log = LoggerFactory.getLogger(HostImpl.class);

	protected final OperatingSystem operatingSystem;
	private ServiceRegistration<Updateable> operatingSystemUpdateable;

	private final DiskSpaceWorker diskSpaceWorker;
	private final NetworkConfigurationWorker networkConfigurationWorker;
	private final UsbConfigurationWorker usbConfigurationWorker;
	private final SystemUpdateHandler systemUpdateHandler;

	@Reference
	protected OpenemsEdgeOem oem;

	@Reference
	protected ConfigurationAdmin cm;

	protected Config config;

	public HostImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Host.ChannelId.values() //
		);
		
		// Initialize correct Operating System handler
		this.operatingSystem = this.getCurrentOS();
		this.diskSpaceWorker = new DiskSpaceWorker(this);
		this.networkConfigurationWorker = new NetworkConfigurationWorker(this);
		this.usbConfigurationWorker = new UsbConfigurationWorker(this);
		this.systemUpdateHandler = new SystemUpdateHandler(this);

		// Initialize 'Hostname' channel
		try {
			this._setHostname(HostImpl.execReadToString("hostname"));
		} catch (IOException e) {
			try {
				this._setHostname(InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
		}

		this.operatingSystem.getOperatingSystemVersion().whenComplete((name, error) -> {
			this._setOsVersion(name);
			if (error != null) {
				this.log.info("Error while trying to get operating system version", error);
			}
		});
	}

	@Activate
	private void activate(ComponentContext componentContext, BundleContext bundleContext, Config config)
			throws OpenemsException {
		super.activate(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.config = config;

		// Start the Workers
		this.diskSpaceWorker.activate(this.id());
		this.networkConfigurationWorker.activate(this.id());
		this.usbConfigurationWorker.activate(this.id());

		final var operatingSystemUpdateable = this.operatingSystem.getSystemUpdateable();
		if (operatingSystemUpdateable != null) {
			this.operatingSystemUpdateable = bundleContext.registerService(Updateable.class, operatingSystemUpdateable,
					new Hashtable<>());
		}

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext componentContext, BundleContext bundleContext, Config config) {
		super.modified(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.config = config;

		// Modify the Workers
		this.diskSpaceWorker.modified(this.id());
		this.networkConfigurationWorker.modified(this.id());
		this.usbConfigurationWorker.modified(this.id());

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		// Stop the Workers
		this.diskSpaceWorker.deactivate();
		this.networkConfigurationWorker.deactivate();
		this.usbConfigurationWorker.deactivate();

		this.systemUpdateHandler.deactivate();

		final var operatingSystemUpdateable = this.operatingSystemUpdateable;
		if (operatingSystemUpdateable != null) {
			operatingSystemUpdateable.unregister();
		}

		super.deactivate();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetNetworkConfig(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			return this.handleGetNetworkConfigRequest();
		});

		builder.handleRequest(new SetNetworkConfig(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			this.handleSetNetworkConfigRequest(call.get(EdgeKeys.USER_KEY), call.getRequest());

			return EmptyObject.INSTANCE;
		});

		builder.handleRequest(GetSystemUpdateStateRequest.METHOD, call -> {
			return this.handleGetSystemUpdateStateRequest(call.get(EdgeKeys.USER_KEY),
					GetSystemUpdateStateRequest.from(call.getRequest())).get();
		});

		builder.handleRequest(ExecuteSystemUpdateRequest.METHOD, call -> {
			return this.handleExecuteSystemUpdateRequest(call.get(EdgeKeys.USER_KEY),
					ExecuteSystemUpdateRequest.from(call.getRequest())).get();
		});

		builder.handleRequest(ExecuteSystemCommandRequest.METHOD, call -> {
			return this.handleExecuteCommandRequest(call.get(EdgeKeys.USER_KEY),
					ExecuteSystemCommandRequest.from(call.getRequest())).get();
		});

		builder.handleRequest(ExecuteSystemRestartRequest.METHOD, call -> {
			return this.handleExecuteSystemRestartRequest(call.get(EdgeKeys.USER_KEY),
					ExecuteSystemRestartRequest.from(call.getRequest())).get();
		});

		builder.handleRequest(new GetNetworkInfo(), endpoint -> {
			endpoint.setDescription("""
					Gets the networkinfo.
					""".stripIndent());

			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> this.operatingSystem.getNetworkInfo());

	}

	@Override
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException {
		return this.operatingSystem.getSystemIPs();
	}

	/**
	 * Handles a GetNetworkConfigRequest.
	 *
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private GetNetworkConfig.Response handleGetNetworkConfigRequest() throws OpenemsNamedException {
		var config = this.operatingSystem.getNetworkConfiguration();
		return new GetNetworkConfig.Response(config);
	}

	/**
	 * Handles a SetNetworkConfigRequest.
	 *
	 * @param user    the User
	 * @param request the SetNetworkConfigRequest
	 * @throws OpenemsNamedException on error
	 */
	public void handleSetNetworkConfigRequest(User user, SetNetworkConfig.Request request)
			throws OpenemsNamedException {
		var oldNetworkConfiguration = this.operatingSystem.getNetworkConfiguration();
		this.operatingSystem.handleSetNetworkConfigRequest(user, oldNetworkConfiguration, request);

		// Notify NetworkConfigurationWorker about the change
		this.networkConfigurationWorker.triggerNextRun();
	}

	/**
	 * Handles a {@link GetSystemUpdateStateRequest}.
	 *
	 * @param user    the User
	 * @param request the {@link GetSystemUpdateStateRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetSystemUpdateStateRequest(User user,
			GetSystemUpdateStateRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleGetSystemUpdateStateRequest", Role.OWNER);

		return this.systemUpdateHandler.handleGetSystemUpdateStateRequest(request);
	}

	/**
	 * Handles a {@link ExecuteSystemUpdateRequest}.
	 *
	 * @param user    the User
	 * @param request the {@link ExecuteSystemUpdateRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleExecuteSystemUpdateRequest(User user,
			ExecuteSystemUpdateRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleSystemUpdateRequest", Role.OWNER);

		return this.systemUpdateHandler.handleExecuteSystemUpdateRequest(request);
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
		user.assertRoleIsAtLeast("handleExecuteCommandRequest", Role.ADMIN);
		return this.operatingSystem.handleExecuteSystemCommandRequest(request);
	}

	/**
	 * Handles a {@link ExecuteSystemRestartRequest}.
	 *
	 * @param user    the User
	 * @param request the {@link ExecuteSystemRestartRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleExecuteSystemRestartRequest(User user,
			ExecuteSystemRestartRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleExecuteSystemRestartRequest", Role.OWNER);
		return this.operatingSystem.handleExecuteSystemRestartRequest(request);
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

	/**
	 * Source: https://stackoverflow.com/a/28043703.
	 *
	 * @param execCommand the command
	 * @return the parsed result
	 * @throws IOException on error
	 */
	private static String execReadToString(String execCommand) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder(execCommand.split(" "));
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();

		try (var s = new Scanner(process.getInputStream()).useDelimiter("\\A")) {
			return s.hasNext() ? s.next().trim() : "";
		}
	}
	
	private OperatingSystem getCurrentOS() {
		if (Files.exists(Paths.get("/.dockerenv"))) {
			return new OperatingSystemDocker();
		}
		
		final String osName = System.getProperty("os.name");

        if (osName.startsWith("Windows")) {
            return new OperatingSystemWindows();
        } else if (osName.startsWith("Mac")) {
            return new OperatingSystemMac();
        }
		
		return new OperatingSystemDebianSystemd(this);
	}

}
