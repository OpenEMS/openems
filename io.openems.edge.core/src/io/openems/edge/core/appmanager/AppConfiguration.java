package io.openems.edge.core.appmanager;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import io.openems.common.types.EdgeConfig;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.Task;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.AggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentConfiguration;

public record AppConfiguration(List<Task<?>> tasks, List<DependencyDeclaration> dependencies) {

	public static final class AppConfigurationBuilder implements Self<AppConfigurationBuilder>,
			OnlyIf<AppConfigurationBuilder>, ThrowingOnlyIf<AppConfigurationBuilder> {

		public final List<Task<?>> tasks = new ArrayList<>();
		public final List<DependencyDeclaration> dependencies = new ArrayList<>();

		private AppConfigurationBuilder() {
			super();
		}

		/**
		 * Adds a {@link Task} to the configuration.
		 * 
		 * <p>
		 * Create tasks with the static methods in the {@link Tasks} class
		 * 
		 * @param task the task to add
		 * @return this
		 */
		public AppConfigurationBuilder addTask(Task<?> task) {
			this.tasks.add(task);
			return this;
		}

		/**
		 * Adds a single dependency to the {@link AppConfiguration}.
		 * 
		 * @param dependencyDeclaration the dependency to add
		 * @return this
		 */
		public AppConfigurationBuilder addDependency(DependencyDeclaration dependencyDeclaration) {
			this.dependencies.add(dependencyDeclaration);
			return this;
		}

		/**
		 * Adds the dependencies to the {@link AppConfiguration}.
		 * 
		 * @param dependencyDeclaration the dependencies to add
		 * @return this
		 */
		public AppConfigurationBuilder addDependencies(DependencyDeclaration... dependencyDeclaration) {
			Stream.of(dependencyDeclaration).forEach(this.dependencies::add);
			return this;
		}

		/**
		 * Adds the dependencies to the {@link AppConfiguration}.
		 * 
		 * @param dependencyDeclaration the dependencies to add
		 * @return this
		 */
		public AppConfigurationBuilder addDependencies(Collection<DependencyDeclaration> dependencyDeclaration) {
			this.dependencies.addAll(dependencyDeclaration);
			return this;
		}

		public final AppConfiguration build() {
			return new AppConfiguration(//
					Collections.unmodifiableList(this.tasks), //
					Collections.unmodifiableList(this.dependencies) //
			);
		}

		@Override
		public AppConfigurationBuilder self() {
			return this;
		}

	}

	/**
	 * Creates a builder for creating an {@link AppConfiguration}.
	 * 
	 * @return the builder
	 */
	public static AppConfigurationBuilder create() {
		return new AppConfigurationBuilder();
	}

	/**
	 * Creates an empty {@link AppConfiguration}.
	 * 
	 * @return the configuration
	 */
	public static AppConfiguration empty() {
		return create().build();
	}

	public List<EdgeConfig.Component> getComponents() {
		return this.map(ComponentAggregateTask.class, ComponentConfiguration::components) //
				.orElse(emptyList());
	}

	/**
	 * Gets the configuration for the given task class.
	 * 
	 * <p>
	 * e. g. if {@link ComponentAggregateTask} is given which is a
	 * {@link AggregateTask} of {@link ComponentConfiguration} then this
	 * configuration is returned if defined in the {@link AppConfiguration} else
	 * null.
	 * 
	 * @param <C>   the type of the configuration
	 * @param <T>   the type of the {@link AggregateTask}
	 * @param clazz the {@link Class} of the {@link AggregateTask}
	 * @return the found configuration or null if not defined
	 */
	@SuppressWarnings("unchecked")
	public <C, T extends AggregateTask<C>> C getConfiguration(Class<T> clazz) {
		return (C) this.tasks.stream() //
				.filter(t -> t.aggregateTaskClass().isAssignableFrom(clazz)) //
				.findAny() //
				.map(Task::configuration) //
				.orElse(null);
	}

	private <C, T extends AggregateTask<C>, L> Optional<L> map(//
			final Class<T> clazz, //
			final Function<C, L> mapper //
	) {
		return Optional.ofNullable(this.getConfiguration(clazz)) //
				.map(mapper); //
	}

	/**
	 * Flat maps a attribute from a configuration to a single list.
	 * 
	 * @param <C>     the type of the Configuration
	 * @param <T>     the type of the {@link AggregateTask}
	 * @param <L>     the type of the list
	 * @param configs the configurations to map
	 * @param clazz   the {@link Class} of the Task
	 * @param mapper  the mapper from the configuration to the type of the list
	 * @return the list with all instances
	 */
	public static <C, T extends AggregateTask<C>, L> Stream<L> flatMap(//
			final List<AppConfiguration> configs, //
			final Class<T> clazz, //
			final Function<C, Collection<L>> mapper //
	) {
		return Optional.ofNullable(configs) //
				.map(c -> {
					return c.stream() //
							.map(t -> t.getConfiguration(clazz)) //
							.filter(Objects::nonNull) //
							.map(mapper) //
							.flatMap(Collection::stream);
				}).orElse(Stream.empty());
	}

	/**
	 * Collects all components from the {@link AppConfiguration AppConfigurations}
	 * and returns them.
	 * 
	 * @param configs the {@link AppConfiguration AppConfigurations} to get the
	 *                {@link EdgeConfig.Component Components} from
	 * @return the {@link EdgeConfig.Component Components}
	 */
	public static List<EdgeConfig.Component> getComponentsFromConfigs(List<AppConfiguration> configs) {
		return flatMap(configs, ComponentAggregateTask.class, ComponentConfiguration::components).toList();
	}

}
