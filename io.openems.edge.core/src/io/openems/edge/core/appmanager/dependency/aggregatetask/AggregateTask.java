package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static java.util.Collections.emptySet;

import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;

public interface AggregateTask<T> {

	/**
	 * Class representing the configuration of an already aggregated
	 * {@link AggregateTask}.
	 */
	public interface AggregateTaskExecutionConfiguration {

		/**
		 * The identifier of the configuration.
		 * 
		 * @return a string which identifies this type of configuration
		 */
		public String identifier();

		/**
		 * Creates a {@link JsonElement} of this configuration.
		 * 
		 * @return the created {@link JsonElement}
		 */
		public JsonElement toJson();

	}

	public static record AggregateTaskExecuteConstraints(//
			/**
			 * Tasks which need to run before this task.
			 */
			Set<Class<? extends AggregateTask<?>>> runAfter //
	) {

	}

	/**
	 * Aggregates the given instance.
	 *
	 * @param currentConfiguration the {@link AppConfiguration} of the instance
	 * @param lastConfiguration    the old configuration of the instance
	 */
	public void aggregate(T currentConfiguration, T lastConfiguration);

	/**
	 * e. g. creates components that were aggregated by the instances and my also
	 * delete unused components.
	 *
	 * @param user                   the executing user
	 * @param otherAppConfigurations the other existing {@link AppConfiguration}s
	 * @throws OpenemsNamedException on error
	 */
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException;

	/**
	 * e. g. deletes components that were aggregated.
	 *
	 * @param user                   the executing user
	 * @param otherAppConfigurations the other existing {@link AppConfiguration}s
	 * @throws OpenemsNamedException on error
	 */
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException;

	/**
	 * Gets the {@link AggregateTaskExecutionConfiguration} which can be used for
	 * debugging.
	 * 
	 * @return the AggregateTaskExecutionConfiguration
	 */
	public AggregateTaskExecutionConfiguration getExecutionConfiguration();

	/**
	 * Validates the expected configuration.
	 * 
	 * @param errors           the errors that occur during the validation
	 * @param appConfiguration the whole configuration
	 * @param config           the configuration to validate
	 */
	public void validate(List<String> errors, AppConfiguration appConfiguration, T config);

	/**
	 * Gets a general message for the user if any operations fails.
	 * 
	 * @param l the {@link Language} of the message
	 * @return the error message
	 */
	public String getGeneralFailMessage(Language l);

	public default AggregateTaskExecuteConstraints getExecuteConstraints() {
		return new AggregateTaskExecuteConstraints(emptySet());
	}

	/**
	 * Resets the task.
	 */
	public void reset();

}
