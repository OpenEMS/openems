package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.Bash.Command;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest.SystemCommand;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse.SystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartResponse;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig;

/**
 * OperatingSystem implementation for Debian with systemd.
 */
public class OperatingSystemDocker implements OperatingSystem {

	private final Path versionFile = Paths.get("/app/version");

	protected OperatingSystemDocker() {
	}

	@Override
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException {
		return new NetworkConfiguration(new TreeMap<>());
	}

	@Override
	public void handleSetNetworkConfigRequest(User user, NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfig.Request request) throws OpenemsNamedException {
		throw new OpenemsNamedException(OpenemsError.GENERIC, "Network configuration is not supported in Docker");
	}

	@Override
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteSystemCommandRequest(
			ExecuteSystemCommandRequest request) throws NotImplementedException {
		return execute(request.systemCommand).thenApply(cmd -> { //
			final var scr = new SystemCommandResponse(cmd.stdout(), cmd.stderr(), cmd.exitCode());
			return new ExecuteSystemCommandResponse(request.id, scr);
		});
	}
		
	private static CompletableFuture<Command> execute(SystemCommand sc) {
		return new Bash(sc.command()) //
				.withTimeout(sc.timeoutSeconds()) //
				.runInBackground(sc.runInBackground()) //
				.execute();
	}

	@Override
	public String getUsbConfiguration() throws OpenemsNamedException {
		return "";
	}

	@Override
	public CompletableFuture<ExecuteSystemRestartResponse> handleExecuteSystemRestartRequest(
			ExecuteSystemRestartRequest request) {
		return CompletableFuture.failedFuture(new NotImplementedException("System restart is not supported in Docker"));
	}

	@Override
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException {
		return Collections.emptyList();
	}

	@Override
	public GetNetworkInfo.Response getNetworkInfo() throws OpenemsNamedException {
		return new GetNetworkInfo.Response(List.of(), List.of());
	}

	@Override
	public CompletableFuture<String> getOperatingSystemVersion() {
		String version = "Docker";
		if (Files.exists(this.versionFile)) {
			try {
				version = "Docker " + Files.readString(this.versionFile);
			} catch (Exception e) {
				// ignore and return default version
			}
		}
		return CompletableFuture.completedFuture(version);
	}

	@Override
	public Updateable getSystemUpdateable() {
		return null;
	}

}
