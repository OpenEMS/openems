package io.openems.edge.ess.refubeckhoff;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.ess.refubeckhoff.enums.EssState;
import io.openems.edge.ess.refubeckhoff.enums.SetOperationMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		private EssState essState = null;
		private SetOperationMode setOperationMode = null;
		private int acknowledgeError;
		private boolean symmetricMode;

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

		public Builder setEssState(EssState essState) {
			this.essState = essState;
			return this;
		}

		public Builder setSetOperationMode(SetOperationMode setOperationMode) {
			this.setOperationMode = setOperationMode;
			return this;
		}

		public Builder setAcknowledgeError(int acknowledgeError) {
			this.acknowledgeError = acknowledgeError;
			return this;
		}

		public Builder setSymmetricMode(boolean symmetricMode) {
			this.symmetricMode = symmetricMode;
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
	public EssState essState() {
		return this.builder.essState;
	}

	@Override
	public SetOperationMode operationState() {
		return this.builder.setOperationMode;
	}

	@Override
	public int acknowledgeError() {
		return this.builder.acknowledgeError;
	}

	@Override
	public boolean symmetricMode() {
		return this.builder.symmetricMode;
	}

}