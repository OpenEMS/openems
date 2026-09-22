package io.openems.edge.ess.stabl;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.ess.stabl.enums.EssState;
import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId = null;
		private int modbusUnitId;
		private EssState essState;
		private boolean useExternalSorting;
		private int numberOfStrings = 3;
		private String solverUrl = "http://localhost:8090/pdist-calculator-stabl";
		private boolean debugLogging = false;

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

		public Builder setEssState(EssState essState) {
			this.essState = essState;
			return this;
		}

		public Builder setUseExternalSorting(boolean useExternalSorting) {
			this.useExternalSorting = useExternalSorting;
			return this;
		}

		public Builder setNumberOfStrings(int numberOfStrings) {
			this.numberOfStrings = numberOfStrings;
			return this;
		}

		public Builder setSolverUrl(String solverUrl) {
			this.solverUrl = solverUrl;
			return this;
		}

		public Builder setDebugLogging(boolean debugLogging) {
			this.debugLogging = debugLogging;
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
	public EssState essState() {
		return this.builder.essState;
	}

	@Override
	public boolean useExternalSorting() {
		return this.builder.useExternalSorting;
	}

	@Override
	public int numberOfStrings() {
		return this.builder.numberOfStrings;
	}

	@Override
	public String solverUrl() {
		return this.builder.solverUrl;
	}

	@Override
	public boolean debugLogging() {
		return this.builder.debugLogging;
	}

}