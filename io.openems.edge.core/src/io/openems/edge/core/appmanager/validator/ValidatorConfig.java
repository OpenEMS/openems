package io.openems.edge.core.appmanager.validator;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

public class ValidatorConfig {

	private final List<CheckableConfig> compatibleCheckableConfigs;
	private final List<CheckableConfig> installableCheckableConfigs;

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

		public ValidatorConfig build() {
			return new ValidatorConfig(this.compatibleCheckableConfigs, this.installableCheckableConfigs);
		}

	}

	// TODO convert to record in java 17.
	public static final class CheckableConfig {

		public final String checkableComponentName;
		public final boolean invertResult;
		public final Map<String, ?> properties;

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
			if (obj == null || this.getClass() != obj.getClass()) {
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
	 * Creates a builder for an {@link ValidatorConfig}.
	 *
	 * @return the builder
	 */
	public static final Builder create() {
		return new Builder();
	}

	protected ValidatorConfig(List<CheckableConfig> compatibleCheckableConfigs,
			List<CheckableConfig> installableCheckableConfigs) {
		this.compatibleCheckableConfigs = compatibleCheckableConfigs != null //
				? compatibleCheckableConfigs
				: Lists.newArrayList();
		this.installableCheckableConfigs = installableCheckableConfigs != null //
				? installableCheckableConfigs
				: Lists.newArrayList();
	}

	public List<CheckableConfig> getCompatibleCheckableConfigs() {
		return this.compatibleCheckableConfigs;
	}

	public List<CheckableConfig> getInstallableCheckableConfigs() {
		return this.installableCheckableConfigs;
	}

}
