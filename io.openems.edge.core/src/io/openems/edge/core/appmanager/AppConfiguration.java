package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.List;

import io.openems.common.types.EdgeConfig.Component;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

public class AppConfiguration {
	// the components the app needs
	public final List<Component> components;
	// the execute order in the scheduler of the components
	public final List<String> schedulerExecutionOrder;
	// the static ips in the Network configuration to access different networks
	public final List<InterfaceConfiguration> ips;

	public final List<DependencyDeclaration> dependencies;

	public AppConfiguration() {
		this(null);
	}

	public AppConfiguration(List<Component> components) {
		this(components, null);
	}

	public AppConfiguration(List<Component> components, List<String> schedulerExecutionOrder) {
		this(components, schedulerExecutionOrder, null);
	}

	public AppConfiguration(List<Component> components, List<String> schedulerExecutionOrder,
			List<InterfaceConfiguration> ips) {
		this(components, schedulerExecutionOrder, ips, null);
	}

	public AppConfiguration(List<Component> components, List<String> schedulerExecutionOrder,
			List<InterfaceConfiguration> ips, List<DependencyDeclaration> dependencies) {
		this.components = components != null ? components : new ArrayList<>();
		this.schedulerExecutionOrder = schedulerExecutionOrder != null ? schedulerExecutionOrder : new ArrayList<>();
		this.ips = ips != null ? ips : new ArrayList<>();
		this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
	}
}
