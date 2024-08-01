package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
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
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
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
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
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
				.onChannelSetNextWrite(
						new PowerConstraint("SetActivePowerEquals", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS))),

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
				.onChannelSetNextWrite(
						new PowerConstraint("SetActivePowerEqualsWithPid", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS) {
							@Override
							public void accept(ManagedSymmetricEss ess, Integer value) throws OpenemsNamedException {
								if (value != null) {
									var power = ess.getPower();
									var pidFilter = power.getPidFilter();

									// configure PID filter
									var minPower = power.getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
									var maxPower = power.getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
									if (maxPower < minPower) {
										maxPower = minPower; // avoid rounding error
									}
									pidFilter.setLimits(minPower, maxPower);

									int currentActivePower = ess.getActivePower().orElse(0);
									var pidOutput = pidFilter.applyPidFilter(currentActivePower, value);

									ess.setActivePowerEquals(pidOutput);
								}
							}
						})),
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
				.onChannelSetNextWrite(
						new PowerConstraint("SetReactivePowerEquals", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS))), //
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
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerLessOrEquals", Phase.ALL, Pwr.ACTIVE,
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
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerGreaterOrEquals", Phase.ALL, Pwr.ACTIVE,
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
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerLessOrEquals", Phase.ALL, Pwr.REACTIVE,
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
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerGreaterOrEquals", Phase.ALL, Pwr.REACTIVE,
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
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
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
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
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
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Applying the Active/Reactive Power failed"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedSymmetricEss.class, accessMode, 100) //
				.<ManagedSymmetricEss>cycleValue(0, "Minimum Power Set-Point", Unit.WATT, "", ModbusType.FLOAT32,
						c -> c.getPower().getMinPower(c, Phase.ALL, Pwr.ACTIVE)) //
				.<ManagedSymmetricEss>cycleValue(2, "Maximum Power Set-Point", Unit.WATT, "", ModbusType.FLOAT32,
						c -> c.getPower().getMaxPower(c, Phase.ALL, Pwr.ACTIVE)) //
				.channel(4, ChannelId.SET_ACTIVE_POWER_EQUALS, ModbusType.FLOAT32) //
				.channel(6, ChannelId.SET_REACTIVE_POWER_EQUALS, ModbusType.FLOAT32) //
				.channel(8, ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(10, ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(12, ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(14, ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets a boolean if the ess is managed or not.
	 *
	 * <p>
	 * Returns false if the ess itself is not managed or in a read only mode.
	 *
	 * @return is managed or not
	 */
	public default boolean isManaged() {
		return true;
	}

	/**
	 * Gets an instance of the 'Power' class, which allows to set limitations to
	 * Active and Reactive Power.
	 *
	 * @return the Power instance
	 */
	public Power getPower();

	/**
	 * Gets the Channel for {@link ChannelId#ALLOWED_CHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAllowedChargePowerChannel() {
		return this.channel(ChannelId.ALLOWED_CHARGE_POWER);
	}

	/**
	 * Gets the Allowed Charge Power in [W], range "<= 0". See
	 * {@link ChannelId#ALLOWED_CHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAllowedChargePower() {
		return this.getAllowedChargePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ALLOWED_CHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAllowedChargePower(Integer value) {
		this.getAllowedChargePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ALLOWED_CHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAllowedChargePower(int value) {
		this.getAllowedChargePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ALLOWED_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAllowedDischargePowerChannel() {
		return this.channel(ChannelId.ALLOWED_DISCHARGE_POWER);
	}

	/**
	 * Gets the Allowed Discharge Power in [W], range ">= 0". See
	 * {@link ChannelId#ALLOWED_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAllowedDischargePower() {
		return this.getAllowedDischargePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ALLOWED_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAllowedDischargePower(Integer value) {
		this.getAllowedDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ALLOWED_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAllowedDischargePower(int value) {
		this.getAllowedDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_EQUALS);
	}

	/**
	 * Sets an Active Power Equals setpoint in [W]. Negative values for Charge;
	 * positive for Discharge. See {@link ChannelId#SET_ACTIVE_POWER_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setActivePowerEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_EQUALS_WITH_PID}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerEqualsWithPidChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID);
	}

	/**
	 * Sets an Active Power Equals setpoint in [W] with applied PID filter. Negative
	 * values for Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_EQUALS_WITH_PID}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setActivePowerEqualsWithPid(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerEqualsWithPidChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_EQUALS);
	}

	/**
	 * Sets a Reactive Power Equals setpoint in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReactivePowerEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerLessOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Less Or Equals setpoint in [W]. Negative values for
	 * Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setActivePowerLessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerLessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerGreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Greater Or Equals setpoint in [W]. Negative values for
	 * Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setActivePowerGreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerGreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerLessOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Less Or Equals setpoint in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReactivePowerLessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerLessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerGreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Greater Or Equals setpoint in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReactivePowerGreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerGreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetActivePowerChannel() {
		return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER);
	}

	/**
	 * Gets the last Active Power setpoint in [W]. See
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetActivePower() {
		return this.getDebugSetActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePower(Integer value) {
		this.getDebugSetActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePower(int value) {
		this.getDebugSetActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_REACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetReactivePowerChannel() {
		return this.channel(ChannelId.DEBUG_SET_REACTIVE_POWER);
	}

	/**
	 * Gets the last Reactive Power setpoint in [var]. See
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetReactivePower() {
		return this.getDebugSetReactivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePower(Integer value) {
		this.getDebugSetReactivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePower(int value) {
		this.getDebugSetReactivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#APPLY_POWER_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getApplyPowerFailedChannel() {
		return this.channel(ChannelId.APPLY_POWER_FAILED);
	}

	/**
	 * Gets the Apply Power Failed State. See {@link ChannelId#APPLY_POWER_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getApplyPowerFailed() {
		return this.getApplyPowerFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#APPLY_POWER_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setApplyPowerFailed(boolean value) {
		this.getApplyPowerFailedChannel().setNextValue(value);
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
