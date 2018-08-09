package io.openems.edge.ess.api;

import org.apache.commons.math3.optim.linear.Relationship;
import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;

@ProviderType
public interface ManagedSymmetricEss extends SymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
		DEBUG_SET_REACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the 'Power' class, which allows to set limitations to Active and
	 * Reactive Power.
	 * 
	 * @return
	 */
	public Power getPower();

	/**
	 * Apply the calculated Power
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
	 * <ul>
	 * 
	 * @return
	 */
	public int getPowerPrecision();

	/**
	 * Adds a Power constraint.
	 * 
	 * @param type         Whether this Constraint is STATIC or for one CYCLE only.
	 * @param phase        Apply Constraint on Phase L1, L2, L3 or on ALL Phases
	 * @param pwr          Constraint for ACTIVE or REACTIVE Power
	 * @param relationship Is the Constraint EQ (Equal), GEQ (Greater or Equal) or
	 *                     LEQ (Less or Equal)?
	 * @param value        The Constraint value (right side of the equation)
	 * @return the added Constraint
	 */
	public default Constraint addPowerConstraint(ConstraintType type, Phase phase, Pwr pwr, Relationship relationship,
			int value) {
		return this.getPower().addSimpleConstraint(this, type, phase, pwr, relationship, value);
	}

	/**
	 * Adds a Power constraint if the problem is still solvable after adding it.
	 * 
	 * @param type         Whether this Constraint is STATIC or for one CYCLE only.
	 * @param phase        Apply Constraint on Phase L1, L2, L3 or on ALL Phases
	 * @param pwr          Constraint for ACTIVE or REACTIVE Power
	 * @param relationship Is the Constraint EQ (Equal), GEQ (Greater or Equal) or
	 *                     LEQ (Less or Equal)?
	 * @param value        The Constraint value (right side of the equation)
	 * @return the added Constraint
	 * @throws PowerException if the problem is not solvable after adding the
	 *                        Constraint. In this case the Constraint was not added.
	 */
	public default Constraint addPowerConstraintAndValidate(ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) throws PowerException {
		return this.getPower().addSimpleConstraintAndValidate(this, type, phase, pwr, relationship, value);
	}
}
