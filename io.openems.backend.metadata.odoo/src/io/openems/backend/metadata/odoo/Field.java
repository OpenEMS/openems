package io.openems.backend.metadata.odoo;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Field {

	public String id();

	public int index();

	public String name();

	public boolean isQuery();

	/**
	 * Gets all fields that should be queried as a comma separated string.
	 * 
	 * @return the String
	 */
	public static String getSqlQueryFields(Field[] fields) {
		return Stream.of(fields) //
				.filter(f -> f.isQuery()) //
				.map(f -> f.id()) //
				.collect(Collectors.joining(","));
	}

	/**
	 * The EdgeDevice-Model.
	 */
	public enum EdgeDevice implements Field {
		ID("id", true), //
		APIKEY("apikey", true), //
		NAME("name", true), //
		COMMENT("comment", true), //
		STATE("state", true), //
		OPENEMS_VERSION("openems_version", true), //
		PRODUCT_TYPE("producttype", true), //
		OPENEMS_CONFIG("openems_config", true), //
		OPENEMS_CONFIG_COMPONENTS("openems_config_components", false), //
		LAST_MESSAGE("lastmessage", false), //
		LAST_UPDATE("lastupdate", false), //
		OPENEMS_SUM_STATE("openems_sum_state_level", false), //
		OPENEMS_SUM_STATE_TEXT("openems_sum_state_text", false), //
		OPENEMS_IS_CONNECTED("openems_is_connected", false);

		public final static String ODOO_MODEL = "fems.device";
		public static final String ODOO_TABLE = ODOO_MODEL.replace(".", "_");

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
			return this.queryIndex;
		}

		public boolean isQuery() {
			return this.query;
		}
	}

	/**
	 * The EdgeDeviceStatus-Model.
	 */
	public enum EdgeDeviceStatus implements Field {
		DEVICE_ID("device_id", false), //
		CHANNEL_ADDRESS("channel_address", false), //
		LEVEL("level", true), //
		COMPONENT_ID("component_id", true), //
		CHANNEL_NAME("channel_name", true), //
		LAST_APPEARANCE("last_appearance", false), //
		LAST_ACKNOWLEDGE("last_acknowledge", false), //
		ACKNOWLEDGE_DAYS("acknowledge_days", false);

		public final static String ODOO_MODEL = "fems.device_status";
		public static final String ODOO_TABLE = ODOO_MODEL.replace(".", "_");

		private static final class StaticFields {
			private static int nextQueryIndex = 1;
		}

		private final int queryIndex;
		private final String id;
		/**
		 * Holds information if this Field should be queried from and written to
		 * Database.
		 */
		private final boolean query;

		private EdgeDeviceStatus(String id, boolean query) {
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
			return this.queryIndex;
		}

		public boolean isQuery() {
			return this.query;
		}
	}

	/**
	 * The EdgeConfigUpdate-Model.
	 */
	public enum EdgeConfigUpdate implements Field {
		DEVICE_ID("device_id", false), //
		TEASER("teaser", false), //
		DETAILS("details", false);

		public final static String ODOO_MODEL = "fems.openemsconfigupdate";
		public static final String ODOO_TABLE = ODOO_MODEL.replace(".", "_");

		private static final class StaticFields {
			private static int nextQueryIndex = 1;
		}

		private final int queryIndex;
		private final String id;
		/**
		 * Holds information if this Field should be queried from and written to
		 * Database.
		 */
		private final boolean query;

		private EdgeConfigUpdate(String id, boolean query) {
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
			return this.queryIndex;
		}

		public boolean isQuery() {
			return this.query;
		}
	}
}
