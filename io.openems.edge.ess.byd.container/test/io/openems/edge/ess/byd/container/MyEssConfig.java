package io.openems.edge.ess.byd.container;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyEssConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private boolean readonly;
		private String modbus_id0;
		private String modbus_id1;
		private String modbus_id2;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setModbus_id0(String modbus_id0) {
			this.modbus_id0 = modbus_id0;
			return this;
		}

		public Builder setModbus_id1(String modbus_id1) {
			this.modbus_id1 = modbus_id1;
			return this;
		}

		public Builder setModbus_id2(String modbus_id2) {
			this.modbus_id2 = modbus_id2;
			return this;
		}

		public Builder setReadonly(boolean readonly) {
			this.readonly = readonly;
			return this;
		}

		public MyEssConfig build() {
			return new MyEssConfig(this);
		}
	}

	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyEssConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean readonly() {
		return this.builder.readonly;
	}

	@Override
	public String modbus_id0() {
		return this.builder.modbus_id0;
	}

	@Override
	public String modbus_id1() {
		return this.builder.modbus_id1;
	}

	@Override
	public String modbus_id2() {
		return this.builder.modbus_id2;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id0());
	}

	@Override
	public String modbus1_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id1());
	}

	@Override
	public String modbus2_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id2());
	}

}
