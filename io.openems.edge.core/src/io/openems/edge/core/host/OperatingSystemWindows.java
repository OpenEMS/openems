package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig;
import io.openems.edge.common.update.Updateable;

/**
 * OperatingSystem implementation for Windows.
 */
public class OperatingSystemWindows implements OperatingSystem {

	protected OperatingSystemWindows() {
	}

	@Override
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException {
		// not implemented
		return new NetworkConfiguration(new TreeMap<>());
	}

	@Override
	public void handleSetNetworkConfigRequest(User user, NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfig.Request request) throws OpenemsNamedException {
		throw new NotImplementedException("SetNetworkConfigRequest is not implemented for Windows");
	}

	@Override
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteSystemCommandRequest(
			ExecuteSystemCommandRequest request) throws NotImplementedException {
		throw new NotImplementedException("ExecuteSystemCommandRequest is not implemented for Windows");
	}

	@Override
	public String getUsbConfiguration() throws OpenemsNamedException {
		// not implemented
		return "";
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleExecuteSystemRestartRequest(
			ExecuteSystemRestartRequest request) throws NotImplementedException {
		throw new NotImplementedException("ExecuteSystemRestartRequest is not implemented for Windows");
	}

	@Override
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException {
		return Collections.emptyList();
	}

	@Override
	public GetNetworkInfo.Response getNetworkInfo() throws OpenemsNamedException {
		throw new NotImplementedException("This request is not implemented for Windows");
	}

	public CompletableFuture<String> getOperatingSystemVersion() {
		return CompletableFuture.completedFuture(System.getProperty("os.name"));
	}

	@Override
	public Updateable getSystemUpdateable() {
		return null;
	}

}
