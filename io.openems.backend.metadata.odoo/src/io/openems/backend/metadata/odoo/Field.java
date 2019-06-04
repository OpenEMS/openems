package io.openems.backend.metadata.odoo;

public interface Field {
	public String n();

	public enum EdgeDevice implements Field {
		ID("id"), //
		APIKEY("apikey"), //
		NAME("name"), //
		COMMENT("comment"), //
		STATE("state"), //
		OPENEMS_VERSION("openems_version"), //
		PRODUCT_TYPE("producttype"), //
		OPENEMS_CONFIG("openems_config"), //
		OPENEMS_CONFIG_COMPONENTS("openems_config_components"), //
		LAST_MESSAGE("lastmessage"), //
		LAST_UPDATE("lastupdate"), //
		SOC("soc"), //
		IPV4("ipv4"), //
		OPENEMS_SUM_STATE("openems_sum_state"), //
		OPENEMS_SUM_STATE_TEXT("openem_sum_state_text"), //
		OPENEMS_IS_CONNECTED("openems_is_connected");

		private final String n;

		private EdgeDevice(String n) {
			this.n = n;
		}

		public String n() {
			return n;
		}
	}
}
