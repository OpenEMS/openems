package io.openems.backend.metadata.odoo;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Field {

	/**
	 * Gets the Field ID.
	 *
	 * @return the ID
	 */
	public String id();

	/**
	 * Gets the Field index.
	 *
	 * @return the index
	 */
	public int index();

	/**
	 * Gets the Field name.
	 *
	 * @return the name
	 */
	public String name();

	/**
	 * Should this Field be queried?.
	 *
	 * @return true if yes
	 */
	public boolean isQuery();

	private static <T extends Enum<? extends Field>> Stream<Field> getSqlQueryFieldsOf(Class<T> field){
		return Stream.of(field.getEnumConstants()).map(v -> {
			if (v instanceof Field f) {
				return f;
			} else {
				return null;
			}
		}).filter(Field::isQuery);
	}

	/**
	 * Gets all fields that should be queried as a comma separated string.
	 *
	 * @param fields an array of {@link Field}s
	 * @return the String
	 */
	public static String getSqlQueryFields(Field[] fields) {
		return Stream.of(fields) //
				.filter(Field::isQuery) //
				.map(Field::id) //
				.collect(Collectors.joining(","));
	}

	public static <T extends Enum<? extends Field>> Field[] getSqlQueryFields(Class<T> field) {
		return Field.getSqlQueryFieldsOf(field).toArray(Field[]::new);
	}

	public static <T extends Enum<? extends Field>> Field[] getSqlQueryFields(Class<T> field, Field... foreign) {
		var querryStream = Field.getSqlQueryFieldsOf(field);
		var foreignStream = Stream.of(foreign);
		return Stream.concat(querryStream, foreignStream).toArray(Field[]::new);
	}

	public record GenericField(String id) implements Field {
		public GenericField(Field... fields) {
			this(Stream.of(fields).map(Field::name).collect(Collectors.joining(".")));
		}

		@Override
		public int index() {
			return -1;
		}

		@Override
		public String name() {
			return this.id.toUpperCase();
		}

		@Override
		public boolean isQuery() {
			return false;
		}
	}

	/**
	 * The EdgeDevice-Model.
	 */
	public enum EdgeDevice implements Field {
		ID("id", true), //
		APIKEY("apikey", true), //
		SETUP_PASSWORD("setup_password", true), //
		NAME("name", true), //
		COMMENT("comment", true), //
		OPENEMS_VERSION("openems_version", true), //
		PRODUCTTYPE("producttype", true), //
		OPENEMS_CONFIG("openems_config", false), //
		OPENEMS_CONFIG_COMPONENTS("openems_config_components", false), //
		LASTMESSAGE("lastmessage", true), //
		OPENEMS_SUM_STATE("openems_sum_state_level", false), //
		OPENEMS_IS_CONNECTED("openems_is_connected", false), //
		STOCK_PRODUCTION_LOT_ID("stock_production_lot_id", false);

		public static final String ODOO_MODEL = "openems.device";
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

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
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

		public static final String ODOO_MODEL = "openems.openemsconfigupdate";
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

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}

	}

	/**
	 * The EdgeDeviceUserRole-Model.
	 */
	public enum EdgeDeviceUserRole implements Field {
		ID("id", true), //
		DEVICE_ODOO_ID("device_id", false), //
		USER_ODOO_ID("user_id", true), //
		ROLE("role", false), //
		TIME_TO_WAIT("time_to_wait", true), //
		LAST_NOTIFICATION("last_notification", true), //
		; //

		public static final String ODOO_MODEL = "openems.device_user_role";
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

		private EdgeDeviceUserRole(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}

	}

	public enum User implements Field {
		LOGIN("login", true), //
		PASSWORD("password", true), //
		PARTNER("partner_id", true), //
		GLOBAL_ROLE("global_role", true), //
		GROUPS("groups_id", true), //
		OPENEMS_LANGUAGE("openems_language", true);

		public static final String ODOO_MODEL = "res.users";
		public static final String ODOO_TABLE = User.ODOO_MODEL.replace(".", "_");

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

		private User(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}

	}

	public enum Partner implements Field {
		FIRSTNAME("firstname", true), //
		LASTNAME("lastname", true), //
		EMAIL("email", true), //
		PHONE("phone", true), //
		COMPANY_NAME("commercial_company_name", true), //
		NAME("name", true), //
		IS_COMPANY("is_company", true), //
		PARENT("parent_id", true), //
		STREET("street", true), //
		ZIP("zip", true), //
		CITY("city", true), //
		COUNTRY("country_id", true), //
		ADDRESS_TYPE("type", true), //
		LANGUAGE("lang", true), //
		CATEGORY_ID("category_id", true);

		public static final String ODOO_MODEL = "res.partner";
		public static final String ODOO_TABLE = Partner.ODOO_MODEL.replace(".", "_");

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

		private Partner(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}

	}

	public enum Country implements Field {
		NAME("name", true), //
		CODE("code", true);

		public static final String ODOO_MODEL = "res.country";
		public static final String ODOO_TABLE = Country.ODOO_MODEL.replace(".", "_");

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

		private Country(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}
	}

	public enum SetupProtocol implements Field {
		CUSTOMER("customer_id", true), //
		DIFFERENT_LOCATION("different_location_id", true), //
		INSTALLER("installer_id", true), //
		EDGE("device_id", true);

		public static final String ODOO_MODEL = "openems.setup_protocol";
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

		private SetupProtocol(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}
	}

	public enum SetupProtocolProductionLot implements Field {
		SETUP_PROTOCOL("setup_protocol_id", true), //
		SEQUENCE("sequence", true), //
		LOT("lot_id", true);

		public static final String ODOO_MODEL = "openems.setup_protocol_production_lot";
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

		private SetupProtocolProductionLot(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}
	}

	public enum SetupProtocolItem implements Field {
		SETUP_PROTOCOL("setup_protocol_id", true), //
		SEQUENCE("sequence", true);

		public static final String ODOO_MODEL = "openems.setup_protocol_item";
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

		private SetupProtocolItem(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}
	}

	public enum StockProductionLot implements Field {
		SERIAL_NUMBER("name", true), //
		PRODUCT("product_id", true);

		public static final String ODOO_MODEL = "stock.production.lot";
		public static final String ODOO_TABLE = StockProductionLot.ODOO_MODEL.replace(".", "_");

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

		private StockProductionLot(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}
	}

	public enum AlertingSetting implements Field {
		DEVICE_ODOO_ID("device_id", true), //
		USER_ODOO_ID("user_id", true), //
		OFFLINE_DELAY("offline_delay", true), //
		WARNING_DELAY("warning_delay", true), //
		FAULT_DELAY("fault_delay", true), //
		OFFLINE_LAST_NOTIFICATION("offline_last_notification", true), //
		SUM_STATE_LAST_NOTIFICATION("sum_state_last_notification", true);

		public static final String ODOO_MODEL = "openems.alerting";
		public static final String ODOO_TABLE = AlertingSetting.ODOO_MODEL.replace(".", "_");

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

		private AlertingSetting(String id, boolean query) {
			this.id = id;
			this.query = query;
			if (query) {
				this.queryIndex = StaticFields.nextQueryIndex++;
			} else {
				this.queryIndex = -1;
			}
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public int index() {
			return this.queryIndex;
		}

		@Override
		public boolean isQuery() {
			return this.query;
		}
	}

}
