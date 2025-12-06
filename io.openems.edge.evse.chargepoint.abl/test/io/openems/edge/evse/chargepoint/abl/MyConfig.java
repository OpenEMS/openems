package io.openems.edge.evse.chargepoint.abl;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

/**
 * Test configuration helper for ABL EVSE component.
 *
 * <p>
 * This class provides a fluent builder API for creating test configurations,
 * following the standard OpenEMS pattern used across all components.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * Config config = MyConfig.create() //
 * 		.setId("evcs0") //
 * 		.setEnabled(true) //
 * 		.setMaxCurrent(32) //
 * 		.build();
 * </pre>
 */
@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	/**
	 * Create a new builder for test configuration.
	 *
	 * @return a new Builder instance
	 */
	public static Builder create() {
		return new Builder();
	}

	public static class Builder {
		private String id = "evcs0";
		private String alias = "";
		private boolean enabled = true;
		private boolean readOnly = false;
		private boolean debugMode = false;
		private SingleOrThreePhase wiring = SingleOrThreePhase.THREE_PHASE;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;
		private String modbusId = "modbus0";
		private int modbusUnitId = 1;
		private int maxCurrent = 32;

		private Builder() {
		}

		/**
		 * Set the component ID.
		 *
		 * @param id component ID
		 * @return this builder
		 */
		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		/**
		 * Set the component alias.
		 *
		 * @param alias human-readable name
		 * @return this builder
		 */
		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		/**
		 * Set whether the component is enabled.
		 *
		 * @param enabled true to enable
		 * @return this builder
		 */
		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		/**
		 * Set whether the component is in read-only mode.
		 *
		 * @param readOnly true for read-only mode
		 * @return this builder
		 */
		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		/**
		 * Set whether debug mode is enabled.
		 *
		 * @param debugMode true to enable debug logging
		 * @return this builder
		 */
		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		/**
		 * Set the hardware wiring configuration.
		 *
		 * @param wiring SINGLE_PHASE or THREE_PHASE
		 * @return this builder
		 */
		public Builder setWiring(SingleOrThreePhase wiring) {
			this.wiring = wiring;
			return this;
		}

		/**
		 * Set the phase rotation.
		 *
		 * @param phaseRotation phase rotation configuration
		 * @return this builder
		 */
		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
			return this;
		}

		/**
		 * Set the Modbus bridge ID.
		 *
		 * @param modbusId Modbus bridge component ID
		 * @return this builder
		 */
		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		/**
		 * Set the Modbus unit ID.
		 *
		 * @param modbusUnitId Modbus unit/device ID (1-255)
		 * @return this builder
		 */
		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		/**
		 * Set the maximum current.
		 *
		 * @param maxCurrent maximum current in Ampere
		 * @return this builder
		 */
		public Builder setMaxCurrent(int maxCurrent) {
			this.maxCurrent = maxCurrent;
			return this;
		}

		/**
		 * Build the configuration.
		 *
		 * @return the MyConfig instance
		 */
		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public SingleOrThreePhase wiring() {
		return this.builder.wiring;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
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
	public int maxCurrent() {
		return this.builder.maxCurrent;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "EVSE Charge-Point ABL [" + this.id() + "]";
	}
}
