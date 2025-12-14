<<<<<<<< HEAD:io.openems.edge.ess.sma/test/io/openems/edge/ess/sma/sunnyisland/MyConfig.java
package io.openems.edge.ess.sma.sunnyisland;
========
package io.openems.edge.sma.meter.shm20;
>>>>>>>> develop:io.openems.edge.sma/test/io/openems/edge/sma/meter/shm20/MyConfig.java

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.ConfigUtils;
<<<<<<<< HEAD:io.openems.edge.ess.sma/test/io/openems/edge/ess/sma/sunnyisland/MyConfig.java
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.sma.sunnyisland.Config;
========
>>>>>>>> develop:io.openems.edge.sma/test/io/openems/edge/sma/meter/shm20/MyConfig.java

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId;
		private int modbusUnitId;
		private MeterType type;
		private boolean invert;

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

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setInvert(boolean invert) {
			this.invert = invert;
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
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public boolean invert() {
		return this.builder.invert;
	}

}