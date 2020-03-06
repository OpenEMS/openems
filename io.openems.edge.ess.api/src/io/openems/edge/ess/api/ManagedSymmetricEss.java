package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@ProviderType
public interface ManagedSymmetricEss extends SymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds the currently maximum allowed charge power. This value is commonly
		 * defined by current battery limitations.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or negative value
		 * </ul>
		 */
		ALLOWED_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		/**
		 * Holds the currently maximum allowed discharge power. This value is commonly
		 * defined by current battery limitations.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		ALLOWED_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		/**
		 * Sets a fixed Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetActivePowerEquals", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS))), //
		/**
		 * Applies the PID filter and then sets a fixed Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_EQUALS_WITH_PID(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetActivePowerEqualsWithPid", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS) {
					@Override
					public void accept(Channel<Integer> channel) {
						((IntegerWriteChannel) channel).onSetNextWrite(value -> {
							if (value != null) {
								ManagedSymmetricEss ess = (ManagedSymmetricEss) channel.getComponent();
								Power power = ess.getPower();
								PidFilter pidFilter = power.getPidFilter();

								// configure PID filter
								int minPower = power.getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
								int maxPower = power.getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
								pidFilter.setLimits(minPower, maxPower);

								int currentActivePower = ess.getActivePower().value().orElse(0);
								int pidOutput = pidFilter.applyPidFilter(currentActivePower, value);

								ess.getSetActivePowerEquals().setNextWriteValue(pidOutput);
							}
						});
					}
				})), //
		/**
		 * Sets a fixed Reactive Power.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetReactivePowerEquals", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS))), //
		/**
		 * Sets a fixed maximum Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetActivePowerLessOrEquals", Phase.ALL, Pwr.ACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetActivePowerGreaterOrEquals", Phase.ALL, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Sets a fixed maximum Reactive Power.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetReactivePowerLessOrEquals", Phase.ALL, Pwr.REACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Reactive Power.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetReactivePowerGreaterOrEquals", Phase.ALL, Pwr.REACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Holds settings of Active Power for debugging.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * just before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * StateChannel is set when calling applyPower() failed.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: StateChannel
		 * <li>Implementation Note: value is automatically written by
		 * {@link Power}-Solver if {@link ManagedAsymmetricEss#applyPower(int, int)}
		 * failed.
		 * </ul>
		 */
		APPLY_POWER_FAILED(Doc.of(Level.FAULT) //
				.text("Applying the Active/Reactive Power failed"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedSymmetricEss.class, accessMode, 100) //
				.channel(0, ChannelId.ALLOWED_CHARGE_POWER, ModbusType.FLOAT32) //
				.channel(2, ChannelId.ALLOWED_DISCHARGE_POWER, ModbusType.FLOAT32) //
				.channel(4, ChannelId.SET_ACTIVE_POWER_EQUALS, ModbusType.FLOAT32) //
				.channel(6, ChannelId.SET_REACTIVE_POWER_EQUALS, ModbusType.FLOAT32) //
				.channel(8, ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(10, ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(12, ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(14, ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets an instance of the 'Power' class, which allows to set limitations to
	 * Active and Reactive Power.
	 * 
	 * @return the Power instance
	 */
	public Power getPower();

	/**
	 * Gets the Allowed Charge Power in [W], range "&lt;= 0".
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getAllowedCharge() {
		return this.channel(ChannelId.ALLOWED_CHARGE_POWER);
	}

	/**
	 * Gets the Allowed Discharge Power in [W], range "&gt;= 0".
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getAllowedDischarge() {
		return this.channel(ChannelId.ALLOWED_DISCHARGE_POWER);
	}

	/**
	 * Gets the Set Active Power Equals in [W].
	 * 
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetActivePowerEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_EQUALS);
	}

	/**
	 * Applies the PID filter and then sets a fixed Active Power in [W].
	 * 
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetActivePowerEqualsWithPid() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID);
	}

	/**
	 * Gets the Set Reactive Power Equals in [var].
	 * 
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetReactivePowerEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_EQUALS);
	}

	/**
	 * Gets the Set Active Power Less Or Equals in [W].
	 * 
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetActivePowerLessOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Active Power Greater Or Equals in [W].
	 * 
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetActivePowerGreaterOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power Less Or Equals in [var].
	 * 
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetReactivePowerLessOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power Greater Or Equals in [var].
	 * 
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetReactivePowerGreaterOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Apply Power Failed StateChannel.
	 * 
	 * @return the Channel
	 */
	default StateChannel getApplyPowerFailed() {
		return this.channel(ChannelId.APPLY_POWER_FAILED);
	}

	/**
	 * Apply the calculated Power.
	 * 
	 * <p>
	 * Careful: do not adjust activePower and reactivePower in this method, e.g.
	 * setting it to zero on error. The purpose of this method is solely to apply
	 * the calculated power to the ESS. If you need to constrain the allowed power,
	 * add Constraints using the {@link #getStaticConstraints()} method.
	 * 
	 * @param activePower   the active power in [W]
	 * @param reactivePower the reactive power in [var]
	 * @throws OpenemsNamedException on error; causes activation of
	 *                               APPLY_POWER_FAILED StateChannel
	 */
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException;

	/**
	 * Gets the smallest positive power that can be set (in W, VA or var). Example:
	 * <ul>
	 * <li>FENECON Commercial 40 allows setting of power in 100 W steps. It should
	 * return 100.
	 * <li>KACO blueplanet gridsave 50 allows setting of power in 0.1 % of 52 VA. It
	 * should return 52 (= 52000 * 0.001)
	 * </ul>
	 * 
	 * @return the power precision
	 */
	public int getPowerPrecision();

	/**
	 * Gets static Constraints for this Ess. Override this method to provide
	 * specific Constraints for this Ess on every Cycle.
	 * 
	 * @return the Constraints
	 * @throws OpenemsException on error
	 */
	public default Constraint[] getStaticConstraints() throws OpenemsNamedException {
		return Power.NO_CONSTRAINTS;
	}

	/**
	 * Creates a Power Constraint.
	 * 
	 * @param description  a description for the Constraint
	 * @param phase        the affected power phase
	 * @param pwr          Active or Reactive power
	 * @param relationship equals, less-than or greater-than
	 * @param value        the function value
	 * @return the Constraint
	 * @throws OpenemsException on error
	 */
	public default Constraint createPowerConstraint(String description, Phase phase, Pwr pwr, Relationship relationship,
			double value) throws OpenemsException {
		return this.getPower().createSimpleConstraint(description, this, phase, pwr, relationship, value);
	}

	/**
	 * Adds a Power Constraint for the current Cycle.
	 * 
	 * <p>
	 * To add a Constraint on every Cycle, use getStaticConstraints()
	 * 
	 * @param description  a description for the Constraint
	 * @param phase        the affected power phase
	 * @param pwr          Active or Reactive power
	 * @param relationship equals, less-than or greater-than
	 * @param value        the function value
	 * @return the Constraint
	 * @throws OpenemsException on error
	 */
	public default Constraint addPowerConstraint(String description, Phase phase, Pwr pwr, Relationship relationship,
			double value) throws OpenemsException {
		return this.getPower().addConstraint(this.createPowerConstraint(description, phase, pwr, relationship, value));
	}

	/**
	 * Adds a Power Constraint for the current Cycle.
	 * 
	 * <p>
	 * To add a Constraint on every Cycle, use getStaticConstraints()
	 * 
	 * @param description  a description for the Constraint
	 * @param phase        the affected power phase
	 * @param pwr          Active or Reactive power
	 * @param relationship equals, less-than or greater-than
	 * @param value        the function value
	 * @return the Constraint
	 * @throws OpenemsException on error
	 */
	public default Constraint addPowerConstraintAndValidate(String description, Phase phase, Pwr pwr,
			Relationship relationship, double value) throws OpenemsException {
		return this.getPower()
				.addConstraintAndValidate(this.createPowerConstraint(description, phase, pwr, relationship, value));
	}
}
