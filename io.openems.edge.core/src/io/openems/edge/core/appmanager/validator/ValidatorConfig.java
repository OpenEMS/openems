package io.openems.edge.core.appmanager.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import io.openems.edge.core.appmanager.OnlyIf;
import io.openems.edge.core.appmanager.Self;

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

		public Builder setCompatibleCheckableConfigs(CheckableConfig... compatibleCheckableConfigs) {
			this.compatibleCheckableConfigs = new ArrayList<>(Arrays.asList(compatibleCheckableConfigs));
			return this;
		}

		public Builder setInstallableCheckableConfigs(List<CheckableConfig> installableCheckableConfigs) {
			this.installableCheckableConfigs = installableCheckableConfigs;
			return this;
		}

		public Builder setInstallableCheckableConfigs(CheckableConfig... installableCheckableConfigs) {
			this.installableCheckableConfigs = new ArrayList<>(Arrays.asList(installableCheckableConfigs));
			return this;
		}

		public ValidatorConfig build() {
			return new ValidatorConfig(this.compatibleCheckableConfigs, this.installableCheckableConfigs);
		}

	}

	public static record CheckableConfig(//
			String checkableComponentName, //
			boolean invertResult, //
			Map<String, ?> properties //
	) {

		public CheckableConfig(String checkableComponentName, Map<String, ?> properties) {
			this(checkableComponentName, false, properties);
		}

		/**
		 * Creates a new {@link CheckableConfig} with the current
		 * {@link CheckableConfig#invertResult} inverted.
		 * 
		 * @return the new {@link CheckableConfig}
		 */
		public CheckableConfig invert() {
			return new CheckableConfig(//
					this.checkableComponentName(), //
					!this.invertResult(), //
					this.properties() //
			);
		}

		/**
		 * Creates a {@link CheckableConfig} which checks if the current check is
		 * successful or the other check.
		 * 
		 * @param other the other check
		 * @return the {@link CheckableConfig}
		 */
		public CheckableConfig or(CheckableConfig other) {
			return Checkables.checkOr(this, other);
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

	public static final class MapBuilder<T extends Map<K, V>, K, V>
			implements Self<MapBuilder<T, K, V>>, OnlyIf<MapBuilder<T, K, V>> {

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

		@Override
		public MapBuilder<T, K, V> self() {
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
