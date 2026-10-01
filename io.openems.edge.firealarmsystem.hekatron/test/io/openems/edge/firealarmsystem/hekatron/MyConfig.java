<<<<<<<< HEAD:io.openems.edge.ess.adstec.storaxe/test/io/openems/edge/ess/adstec/storaxe/MyConfig.java
package io.openems.edge.ess.adstec.storaxe;
========
package io.openems.edge.firealarmsystem.hekatron;
>>>>>>>> b059229803 ([Edge] Hekatron B9-X2 fire alarm system: implementation as standalone component (#3294)):io.openems.edge.firealarmsystem.hekatron/test/io/openems/edge/firealarmsystem/hekatron/MyConfig.java

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId;
		private int modbusUnitId;
		private int capacity;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

<<<<<<<< HEAD:io.openems.edge.ess.adstec.storaxe/test/io/openems/edge/ess/adstec/storaxe/MyConfig.java
	@Override
	public int capacity() {
		return this.builder.capacity;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

========
>>>>>>>> b059229803 ([Edge] Hekatron B9-X2 fire alarm system: implementation as standalone component (#3294)):io.openems.edge.firealarmsystem.hekatron/test/io/openems/edge/firealarmsystem/hekatron/MyConfig.java
}