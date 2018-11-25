package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.AccessMode;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@ProviderType
public interface ManagedSymmetricEss extends SymmetricEss {

	public static final Logger log = LoggerFactory.getLogger(ManagedSymmetricEss.class);

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
		ALLOWED_CHARGE_POWER(new Doc().unit(Unit.WATT)), //
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
		ALLOWED_DISCHARGE_POWER(new Doc().unit(Unit.WATT)), //
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
		SET_ACTIVE_POWER_EQUALS(new Doc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetActivePowerEquals", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS))), //
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
		SET_REACTIVE_POWER_EQUALS(new Doc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetReactivePowerEquals", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS))), //
		/**
		 * Holds settings of Active Power for debugging
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
		DEBUG_SET_ACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging
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
		DEBUG_SET_REACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)) //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable() {
		return ModbusSlaveNatureTable.of(ManagedSymmetricEss.class, 100) //
				.channel(0, ChannelId.ALLOWED_CHARGE_POWER, ModbusType.FLOAT32) //
				.channel(2, ChannelId.ALLOWED_DISCHARGE_POWER, ModbusType.FLOAT32) //
				.channel(4, ChannelId.SET_ACTIVE_POWER_EQUALS, ModbusType.FLOAT32) //
				.channel(6, ChannelId.SET_REACTIVE_POWER_EQUALS, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets the 'Power' class, which allows to set limitations to Active and
	 * Reactive Power.
	 * 
	 * @return
	 */
	public Power getPower();

	/**
	 * Gets the Allowed Charge Power in [W], range "&lt;= 0"
	 * 
	 * @return
	 */
	default Channel<Integer> getAllowedCharge() {
		return this.channel(ChannelId.ALLOWED_CHARGE_POWER);
	}

	/**
	 * Gets the Allowed Discharge Power in [W], range "&gt;= 0"
	 * 
	 * @return
	 */
	default Channel<Integer> getAllowedDischarge() {
		return this.channel(ChannelId.ALLOWED_DISCHARGE_POWER);
	}

	/**
	 * Gets the Set Active Power Equals in [W]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power Equals in [var]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_EQUALS);
	}

	/**
	 * Apply the calculated Power.
	 * 
	 * Careful: do not adjust activePower and reactivePower in this method, e.g.
	 * setting it to zero on error. The purpose of this method is solely to apply
	 * the calculated power to the ESS. If you need to constrain the allowed power,
	 * add Constraints using the {@link #getStaticConstraints()} method.
	 * 
	 * @param activePower
	 * @param reactivePower
	 */
	public void applyPower(int activePower, int reactivePower);

	/**
	 * Gets the smallest positive power that can be set (in W, VA or var). Example:
	 * <ul>
	 * <li>FENECON Commercial 40 allows setting of power in 100 W steps. It should
	 * return 100.
	 * <li>KACO blueplanet gridsave 50 allows setting of power in 0.1 % of 52 VA. It
	 * should return 52 (= 52000 * 0.001)
	 * </ul>
	 * 
	 * @return
	 */
	public int getPowerPrecision();

	/**
	 * Gets static Constraints for this Ess. Override this method to provide
	 * specific Constraints for this Ess on every Cycle.
	 * 
	 * @return
	 */
	public default Constraint[] getStaticConstraints() {
		return Power.NO_CONSTRAINTS;
	}

	/**
	 * Creates a Power Constraint
	 * 
	 * @param description
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 */
	public default Constraint createPowerConstraint(String description, Phase phase, Pwr pwr, Relationship relationship,
			double value) {
		return this.getPower().createSimpleConstraint(description, this, phase, pwr, relationship, value);
	}

	/**
	 * Adds a Power Constraint for the current Cycle.
	 * 
	 * To add a Constraint on every Cycle, use getStaticConstraints()
	 * 
	 * @param description
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 */
	public default Constraint addPowerConstraint(String description, Phase phase, Pwr pwr, Relationship relationship,
			double value) {
		return this.getPower().addConstraint(this.createPowerConstraint(description, phase, pwr, relationship, value));
	}

	/**
	 * Adds a Power Constraint for the current Cycle.
	 * 
	 * To add a Constraint on every Cycle, use getStaticConstraints()
	 * 
	 * @param description
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @throws PowerException
	 */
	public default Constraint addPowerConstraintAndValidate(String description, Phase phase, Pwr pwr,
			Relationship relationship, double value) throws PowerException {
		return this.getPower()
				.addConstraintAndValidate(this.createPowerConstraint(description, phase, pwr, relationship, value));
	}
}
