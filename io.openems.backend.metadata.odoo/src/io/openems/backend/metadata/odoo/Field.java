package io.openems.backend.metadata.odoo;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Field {

	public String id();

	public enum EdgeDevice implements Field {
		ID("id", true), //
		APIKEY("apikey", false), //
		NAME("name", true), //
		COMMENT("comment", true), //
		STATE("state", true), //
		OPENEMS_VERSION("openems_version", true), //
		PRODUCT_TYPE("producttype", true), //
		OPENEMS_CONFIG("openems_config", true), //
		OPENEMS_CONFIG_COMPONENTS("openems_config_components", false), //
		LAST_MESSAGE("lastmessage", false), //
		LAST_UPDATE("lastupdate", false), //
		SOC("soc", true), //
		IPV4("ipv4", true), //
		OPENEMS_SUM_STATE("openems_sum_state", false), //
		OPENEMS_SUM_STATE_TEXT("openem_sum_state_text", false), //
		OPENEMS_IS_CONNECTED("openems_is_connected", false);

		private static final class StaticFields {
			private static int nextQueryIndex = 1;
		}

		private final int queryIndex;
		private final String id;
		/**
		 * Holds information if this Field should be queried from Database.
		 */
		private final boolean query;

		private EdgeDevice(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		public String id() {
			return this.id;
		}

		public int index() {
			return queryIndex;
		}

		public boolean isQuery() {
			return query;
		}

		/**
		 * Gets all fields that should be queried as a comma separated string.
		 * 
		 * @return the String
		 */
		public static final String getSqlQueryFields() {
			return Stream.of(EdgeDevice.values()) //
					.filter(f -> f.isQuery()) //
					.map(f -> f.id()) //
					.collect(Collectors.joining(","));
		}
	}
}
