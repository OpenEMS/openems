package io.openems.backend.metadata.odoo;

public interface Field {
	public String n();

	public enum FemsDevice implements Field {
		ID("id"), NAME("name"), COMMENT("comment"), PRODUCT_TYPE("producttype"), OPENEMS_CONFIG("openems_config");

		private final String n;

		private FemsDevice(String n) {
			this.n = n;
		}

		public String n() {
			return n;
		}
	}
}
