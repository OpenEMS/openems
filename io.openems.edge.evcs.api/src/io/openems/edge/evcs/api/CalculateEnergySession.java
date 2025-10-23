package io.openems.edge.evcs.api;

public class CalculateEnergySession {

	private final Evcs parent;

	private boolean isPluggedOld = false;
	private Long energyAtStartOfSession;

	public CalculateEnergySession(Evcs parent) {
		this.parent = parent;
	}

	/**
	 * Updates the {@link Evcs.ChannelId#ENERGY_SESSION} from the
	 * {@link Evcs.ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
	 *
	 * @param isPlugged true, if a car is plugged.
	 */
	public void update(boolean isPlugged) {
		var energy = this.parent.getActiveProductionEnergy().get();
		if (energy == null) {
			this.parent._setEnergySession(null);
			return; // exit without updating isPluggedOld
		}

		if (isPlugged) {
			if (!this.isPluggedOld) { // Plugged-State changed
				this.energyAtStartOfSession = energy;
			}
			if (this.energyAtStartOfSession == null) { // Should not happen
				this.parent._setEnergySession(null);
				return;
			}

			this.parent._setEnergySession(Math.max(0, (int) (energy - this.energyAtStartOfSession)));
		}
		this.isPluggedOld = isPlugged;
	}
}