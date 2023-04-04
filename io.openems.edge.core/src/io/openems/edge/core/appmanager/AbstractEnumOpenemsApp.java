package io.openems.edge.core.appmanager;

import java.util.EnumMap;
import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;

public abstract class AbstractEnumOpenemsApp<PROPERTY extends Enum<PROPERTY> & Nameable>
		extends AbstractOpenemsApp<PROPERTY> {

	protected AbstractEnumOpenemsApp(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	protected abstract ThrowingTriFunction<ConfigurationTarget, //
			EnumMap<PROPERTY, JsonElement>, //
			Language, //
			AppConfiguration, //
			OpenemsNamedException> appConfigurationFactory();

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<PROPERTY, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, m, l) -> {
			final var map = new EnumMap<PROPERTY, JsonElement>(this.getPropertyClass());
			m.forEach((k, v) -> map.put(k, v));
			return this.appConfigurationFactory().apply(t, map, l);
		};
	}

	@Override
	protected PROPERTY[] propertyValues() {
		return this.getPropertyClass().getEnumConstants();
	}

	/**
	 * Gets the id of the map with the given DefaultEnum
	 *
	 * <p>
	 * e. g. defaultValue: "ess0" => the next available id with the base-name "ess"
	 * and the the next available number
	 *
	 * @param t   the configuration target
	 * @param map the configuration map
	 * @param key the key to be searched for
	 * @return the found id
	 */
	protected String getId(ConfigurationTarget t, Map<PROPERTY, JsonElement> map, DefaultEnum key) {
		try {
			return this.getId(t, map, Enum.valueOf(this.getPropertyClass(), key.name()), key.getDefaultValue());
		} catch (IllegalArgumentException ex) {
			// not a enum of property
		}
		return key.getDefaultValue();
	}

	/**
	 * Gets the value of the property name in the map or the defaulValue if the
	 * property was not found.
	 *
	 * @param map      the configuration map
	 * @param property the property to be searched for
	 * @return the String value
	 */
	protected String getValueOrDefault(EnumMap<PROPERTY, JsonElement> map, DefaultEnum property) {
		var key = Enum.valueOf(this.getPropertyClass(), property.name());
		return this.getValueOrDefault(map, key, property.getDefaultValue());
	}

	protected abstract Class<PROPERTY> getPropertyClass();

}
