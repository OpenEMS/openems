package io.openems.edge.energy.v1.optimizer;

import java.time.Clock;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Deprecated
public record GlobalContextV1(//
		Clock clock, //
		EnergyScheduleHandlerV1 energyScheduleHandler, //
		Sum sum, //
		PredictorManager predictorManager, //
		TimeOfUseTariff timeOfUseTariff) {

	public static class Builder {
		private Clock clock;
		private EnergyScheduleHandlerV1 energyScheduleHandler;
		private Sum sum;
		private PredictorManager predictorManager;
		private TimeOfUseTariff timeOfUseTariff;

		/**
		 * The {@link Clock}.
		 * 
		 * @param clock the {@link Clock}
		 * @return myself
		 */
		public Builder setClock(Clock clock) {
			this.clock = clock;
			return this;
		}

		/**
		 * The {@link EnergyScheduleHandler} of the {@link TimeOfUseTariffController}.
		 * 
		 * @param energyScheduleHandler the {@link EnergyScheduleHandler}
		 * @return myself
		 */
		public Builder setEnergyScheduleHandler(EnergyScheduleHandlerV1 energyScheduleHandler) {
			this.energyScheduleHandler = energyScheduleHandler;
			return this;
		}

		/**
		 * The {@link Sum}.
		 * 
		 * @param sum the {@link Sum}
		 * @return myself
		 */
		public Builder setSum(Sum sum) {
			this.sum = sum;
			return this;
		}

		/**
		 * The {@link PredictorManager}.
		 * 
		 * @param predictorManager the {@link PredictorManager}
		 * @return myself
		 */
		public Builder setPredictorManager(PredictorManager predictorManager) {
			this.predictorManager = predictorManager;
			return this;
		}

		/**
		 * The {@link TimeOfUseTariff}.
		 * 
		 * @param timeOfUseTariff the {@link TimeOfUseTariff}
		 * @return myself
		 */
		public Builder setTimeOfUseTariff(TimeOfUseTariff timeOfUseTariff) {
			this.timeOfUseTariff = timeOfUseTariff;
			return this;
		}

		/**
		 * Builds the {@link GlobalContextV1}.
		 * 
		 * @return the {@link GlobalContextV1} record
		 */
		public GlobalContextV1 build() {
			return new GlobalContextV1(this.clock, this.energyScheduleHandler, this.sum, this.predictorManager,
					this.timeOfUseTariff);
		}
	}

	/**
	 * Create a {@link GlobalContextV1} {@link Builder}.
	 * 
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new GlobalContextV1.Builder();
	}

}
