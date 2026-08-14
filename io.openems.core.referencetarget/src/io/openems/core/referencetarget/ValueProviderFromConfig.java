package io.openems.core.referencetarget;

import static io.openems.common.utils.FunctionUtils.lazySingleton;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueProviderFromConfig implements ValueProvider {

	private final Logger log = LoggerFactory.getLogger(ValueProviderFromConfig.class);

	private final Dictionary<String, Object> properties;
	private final Supplier<Map<String, Object>> defaultValues;

	public ValueProviderFromConfig(Dictionary<String, Object> properties, Supplier<ObjectClassDefinition> ocd) {
		this.properties = properties;
		this.defaultValues = lazySingleton(() -> getDefaultValues(ocd.get()));
	}

	@Override
	public Object getValue(String variable) {
		final var propertyName = variable.replace("_", ".");
		final var configValue = this.properties.get(propertyName);
		if (configValue != null) {
			return configValue;
		}

		final var defaultValue = this.defaultValues.get().get(propertyName);
		this.log.info("Using default value for {}: {}", propertyName, defaultValue);
		return defaultValue;
	}

	private static Map<String, Object> getDefaultValues(ObjectClassDefinition ocd) {
		if (ocd == null) {
			return emptyMap();
		}

		final var attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		return Arrays.stream(attributes) //
				.map(t -> {
					final var defaultValue = t.getDefaultValue();
					if (defaultValue == null || defaultValue.length == 0) {
						return null;
					}
					return Map.entry(t.getID(), defaultValue.length == 1 ? defaultValue[0] : defaultValue);
				}) //
				.filter(Objects::nonNull) //
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

}
