package io.openems.edge.core.host;

import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfigRequest;

public interface OperatingSystem {

	/**
	 * Gets the network configuration.
	 *
	 * @return the network configuration object
	 * @throws OpenemsNamedException on error
	 */
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException;

	/**
	 * Handles a SetNetworkConfigRequest.
	 *
	 * @param user                    the User
	 * @param oldNetworkConfiguration the current/old network configuration
	 * @param request                 the SetNetworkConfigRequest
	 * @throws OpenemsNamedException on error
	 */
	public void handleSetNetworkConfigRequest(User user, NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfigRequest request) throws OpenemsNamedException;

	/**
	 * Gets the USB configuration.
	 *
	 * @return the original configuration in textual form
	 * @throws OpenemsNamedException on error
	 */
	public String getUsbConfiguration() throws OpenemsNamedException;

	/**
	 * Executes a command.
	 *
	 * @param request the ExecuteCommandRequest
	 * @return a ExecuteCommandResponse
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteCommandRequest(
			ExecuteSystemCommandRequest request) throws OpenemsNamedException;

}
