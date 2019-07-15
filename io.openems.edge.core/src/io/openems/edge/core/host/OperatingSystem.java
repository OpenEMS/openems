package io.openems.edge.core.host;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

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
	 * @param oldNetworkConfiguration the current/old network configuration
	 * @param request                 the SetNetworkConfigRequest
	 * @throws OpenemsNamedException on error
	 */
	public void handleSetNetworkConfigRequest(NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfigRequest request) throws OpenemsNamedException;

	/**
	 * Executes a command.
	 * 
	 * @param password       the system user password
	 * @param command        the command
	 * @param background     run the command in background (true) or in foreground
	 *                       (false)
	 * @param timeoutSeconds interrupt the command after ... seconds
	 * @return the output of the command
	 * @throws OpenemsException on error
	 */
	public String executeCommand(String password, String command, boolean background, int timeoutSeconds)
			throws OpenemsException;

}
