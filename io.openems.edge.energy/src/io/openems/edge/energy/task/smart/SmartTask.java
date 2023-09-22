package io.openems.edge.energy.task.smart;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.simulatable.Forecast;
import io.openems.edge.energy.api.simulatable.Simulatable;
import io.openems.edge.energy.task.AbstractEnergyTask;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.scheduler.api.Scheduler;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public class SmartTask extends AbstractEnergyTask {

	private final Logger log = LoggerFactory.getLogger(SmartTask.class);
	private final Scheduler scheduler;
	private final TimeOfUseTariff timeOfUseTariff;
	private final PredictorManager predictor;

	public SmartTask(ComponentManager componentManager, PredictorManager predictor, TimeOfUseTariff timeOfUseTariff,
			Scheduler scheduler, Consumer<Boolean> scheduleError) {
		super(componentManager, scheduleError);
		this.predictor = predictor;
		this.timeOfUseTariff = timeOfUseTariff;
		this.scheduler = scheduler;
	}

	@Override
	public void run() {
		// Schedulable Controllers in the order given by the Scheduler
		var forecast = generateForecast(this.predictor, this.timeOfUseTariff);
		var controllers = this.scheduler.getControllers().stream() //
				.map(c -> this.componentManager.getComponentOrNull(c)) //
				.filter(Objects::nonNull) //
				.toArray(Controller[]::new);
		var simulatables = this.componentManager.getEnabledComponentsOfType(Simulatable.class) //
				.toArray(Simulatable[]::new);

		var ep = Optimizer.getBestExecutionPlan(forecast, controllers, simulatables);
		ep.print();

		this.log.info("To be implemented...");
	}

	private static Forecast generateForecast(PredictorManager predictor, TimeOfUseTariff timeOfUseTariff) {
		// TODO use 96 instead of 24 periods
		var production = Utils.sumQuartersToHours(
				predictor.get24HoursPrediction(new ChannelAddress("_sum", "ProductionActivePower")).getValues());
		var consumption = Utils.sumQuartersToHours(
				predictor.get24HoursPrediction(new ChannelAddress("_sum", "ConsumptionActivePower")).getValues());
		// TODO should be UnmanagedConsumption
		var buyFromGrid = Utils.avgQuartersToHours(timeOfUseTariff.getPrices().getValues());
		var sellToGrid = Collections.nCopies(buyFromGrid.length, 10F /* static feed in tariff */)
				.toArray(new Float[buyFromGrid.length]);
		return new Forecast(production, consumption, buyFromGrid, sellToGrid);
	}
}
