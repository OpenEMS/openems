package io.openems.backend.metadata.odoo;

public interface Fields {
	public String n();

	public enum FemsDevice implements Fields {
		ID("id"), NAME("name"), COMMENT("comment"), PRODUCT_TYPE("producttype");

		private final String n;

		private FemsDevice(String n) {
			this.n = n;
		}

		public String n() {
			return n;
		}
	}
}
