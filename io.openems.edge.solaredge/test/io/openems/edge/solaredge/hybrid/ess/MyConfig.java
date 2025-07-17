package io.openems.edge.solaredge.hybrid.ess;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean readOnly;
		private boolean hybrid;
		private String modbusId;
		private int modbusUnitId;
		private SingleOrAllPhase phase;
		private boolean debugMode;
		private boolean readOnlyMode;
		private int chargePowerLimit;
		private int dischargePowerLimit;
		private int feedToGridPowerLimit;
		private int maxPvProductionPowerLimit;
		
		private String meterId;
		private String coreTarget;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}
		
		public Builder setHybrid(boolean hybrid) {
			this.hybrid = hybrid;
			return this;
		}		
		
		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}			

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
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

		public Builder setPhase(SingleOrAllPhase phase) {
			this.phase = phase;
			return this;
		}
		
		public Builder setCoreTarget(String coreTarget) {
			this.coreTarget = coreTarget;
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



	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}






	@Override
	public boolean readOnlyMode() {
		return this.builder.readOnlyMode;
	}



	@Override
	public int chargePowerLimit() {
		return this.builder.chargePowerLimit;
	}



	@Override
	public int dischargePowerLimit() {
		return this.builder.dischargePowerLimit;
	}



	@Override
	public int feedToGridPowerLimit() {
		return this.builder.feedToGridPowerLimit;
	}



	@Override
	public int maxPvProductionPowerLimit() {
		return this.builder.maxPvProductionPowerLimit;
	}



	@Override
	public String meter_id() {
		return this.builder.meterId;
	}



	@Override
	public String core_target() {
		return this.builder.coreTarget;
	}



}