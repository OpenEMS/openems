package io.openems.edge.common.test;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Simulates a ConfigurationAdmin for the OpenEMS Component test framework.
 */
public class DummyConfigurationAdmin implements ConfigurationAdmin {

	public static class DummyConfiguration implements Configuration {

		Hashtable<String, Object> properties = new Hashtable<String, Object>();

		@Override
		public String getPid() {
			return "";
		}

		@Override
		public Dictionary<String, Object> getProperties() {
			return this.properties;
		}

		@Override
		public void update(Dictionary<String, ?> properties) throws IOException {
			Enumeration<String> keys = properties.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				Object value = properties.get(key);
				this.properties.put(key, value);
			}
		}

		@Override
		public void update() throws IOException {
		}

		@Override
		public void delete() throws IOException {
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
			return 0;
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
	}

	private final DummyConfiguration dummyConfiguration = new DummyConfiguration();

	@Override
	public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
		return this.dummyConfiguration;
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
		return this.dummyConfiguration;
	}

	@Override
	public Configuration getConfiguration(String pid, String location) throws IOException {
		return this.dummyConfiguration;
	}

	@Override
	public Configuration getConfiguration(String pid) throws IOException {
		return this.dummyConfiguration;
	}

	@Override
	public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
		return new Configuration[] { this.dummyConfiguration };
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name, String location) throws IOException {
		return this.dummyConfiguration;
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name) throws IOException {
		return this.dummyConfiguration;
	}

}