package io.openems.edge.core.host;

import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfigRequest;

/**
 * OperatingSystem implementation for Windows.
 */
public class OperatingSystemWindows implements OperatingSystem {

	protected OperatingSystemWindows() {
	}

	/**
	 * Gets the current network configuration.
	 *
	 * @return the current network configuration
	 * @throws OpenemsException on error
	 */
	@Override
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException {
		// not implemented
		return new NetworkConfiguration(new TreeMap<>());
	}

	/**
	 * Handles a SetNetworkConfigRequest for Windows.
	 *
	 * @param oldNetworkConfiguration the current/old network configuration
	 * @param request                 the JSON-RPC request
	 * @throws OpenemsException on error
	 */
	@Override
	public void handleSetNetworkConfigRequest(NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfigRequest request) throws OpenemsNamedException {
		throw new NotImplementedException("SetNetworkConfigRequest is not implemented for Windows");
	}

	@Override
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteCommandRequest(
			ExecuteSystemCommandRequest request) throws NotImplementedException {
		throw new NotImplementedException("ExecuteSystemCommandRequest is not implemented for Windows");
	}

	@Override
	public String getUsbConfiguration() throws OpenemsNamedException {
		// not implemented
		return "";
	}

}
