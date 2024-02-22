package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import java.time.Clock;
import java.util.List;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public record Context(//
		Clock clock, //
		Sum sum, //
		PredictorManager predictorManager, //
		TimeOfUseTariff timeOfUseTariff, //
		ManagedSymmetricEss ess, //
		List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves, //
		List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges, //
		ControlMode controlMode, //
		int maxChargePowerFromGrid) {

	public static class Builder {
		private Clock clock;
		private Sum sum;
		private PredictorManager predictorManager;
		private TimeOfUseTariff timeOfUseTariff;
		private ManagedSymmetricEss ess;
		private List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves;
		private List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges;
		private ControlMode controlMode;
		private int maxChargePowerFromGrid;

		/**
		 * The {@link Clock}.
		 * 
		 * @param clock the {@link Clock}
		 * @return myself
		 */
		public Builder clock(Clock clock) {
			this.clock = clock;
			return this;
		}

		/**
		 * The {@link Sum}.
		 * 
		 * @param sum the {@link Sum}
		 * @return myself
		 */
		public Builder sum(Sum sum) {
			this.sum = sum;
			return this;
		}

		/**
		 * The {@link PredictorManager}.
		 * 
		 * @param predictorManager the {@link PredictorManager}
		 * @return myself
		 */
		public Builder predictorManager(PredictorManager predictorManager) {
			this.predictorManager = predictorManager;
			return this;
		}

		/**
		 * The {@link TimeOfUseTariff}.
		 * 
		 * @param timeOfUseTariff the {@link TimeOfUseTariff}
		 * @return myself
		 */
		public Builder timeOfUseTariff(TimeOfUseTariff timeOfUseTariff) {
			this.timeOfUseTariff = timeOfUseTariff;
			return this;
		}

		/**
		 * The {@link ManagedSymmetricEss}.
		 * 
		 * @param ess the {@link ManagedSymmetricEss}
		 * @return myself
		 */
		public Builder ess(ManagedSymmetricEss ess) {
			this.ess = ess;
			return this;
		}

		/**
		 * The list of {@link ControllerEssEmergencyCapacityReserve}.
		 * 
		 * @param ctrlEmergencyCapacityReserves the list of
		 *                                      {@link ControllerEssEmergencyCapacityReserve}
		 * @return myself
		 */
		public Builder ctrlEmergencyCapacityReserves(
				List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves) {
			this.ctrlEmergencyCapacityReserves = ctrlEmergencyCapacityReserves;
			return this;
		}

		/**
		 * The list of {@link ControllerEssLimitTotalDischarge}.
		 * 
		 * @param ctrlLimitTotalDischarges the list of
		 *                                 {@link ControllerEssLimitTotalDischarge}
		 * @return myself
		 */
		public Builder ctrlLimitTotalDischarges(List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges) {
			this.ctrlLimitTotalDischarges = ctrlLimitTotalDischarges;
			return this;
		}

		/**
		 * The {@link ControlMode}.
		 * 
		 * @param controlMode the {@link ControlMode}
		 * @return myself
		 */
		public Builder controlMode(ControlMode controlMode) {
			this.controlMode = controlMode;
			return this;
		}

		/**
		 * The maxChargePowerFromGrid.
		 * 
		 * @param maxChargePowerFromGrid the maxChargePowerFromGrid
		 * @return myself
		 */
		public Builder maxChargePowerFromGrid(int maxChargePowerFromGrid) {
			this.maxChargePowerFromGrid = maxChargePowerFromGrid;
			return this;
		}

		/**
		 * Builds the {@link Context}.
		 * 
		 * @return the {@link Context} record
		 */
		public Context build() {
			return new Context(this.clock, this.sum, this.predictorManager, this.timeOfUseTariff, this.ess,
					this.ctrlEmergencyCapacityReserves, this.ctrlLimitTotalDischarges, this.controlMode,
					this.maxChargePowerFromGrid);
		}
	}

	/**
	 * Create a {@link Context} {@link Builder}.
	 * 
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Context.Builder();
	}

}