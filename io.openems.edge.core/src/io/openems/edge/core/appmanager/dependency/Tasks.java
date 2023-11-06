package io.openems.edge.core.appmanager.dependency;

import java.util.List;
import java.util.Set;

import io.openems.common.types.EdgeConfig;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.dependency.aggregatetask.AggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentConfiguration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.PersistencePredictorAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.PersistencePredictorConfiguration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerConfiguration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.StaticIpAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.StaticIpConfiguration;

public class Tasks {

	/**
	 * Creates a {@link Task} for setting the {@link ComponentConfiguration}.
	 * 
	 * @param components the components to create or update
	 * @return the {@link Task}
	 */
	public static Task<ComponentConfiguration> component(List<EdgeConfig.Component> components) {
		return createTask(ComponentAggregateTask.class, new ComponentConfiguration(components));
	}

	/**
	 * Creates a {@link Task} for setting the {@link ComponentConfiguration}.
	 * 
	 * @param components the components to create or update
	 * @return the {@link Task}
	 */
	public static Task<ComponentConfiguration> component(EdgeConfig.Component... components) {
		return createTask(ComponentAggregateTask.class, new ComponentConfiguration(components));
	}

	/**
	 * Creates a {@link Task} for setting the {@link StaticIpConfiguration}.
	 * 
	 * @param interfaceConfiguration the {@link InterfaceConfiguration} to set
	 * @return the {@link Task}
	 */
	public static Task<StaticIpConfiguration> staticIp(List<InterfaceConfiguration> interfaceConfiguration) {
		return createTask(StaticIpAggregateTask.class, new StaticIpConfiguration(interfaceConfiguration));
	}

	/**
	 * Creates a {@link Task} for setting the {@link StaticIpConfiguration}.
	 * 
	 * @param interfaceConfiguration the {@link InterfaceConfiguration} to set
	 * @return the {@link Task}
	 */
	public static Task<StaticIpConfiguration> staticIp(InterfaceConfiguration... interfaceConfiguration) {
		return createTask(StaticIpAggregateTask.class, new StaticIpConfiguration(interfaceConfiguration));
	}

	/**
	 * Creates a Task for setting the {@link SchedulerConfiguration}.
	 * 
	 * @param componentOrder the order of the components in the scheduler
	 * @return the {@link Task} to run when creating the {@link OpenemsAppInstance}
	 */
	public static Task<SchedulerConfiguration> scheduler(List<String> componentOrder) {
		return createTask(SchedulerAggregateTask.class, new SchedulerConfiguration(componentOrder));
	}

	/**
	 * Creates a Task for setting the {@link SchedulerConfiguration}.
	 * 
	 * @param componentOrder the order of the components in the scheduler
	 * @return the {@link Task} to run when creating the {@link OpenemsAppInstance}
	 */
	public static Task<SchedulerConfiguration> scheduler(String... componentOrder) {
		return createTask(SchedulerAggregateTask.class, new SchedulerConfiguration(componentOrder));
	}

	/**
	 * Creates a Task for setting the {@link PersistencePredictorConfiguration}.
	 * 
	 * @param channels the channels that need to be in the predictor
	 * @return the {@link Task} to run when creating the {@link OpenemsAppInstance}
	 */
	public static Task<PersistencePredictorConfiguration> persistencePredictor(Set<String> channels) {
		return createTask(PersistencePredictorAggregateTask.class, new PersistencePredictorConfiguration(channels));
	}

	/**
	 * Creates a Task for setting the {@link PersistencePredictorConfiguration}.
	 * 
	 * @param channels the channels that need to be in the predictor
	 * @return the {@link Task} to run when creating the {@link OpenemsAppInstance}
	 */
	public static Task<PersistencePredictorConfiguration> persistencePredictor(String... channels) {
		return createTask(PersistencePredictorAggregateTask.class, new PersistencePredictorConfiguration(channels));
	}

	private static <C, T extends AggregateTask<C>> Task<C> createTask(Class<T> clazz, C configuration) {
		return new Task<>(//
				clazz, //
				configuration //
		);
	}

}
