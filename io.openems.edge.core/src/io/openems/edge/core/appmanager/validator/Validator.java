package io.openems.edge.core.appmanager.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.ConfigurationTarget;

public class Validator {

	private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

	private final List<CheckableConfig> compatibleCheckableConfigs;
	private final List<CheckableConfig> installableCheckableConfigs;

	private ThrowingBiFunction<ConfigurationTarget, //
			JsonObject, //
			Map<String, Map<String, ?>>, //
			OpenemsNamedException> //
	configurationValidation;

	public static final class Builder {

		private List<CheckableConfig> compatibleCheckableConfigs;
		private List<CheckableConfig> installableCheckableConfigs;

		protected Builder() {

		}

		public Builder setCompatibleCheckableConfigs(List<CheckableConfig> compatibleCheckableConfigs) {
			this.compatibleCheckableConfigs = compatibleCheckableConfigs;
			return this;
		}

		public Builder setInstallableCheckableConfigs(List<CheckableConfig> installableCheckableConfigs) {
			this.installableCheckableConfigs = installableCheckableConfigs;
			return this;
		}

		public Validator build() {
			return new Validator(this.compatibleCheckableConfigs, this.installableCheckableConfigs);
		}

	}

	// TODO convert to record in java 17.
	public static final class CheckableConfig {

		private final String checkableComponentName;
		private final boolean invertResult;
		private final Map<String, ?> properties;

		public CheckableConfig(String checkableComponentName, boolean invertResult, Map<String, ?> properties) {
			this.checkableComponentName = checkableComponentName;
			this.invertResult = invertResult;
			this.properties = properties;
		}

		public CheckableConfig(String checkableComponentName, Map<String, ?> properties) {
			this(checkableComponentName, false, properties);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}
			var other = (CheckableConfig) obj;
			return java.util.Objects.equals(this.checkableComponentName, other.checkableComponentName)
					&& this.invertResult == other.invertResult;
		}

	}

	public static final class MapBuilder<T extends Map<K, V>, K, V> {

		private final T map;

		public MapBuilder(T mapImpl) {
			this.map = mapImpl;
		}

		/**
		 * Does the exact same like {@link Map#put(Object, Object)}.
		 *
		 * @param key   the key
		 * @param value the value
		 * @return this
		 */
		public MapBuilder<T, K, V> put(K key, V value) {
			this.map.put(key, value);
			return this;
		}

		public T build() {
			return this.map;
		}
	}

	/**
	 * Creates a builder for an {@link Validator}.
	 *
	 * @return the builder
	 */
	public static final Builder create() {
		return new Builder();
	}

	protected Validator(List<CheckableConfig> compatibleCheckableConfigs,
			List<CheckableConfig> installableCheckableConfigs) {
		this.compatibleCheckableConfigs = compatibleCheckableConfigs != null //
				? compatibleCheckableConfigs
				: Lists.newArrayList();
		this.installableCheckableConfigs = installableCheckableConfigs != null //
				? installableCheckableConfigs
				: Lists.newArrayList();
	}

	/**
	 * Gets the error messages for compatibility.
	 *
	 * @return the error messages
	 */
	public List<String> getErrorCompatibleMessages() {
		return getErrorMessages(this.compatibleCheckableConfigs, false);
	}

	/**
	 * Gets the error messages for the given {@link Checkable}.
	 *
	 * @param checkableConfigs the {@link Checkable}s to be checked.
	 * @param returnImmediate  after the first checkable who returns false
	 * @return a list of errors
	 */
	private static List<String> getErrorMessages(List<CheckableConfig> checkableConfigs, boolean returnImmediate) {
		if (checkableConfigs.isEmpty()) {
			return new ArrayList<>();
		}
		var errorMessages = new ArrayList<String>(checkableConfigs.size());
		var bundleContext = FrameworkUtil.getBundle(Checkable.class).getBundleContext();
		// build filter
		var filterBuilder = new StringBuilder();
		if (checkableConfigs.size() > 1) {
			filterBuilder.append("(|");
		}
		checkableConfigs.forEach(t -> filterBuilder.append("(component.name=" + t.checkableComponentName + ")"));
		if (checkableConfigs.size() > 1) {
			filterBuilder.append(")");
		}
		try {
			// get all service references
			Collection<ServiceReference<Checkable>> serviceReferences = bundleContext
					.getServiceReferences(Checkable.class, filterBuilder.toString());
			var noneExistingCheckables = Lists.<CheckableConfig>newArrayList();
			checkableConfigs.forEach(c -> noneExistingCheckables.add(c));
			var isReturnedImmediate = false;
			for (var reference : serviceReferences) {
				var componentName = (String) reference.getProperty(OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME);
				var checkableConfig = checkableConfigs.stream()
						.filter(c -> c.checkableComponentName.equals(componentName)).findFirst().orElse(null);
				var checkable = bundleContext.getService(reference);
				if (checkableConfig.properties != null) {
					checkable.setProperties(checkableConfig.properties);
				}
				noneExistingCheckables.removeIf(c -> c.equals(checkableConfig));
				var result = checkable.check();
				if (result == checkableConfig.invertResult) {
					var errorMessage = checkable.getErrorMessage();
					if (checkableConfig.invertResult) {
						errorMessage = "Invert[" + errorMessage + "]";
					}
					errorMessages.add(errorMessage);
					if (returnImmediate) {
						isReturnedImmediate = true;
						break;
					}
				}
			}

			if (!noneExistingCheckables.isEmpty() && !isReturnedImmediate) {
				LOG.warn("Checkables[" + noneExistingCheckables.stream().map(c -> c.checkableComponentName)
						.collect(Collectors.joining(";")) + "] are not found!");
			}

			// free all service references
			for (var reference : serviceReferences) {
				bundleContext.ungetService(reference);
			}
		} catch (InvalidSyntaxException | IllegalStateException e) {
			// Can not get service references
			e.printStackTrace();
		}
		return errorMessages;
	}

	/**
	 * Gets the error messages for installation.
	 *
	 * @return the error messages
	 */
	public List<String> getErrorInstallableMessages() {
		return getErrorMessages(this.installableCheckableConfigs, false);
	}

	/**
	 * Validates the {@link Checkable}s and gets the Status.
	 *
	 * @return the Status
	 */
	public OpenemsAppStatus getStatus() {
		if (!getErrorMessages(this.compatibleCheckableConfigs, true).isEmpty()) {
			return OpenemsAppStatus.INCOMPATIBLE;
		}
		if (!getErrorMessages(this.installableCheckableConfigs, true).isEmpty()) {
			return OpenemsAppStatus.COMPATIBLE;
		}
		return OpenemsAppStatus.INSTALLABLE;
	}

	public void setConfigurationValidation(ThrowingBiFunction<ConfigurationTarget, //
			JsonObject, //
			Map<String, Map<String, ?>>, OpenemsNamedException> configurationValidation) {
		this.configurationValidation = configurationValidation;
	}

	/**
	 * Builds a {@link JsonObject} out of this {@link Validator}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		var compatibleMessages = JsonUtils.buildJsonArray().build();
		for (var message : this.getErrorCompatibleMessages()) {
			compatibleMessages.add(message);
		}
		var installableMessages = JsonUtils.buildJsonArray().build();
		for (var message : this.getErrorInstallableMessages()) {
			installableMessages.add(message);
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.getStatus().name()) //
				.add("errorCompatibleMessages", compatibleMessages) //
				.add("errorInstallableMessages", installableMessages) //
				.build();
	}

	/**
	 * Validates the Configuration {@link Checkable}s.
	 *
	 * @param target     the target of the configuration
	 * @param properties the configuration properties
	 * @throws OpenemsNamedException on validation error
	 */
	public void validateConfiguration(ConfigurationTarget target, JsonObject properties) throws OpenemsNamedException {
		if (this.configurationValidation == null) {
			return;
		}
		var checkables = this.configurationValidation.apply(target, properties);
		if (checkables == null) {
			return;
		}
		var errors = getErrorMessages(this.compatibleCheckableConfigs, false);
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining(";")));
		}
	}

	public List<CheckableConfig> getCompatibleCheckableConfigs() {
		return this.compatibleCheckableConfigs;
	}

	public List<CheckableConfig> getInstallableCheckableConfigs() {
		return this.installableCheckableConfigs;
	}

}
