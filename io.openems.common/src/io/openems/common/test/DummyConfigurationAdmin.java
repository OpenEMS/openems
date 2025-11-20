package io.openems.common.test;

import static io.openems.common.utils.DictionaryUtils.getAsBoolean;
import static io.openems.common.utils.DictionaryUtils.getAsString;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Simulates a ConfigurationAdmin for the OpenEMS Component test framework.
 */
public class DummyConfigurationAdmin implements ConfigurationAdmin {

	public static class DummyConfiguration implements Configuration {

		private final Hashtable<String, Object> properties = new Hashtable<>();
		private int changeCount = 0;

		@Override
		public String getPid() {
			return "";
		}

		@Override
		public Dictionary<String, Object> getProperties() {
			return this.properties;
		}

		/**
		 * Adds a configuration property to this {@link DummyConfiguration}.
		 *
		 * @param key   the property key
		 * @param value the property value
		 * @return myself
		 */
		public DummyConfiguration addProperty(String key, Object value) {
			this.properties.put(key, value);
			return this;
		}

		@Override
		public void update(Dictionary<String, ?> properties) throws IOException {
			var keys = properties.keys();
			while (keys.hasMoreElements()) {
				var key = keys.nextElement();
				Object value = properties.get(key);
				this.properties.put(key, value);
			}
			this.changeCount++;
		}

		@Override
		public void update() throws IOException {
			this.changeCount++;
		}

		@Override
		public String getFactoryPid() {
			return "";
		}

		@Override
		public void setBundleLocation(String location) {
		}

		@Override
		public String getBundleLocation() {
			return "?";
		}

		@Override
		public long getChangeCount() {
			return this.changeCount;
		}

		@Override
		public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> reference) {
			return this.properties;
		}

		@Override
		public boolean updateIfDifferent(Dictionary<String, ?> properties) throws IOException {
			return false;
		}

		@Override
		public void addAttributes(ConfigurationAttribute... attrs) throws IOException {
		}

		@Override
		public Set<ConfigurationAttribute> getAttributes() {
			return new HashSet<>();
		}

		@Override
		public void removeAttributes(ConfigurationAttribute... attrs) throws IOException {
		}

		@Override
		public void delete() throws IOException {
			this.changeCount++;
		}
	}

	private final Map<String, DummyConfiguration> configurations = new HashMap<>();

	@Override
	public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
		return this.getOrCreateEmptyConfiguration(factoryPid);
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
		return this.getOrCreateEmptyConfiguration(factoryPid);
	}

	@Override
	public Configuration getConfiguration(String pid) throws IOException {
		return this.getOrCreateEmptyConfiguration(pid);
	}

	@Override
	public Configuration getConfiguration(String pid, String location) throws IOException {
		return this.getOrCreateEmptyConfiguration(pid);
	}

	private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("\\((?<key>\\S+)=(?<value>\\S+)\\)");

	@Override
	public synchronized Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
		// org.apache.felix.cm.impl.Simplefilter is not available here, so we apply just
		// a simple parsing. See
		// https://github.com/apache/felix-dev/blob/master/configadmin/src/main/java/org/apache/felix/cm/impl/SimpleFilter.java
		return this.configurations.values().stream() //
				.filter(c -> {
					if (filter == null) {
						return true;
					}
					var matcher = KEY_VALUE_PATTERN.matcher(filter);
					if (!matcher.find()) {
						return true;
					}
					final var props = c.getProperties();
					var key = matcher.group("key");
					var value = matcher.group("value");
					if (value.equalsIgnoreCase("true") && getAsBoolean(props, key) == TRUE) {
						return true;
					} else if (value.equalsIgnoreCase("false") && getAsBoolean(props, key) == FALSE) {
						return true;
					} else if (value.equals(getAsString(props, key))) {
						return true;
					}
					return false;
				}) //
				.map(c -> (Configuration) c) //
				.toArray(Configuration[]::new);
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name, String location) throws IOException {
		return this.configurations.get(factoryPid);
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name) throws IOException {
		return this.configurations.get(factoryPid);
	}

	/**
	 * Gets a {@link DummyConfiguration} by id or creates a new empty
	 * {@link DummyConfiguration} for the given id.
	 *
	 * @param id the given id
	 * @return the {@link DummyConfiguration}
	 */
	public synchronized DummyConfiguration getOrCreateEmptyConfiguration(String id) {
		return this.configurations.computeIfAbsent(id, ignore -> new DummyConfiguration());
	}

	/**
	 * Adds a simulated {@link AbstractComponentConfig} with all its properties to
	 * the configurations.
	 *
	 * @param config the {@link AbstractComponentConfig}
	 * @throws IllegalAccessException    on error
	 * @throws IllegalArgumentException  on error
	 * @throws InvocationTargetException on error
	 */
	public void addConfig(AbstractComponentConfig config)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var properties = config.getAsProperties();
		var keys = properties.keys();

		var c = this.getOrCreateEmptyConfiguration(config.id());
		while (keys.hasMoreElements()) {
			var key = keys.nextElement();
			c.properties.put(key, properties.get(key));
		}
	}

	/**
	 * Adds a simulated {@link DummyConfiguration}.
	 *
	 * @param key           the PID
	 * @param configuration the {@link DummyConfiguration}.
	 * @return myself
	 */
	public DummyConfigurationAdmin addConfiguration(String key, DummyConfiguration configuration) {
		this.configurations.put(key, configuration);
		return this;
	}

}