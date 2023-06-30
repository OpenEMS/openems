package io.openems.edge.common.test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import io.openems.common.test.AbstractComponentConfig;

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
		 */
		public void addProperty(String key, Object value) {
			this.properties.put(key, value);
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

	@Override
	public synchronized Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
		return this.configurations.values().stream() //
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
	 */
	public void addConfiguration(String key, DummyConfiguration configuration) {
		this.configurations.put(key, configuration);
	}

}