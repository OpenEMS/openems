package io.openems.edge.core.host;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import com.sun.management.OperatingSystemMXBean;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig;

public class OperatingSystemMac implements OperatingSystem {

	@Override
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException {
		return new NetworkConfiguration(new TreeMap<>());
	}

	@Override
	public void handleSetNetworkConfigRequest(User user, NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfig.Request request) throws OpenemsNamedException {
		throw new NotImplementedException("SetNetworkConfigRequest is not implemented for Mac");
	}

	@Override
	public String getUsbConfiguration() throws OpenemsNamedException {
		// not implemented
		return "";
	}

	@Override
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteSystemCommandRequest(
			ExecuteSystemCommandRequest request) throws OpenemsNamedException {
		throw new NotImplementedException("ExecuteSystemCommandRequest is not implemented for Mac");
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleExecuteSystemRestartRequest(
			ExecuteSystemRestartRequest request) throws NotImplementedException {
		throw new NotImplementedException("ExecuteSystemRestartRequest is not implemented for Mac");
	}

	@Override
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException {
		return Collections.emptyList();
	}

	@Override
	public CompletableFuture<String> getOperatingSystemVersion() {
		return CompletableFuture.completedFuture(System.getProperty("os.name"));
	}

	@Override
	public GetNetworkInfo.Response getNetworkInfo() throws OpenemsNamedException {
		throw new NotImplementedException("This request is not implemented for mac");
	}

	@Override
	public Updateable getSystemUpdateable() {
		return null;
	}

	@Override
	public void deleteNetworkInterfaces(User user, List<String> interfaceNames) throws OpenemsNamedException {
		throw new NotImplementedException("deleteNetworkInterfaces is not implemented for Mac");
	}

	@Override
	public Optional<Double> getCpuTemperature() {
		return Optional.empty();
	}

	@Override
	public Optional<Double> getCpuLoad() {
		var bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		var load = bean.getCpuLoad();
		return load < 0 ? Optional.empty() : Optional.of(load);
	}

	@Override
	public Optional<MemoryInformation> getSystemMemory() {
		return Optional.empty();
	}

}
