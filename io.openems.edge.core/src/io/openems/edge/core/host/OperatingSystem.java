package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartResponse;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig;

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
			SetNetworkConfig.Request request) throws OpenemsNamedException;

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
	 * @param request the {@link ExecuteSystemCommandRequest}
	 * @return a {@link ExecuteSystemCommandResponse}
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteSystemCommandRequest(
			ExecuteSystemCommandRequest request) throws OpenemsNamedException;

	/**
	 * Executes a system restart (soft or hard).
	 * 
	 * @param request the {@link ExecuteSystemRestartRequest}
	 * @return an {@link ExecuteSystemRestartResponse}
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleExecuteSystemRestartRequest(
			ExecuteSystemRestartRequest request) throws NotImplementedException;

	/**
	 * Gets the System IPs.
	 * 
	 * @return a list of all ips of the system
	 * @throws OpenemsNamedException on error
	 */
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException;

	/**
	 * Gets Network Info.
	 * 
	 * @return Response of GetIpAddresses
	 * @throws OpenemsNamedException on error
	 */
	public GetNetworkInfo.Response getNetworkInfo() throws OpenemsNamedException;

	/**
	 * Gets the current operating system version.
	 * 
	 * @return a future with the result
	 */
	public CompletableFuture<String> getOperatingSystemVersion();

	/**
	 * Returns the {@link Updateable} to update the current operating system.
	 * 
	 * @return the {@link Updateable} for the current operating system or null if
	 *         not implemented
	 */
	public Updateable getSystemUpdateable();

	/**
	 * Deletes network interface configuration files.
	 * 
	 * @param user           the user performing the operation
	 * @param interfaceNames the list of interface names to delete
	 * @throws OpenemsNamedException on error
	 */
	public void deleteNetworkInterfaces(User user, List<String> interfaceNames) throws OpenemsNamedException;

	/**
	 * Returns the current cpu temperature in Celsius Degrees. Can throw an
	 * exception if the reading fails or can return Optional.empty() if there is no
	 * way to read a temperature.
	 *
	 * @return CPU temperature in Celsius Degrees.
	 */
	public Optional<Double> getCpuTemperature();

	/**
	 * Returns the "recent cpu usage". This value is a double in the [0.0,1.0]
	 * interval. A value of 0.0 means that all CPUs were idle during the recent
	 * period of time observed, while a value of 1.0 means that all CPUs were
	 * actively running 100% of the time during the recent period being observed.
	 * 
	 * <p>
	 * Currently, the load average is only available in linux operating systems.
	 * 
	 * @return Returns the load average as OptionalDouble or an empty optional if
	 *         there is no data or the reading is not supported.
	 */
	public Optional<Double> getCpuLoad();

	/**
	 * Returns the available and total memory of the operating system.
	 * 
	 * @return Class with memory informations.
	 */
	public Optional<MemoryInformation> getSystemMemory();

	record MemoryInformation(long availableMemoryInKBytes, long totalMemoryInKBytes) {
	}

}
