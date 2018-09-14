package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@ProviderType
public interface ManagedSymmetricEss extends SymmetricEss {

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

	/**
	 * Gets the 'Power' class, which allows to set limitations to Active and
	 * Reactive Power.
	 * 
	 * @return
	 */
	public Power getPower();

	/**
	 * Gets the Allowed Charge Power in [W], range "<= 0"
	 * 
	 * @return
	 */
	default Channel<Integer> getAllowedCharge() {
		return this.channel(ChannelId.ALLOWED_CHARGE_POWER);
	}

	/**
	 * Gets the Allowed Discharge Power in [W], range ">= 0"
	 * 
	 * @return
	 */
	default Channel<Integer> getAllowedDischarge() {
		return this.channel(ChannelId.ALLOWED_DISCHARGE_POWER);
	}

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
}
