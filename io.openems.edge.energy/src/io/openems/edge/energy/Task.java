package io.openems.edge.energy;

import io.openems.edge.energy.api.schedulable.Schedules;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 *
 */
public class Task implements Runnable {

	// private final Logger log = LoggerFactory.getLogger(Task.class);

	// private final EnergyImpl parent;

	private Schedules schedules;

	public Task(EnergyImpl parent) {
		// this.parent = parent;
	}

	@Override
	public void run() {
		// // Schedules
		// var now = ZonedDateTime.now(this.parent.componentManager.getClock());
		// var essFixActivePowerModes =
		// Utils.createEssFixActivePowerScheduleFromConfig(this.parent.config);
		// var evcsModes = Utils.createEvcsScheduleFromConfig(this.parent.config);
		//
		// this.schedules = Schedules.create() //
		// .add("ctrlEssFixActivePower0", Schedule.of(now, essFixActivePowerModes)) //
		// .add("ctrlEvcs0", Schedule.of(now, evcsModes)) //
		// .build();
		//
		// this.parent.ctrlFixActivePower0.applySchedule(this.schedules.get("ctrlEssFixActivePower0"));
		// this.parent.ctrlEvcs0.applySchedule(this.schedules.get("ctrlEvcs0"));
		//
		// // EXPERIMENTS FROM HERE
		// var components =
		// this.parent.componentManager.getEnabledComponents().toArray(OpenemsComponent[]::new);
		// var forecast = generateForecast(this.parent.predictor,
		// this.parent.timeOfUseTariff);
		// var scheduler = this.parent.scheduler.getControllers().stream() //
		// .map(c -> this.parent.componentManager.getComponentOrNull(c)) //
		// .filter(Objects::nonNull) //
		// .toArray(Controller[]::new);
		// try {
		// ExecutionPlan ep = getBestExecutionPlan(forecast, components, scheduler);
		// ep.print();
		// } catch (OpenemsException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	// private Forecast generateForecast(PredictorManager predictor, TimeOfUseTariff
	// timeOfUseTariff) {
	// // TODO use 96 instead of 24 periods
	// var production = Utils.sumQuartersToHours(
	// predictor.get24HoursPrediction(new ChannelAddress("_sum",
	// "ProductionActivePower")).getValues());
	// var consumption = Utils.sumQuartersToHours(
	// predictor.get24HoursPrediction(new ChannelAddress("_sum",
	// "ConsumptionActivePower")).getValues());
	// // TODO should be UnmanagedConsumption
	// var buyFromGrid =
	// Utils.avgQuartersToHours(timeOfUseTariff.getPrices().getValues());
	// var sellToGrid = Collections.nCopies(buyFromGrid.length, 10F /* static feed
	// in tariff */)
	// .toArray(new Float[buyFromGrid.length]);
	// return new Forecast(production, consumption, buyFromGrid, sellToGrid);
	// }

	/**
	 * Gets a debug log message.
	 * 
	 * @return a message
	 */
	public String debugLog() {
		var schedules = this.schedules;
		if (schedules != null) {
			return this.schedules.debugLog();
		} else {
			return "";
		}
	}

}
