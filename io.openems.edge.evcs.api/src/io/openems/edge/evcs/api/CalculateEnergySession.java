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
		if (isPlugged) {
			if (!this.isPluggedOld) {
				this.energyAtStartOfSession = this.parent.getActiveProductionEnergy().orElse(0L);
			}
			this.parent._setEnergySession(
					(int) (this.parent.getActiveProductionEnergy().orElse(0L) - this.energyAtStartOfSession));
		}
		this.isPluggedOld = isPlugged;
	}

}