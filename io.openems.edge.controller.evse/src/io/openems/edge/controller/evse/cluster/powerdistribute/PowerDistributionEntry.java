package io.openems.edge.controller.evse.cluster.powerdistribute;

import static com.google.common.base.MoreObjects.toStringHelper;

import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class PowerDistributionEntry {

	public final PowerDistributionKey key;
	public final Status status;
	public final Mode mode;
	public final Integer activePower;
	public final ApplySetPoint.Ability.Watt applySetPoint;

	protected int setPointInWatt;

	public PowerDistributionEntry(PowerDistributionKey key, Status status, Mode mode, Integer activePower,
			ApplySetPoint.Ability.Watt applySetPoint) {
		this.key = key;
		this.status = status;
		this.mode = mode;
		this.activePower = activePower;
		this.applySetPoint = applySetPoint;
	}

	public int getSetPointInWatt() {
		return this.setPointInWatt;
	}

	public void setSetPointInWatt(int setPointInWatt) {
		this.setPointInWatt = setPointInWatt;
	}

	/**
	 * Checks if the mode is surplus charging.
	 *
	 * @return true for surplus mode
	 */
	public boolean hasSurplus() {
		return switch (this.mode) {
		case FORCE, MINIMUM, ZERO -> false;
		case SURPLUS -> true;
		};
	}

	/**
	 * Checks if the mode is surplus or minimum charging.
	 *
	 * @return true for surplus or minimum mode
	 */
	public boolean hasSurplusOrMinimum() {
		return switch (this.mode) {
		case FORCE, ZERO -> false;
		case SURPLUS, MINIMUM -> true;
		};
	}

	@Override
	public final String toString() {
		return toStringHelper(PowerDistributionEntry.class) //
				.add("key", this.key) //
				.add("status", this.status) //
				.add("mode", this.mode) //
				.add("activePower", this.activePower) //
				.add("setPointInWatt", this.setPointInWatt) //
				.toString();
	}

	public enum Status {
		NOT_AVAILABLE, ACTIVE, CANNOT_ACCEPT_ENERGY,
		/* READ_ONLY */
	}
}
