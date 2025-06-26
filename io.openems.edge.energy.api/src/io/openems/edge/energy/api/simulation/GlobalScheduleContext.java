package io.openems.edge.energy.api.simulation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.Math.max;

/**
 * Holds the context that is used globally for the simulation of one schedule.
 * 
 * <p>
 * This record is usually created multiple times per second.
 */
public class GlobalScheduleContext {

	public static class Ess {
		/** ESS Currently Available Energy (SoC in [Wh]). */
		private int initialEnergy;

		protected static Ess from(GlobalOptimizationContext.Ess ess) {
			return new Ess(ess.currentEnergy());
		}

		private Ess(int initialEnergy) {
			this.initialEnergy = initialEnergy;
		}

		/**
		 * Calculates the initial SoC-Energy of the next Period.
		 * 
		 * @param ess the ess charge/discharge energy of the current Period
		 */
		public synchronized void calculateInitialEnergy(int ess) {
			this.initialEnergy = max(0, this.initialEnergy - ess); // always at least '0'
		}

		/**
		 * The initial SoC-Energy of the Period.
		 * 
		 * @return the value
		 */
		public int getInitialEnergy() {
			return this.initialEnergy;
		}

		@Override
		public String toString() {
			return toStringHelper(this) //
					.add("initialEnergy", this.initialEnergy) //
					.toString();
		}
	}

	/**
	 * Builds a {@link GlobalScheduleContext}.
	 * 
	 * @param goc the {@link GlobalOptimizationContext}
	 * @return the {@link GlobalScheduleContext}
	 */
	public static GlobalScheduleContext from(GlobalOptimizationContext goc) {
		return new GlobalScheduleContext(goc, Ess.from(goc.ess()));
	}

	public final GlobalOptimizationContext goc;
	public final Ess ess;

	private GlobalScheduleContext(GlobalOptimizationContext goc, Ess ess) {
		this.goc = goc;
		this.ess = ess;
	}

	@Override
	public String toString() {
		return toStringHelper(this) //
				.add("ess", this.ess) //
				.addValue(this.goc) //
				.toString();
	}
}
