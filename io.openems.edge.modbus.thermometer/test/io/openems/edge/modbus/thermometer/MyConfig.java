package io.openems.edge.modbus.thermometer;

import io.openems.common.utils.ConfigUtils;
import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId = null;
		private int modbusUnitId;

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
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public String aliasOwd1() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd3() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd4() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd5() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd6() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd7() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd8() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd9() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aliasOwd10() {
		// TODO Auto-generated method stub
		return null;
	}

}