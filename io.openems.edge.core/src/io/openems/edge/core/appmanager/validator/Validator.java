package io.openems.edge.core.appmanager.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.ConfigurationTarget;

public class Validator {

	private static final Logger LOG = Logger.getLogger(Validator.class.getName());

	private final Map<String, Map<String, ?>> compatibleCheckableNames;
	private final Map<String, Map<String, ?>> installableCheckableNames;

	private ThrowingBiFunction<ConfigurationTarget, //
			JsonObject, //
			Map<String, Map<String, ?>>, //
			OpenemsNamedException> //
	configurationValidation;

	public static final class Builder {

		private Map<String, Map<String, ?>> compatibleCheckableNames;
		private Map<String, Map<String, ?>> installableCheckableNames;

		protected Builder() {

		}

		public Builder setCompatibleCheckableNames(Map<String, Map<String, ?>> compatibleCheckableNames) {
			this.compatibleCheckableNames = compatibleCheckableNames;
			return this;
		}

		public Builder setInstallableCheckableNames(Map<String, Map<String, ?>> installableCheckableNames) {
			this.installableCheckableNames = installableCheckableNames;
			return this;
		}

		public Validator build() {
			return new Validator(this.compatibleCheckableNames, this.installableCheckableNames);
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

	protected Validator(Map<String, Map<String, ?>> compatibleCheckableNames,
			Map<String, Map<String, ?>> installableCheckableNames) {
		this.compatibleCheckableNames = compatibleCheckableNames != null //
				? compatibleCheckableNames
				: new HashMap<>();
		this.installableCheckableNames = installableCheckableNames != null //
				? installableCheckableNames
				: new HashMap<>();

	}

	/**
	 * Gets the error messages for compatibility.
	 *
	 * @return the error messages
	 */
	public List<String> getErrorCompatibleMessages() {
		return getErrorMessages(this.compatibleCheckableNames, false);
	}

	/**
	 * Gets the error messages for the given {@link Checkable}.
	 *
	 * @param checkableNames  the {@link Checkable} to be checked.
	 * @param returnImmediate after the first checkable who returns false
	 * @return a list of errors
	 */
	private static List<String> getErrorMessages(Map<String, Map<String, ?>> checkableNames, boolean returnImmediate) {
		if (checkableNames == null || checkableNames.isEmpty()) {
			return new ArrayList<>();
		}
		var errorMessages = new ArrayList<String>(checkableNames.size());
		var bundleContext = FrameworkUtil.getBundle(Checkable.class).getBundleContext();
		// build filter
		var filterBuilder = new StringBuilder();
		if (checkableNames.size() > 1) {
			filterBuilder.append("(|");
		}
		checkableNames.entrySet().forEach(t -> filterBuilder.append("(component.name=" + t.getKey() + ")"));
		if (checkableNames.size() > 1) {
			filterBuilder.append(")");
		}
		try {
			// get all service references
			Collection<ServiceReference<Checkable>> serviceReferences = bundleContext
					.getServiceReferences(Checkable.class, filterBuilder.toString());
			var noneExistingCheckables = Lists.<String>newArrayList();
			checkableNames.forEach((t, u) -> noneExistingCheckables.add(t));
			var isReturnedImmediate = false;
			for (var reference : serviceReferences) {
				var componentName = (String) reference.getProperty(OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME);
				var properties = checkableNames.get(componentName);
				var checkable = bundleContext.getService(reference);
				if (properties != null) {
					checkable.setProperties(properties);
				}
				noneExistingCheckables.remove(componentName);
				if (!checkable.check()) {
					errorMessages.add(checkable.getErrorMessage());
					if (returnImmediate) {
						isReturnedImmediate = true;
						break;
					}
				}
			}

			if (!noneExistingCheckables.isEmpty() && !isReturnedImmediate) {
				LOG.log(Level.WARNING, "Checkables[" + noneExistingCheckables.stream().collect(Collectors.joining(";"))
						+ "] are not found!");
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
		return getErrorMessages(this.installableCheckableNames, false);
	}

	/**
	 * Validates the {@link Checkable}s and gets the Status.
	 *
	 * @return the Status
	 */
	public OpenemsAppStatus getStatus() {
		if (!getErrorMessages(this.compatibleCheckableNames, true).isEmpty()) {
			return OpenemsAppStatus.INCOMPATIBLE;
		}
		if (!getErrorMessages(this.installableCheckableNames, true).isEmpty()) {
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
		var errors = getErrorMessages(this.compatibleCheckableNames, false);
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining(";")));
		}
	}

	public Map<String, Map<String, ?>> getCompatibleCheckableNames() {
		return this.compatibleCheckableNames;
	}

	public Map<String, Map<String, ?>> getInstallableCheckableNames() {
		return this.installableCheckableNames;
	}

}
