package io.openems.edge.energy.optimizer.app;

import static io.openems.common.utils.FunctionUtils.doNothing;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.function.IntFunction;

import io.openems.edge.common.test.Plot;
import io.openems.edge.common.test.Plot.AxisFormat;
import io.openems.edge.common.test.Plot.Data;
import io.openems.edge.common.test.Plot.Line;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GocUtils.PeriodDuration;
import io.openems.edge.energy.optimizer.SimulationResult;

public class PlotUtils {

	private PlotUtils() {
	}

	public static enum PlotSettings {
		DISABLE, //
		GLOBAL_OPTIMIZATION_CONTEXT_ALL, //
		SIMULATION_RESULT_ALL, SIMULATION_RESULT_POWER, SIMULATION_RESULT_BATTERY, SIMULATION_RESULT_PRICE;
	}

	private static final String AXIS_POWER = "W";
	private static final String AXIS_PERCENTAGE = "%";
	private static final String AXIS_MONEY = "€";

	protected static void plotGlobalOptimizationContext(PlotSettings plotSettings, GlobalOptimizationContext goc)
			throws IOException {
		switch (plotSettings) {
		case GLOBAL_OPTIMIZATION_CONTEXT_ALL:
			break;
		default:
			break;
		}

		final var periods = goc.periods().stream().toArray(GlobalOptimizationContext.Period[]::new);
		final var plot = initializePlot(periods.length);

		Data consumptionPredicted = Plot.data();
		Data consumptionRiskAdjusted = Plot.data();
		Data gridCostActual = Plot.data();
		Data gridCostNormalized = Plot.data();

		for (var i = 0; i < periods.length; i++) {
			final var p = periods[i];
			final IntFunction<Integer> toPower = p.duration()::convertEnergyToPower;

			if (p instanceof Period.WithPrediction wp) {
				var pp = wp.prediction();
				consumptionPredicted.xy(i, toPower.apply(pp.consumptionPredicted()));
				consumptionRiskAdjusted.xy(i, toPower.apply(pp.consumptionRiskAdjusted()));
			}
			if (p instanceof Period.WithPrice wp) {
				var pp = wp.price();
				gridCostActual.xy(i, pp.actual());
				gridCostNormalized.xy(i, pp.normalized() * 100);
			}
		}

		plot //
				.series("Cons Predicted", consumptionPredicted, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(255, 206, 0))) //
				.series("Cons Adjusted", consumptionRiskAdjusted, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(255, 206, 200))) //
				.series("Cost Actual", gridCostActual, Plot.seriesOpts() //
						.yAxis(AXIS_MONEY) //
						.line(Line.DASHED) //
						.lineDash(new float[] { 1.0f, 10.0f }) //
						.color(Color.BLACK)) //
				.series("Cost Normalized", gridCostNormalized, Plot.seriesOpts() //
						.yAxis(AXIS_PERCENTAGE) //
						.line(Line.SOLID) //
						.color(Color.GRAY));

		saveAndOpenFile(plot, "GlobalOptimizationContext");
	}

	protected static void plotSimulationResult(PlotSettings plotChart, GlobalOptimizationContext goc,
			SimulationResult sr) throws IOException {
		switch (plotChart) {
		case SIMULATION_RESULT_ALL, SIMULATION_RESULT_BATTERY, SIMULATION_RESULT_POWER, SIMULATION_RESULT_PRICE:
			break;
		default:
			return;
		}

		final var periods = sr.periods().values().stream().toArray(SimulationResult.Period[]::new);
		final var essCapacity = goc.ess().totalEnergy() / 100f;
		final IntFunction<Integer> toPower = PeriodDuration.QUARTER::convertEnergyToPower;
		final var plot = initializePlot(periods.length);

		Data production = Plot.data();
		Data consumption = Plot.data();
		Data essCharge = Plot.data();
		Data essDischarge = Plot.data();
		Data essSoc = Plot.data();
		Data gridBuy = Plot.data();
		Data gridSell = Plot.data();
		Data gridCost = Plot.data();

		for (var i = 0; i < periods.length; i++) {
			final var p = periods[i];
			final var ef = p.energyFlow();

			production.xy(i, toPower.apply(ef.getProduction()));
			consumption.xy(i, toPower.apply(ef.getConsumption()));
			essCharge.xy(i, -Math.min(0, toPower.apply(ef.getEss())));
			essDischarge.xy(i, Math.max(0, toPower.apply(ef.getEss())));
			essSoc.xy(i, p.essInitialEnergy() / essCapacity);
			gridBuy.xy(i, Math.max(0, toPower.apply(ef.getGrid())));
			gridSell.xy(i, -Math.min(0, toPower.apply(ef.getGrid())));
			gridCost.xy(i, switch (p.period()) {
			case Period.WithPrice wp -> wp.price().actual() / 10.;
			default -> 0;
			});
		}

		switch (plotChart) {
		case SIMULATION_RESULT_ALL, SIMULATION_RESULT_POWER -> plot //
				.series("Production", production, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(54, 174, 209))) //
				.series("Consumption", consumption, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(255, 206, 0))) //
				.series("ESS Charge", essCharge, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(14, 190, 84))) //
				.series("ESS Discharge", essDischarge, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(255, 98, 63))) //
				.series("Grid Buy", gridBuy, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(77, 106, 130))) //
				.series("Grid Sell", gridSell, Plot.seriesOpts() //
						.yAxis(AXIS_POWER) //
						.color(new Color(91, 92, 214)));
		default -> doNothing();
		}

		switch (plotChart) {
		case SIMULATION_RESULT_ALL, SIMULATION_RESULT_BATTERY -> plot //
				.series("SoC", essSoc, Plot.seriesOpts() //
						.yAxis(AXIS_PERCENTAGE) //
						.line(Line.DASHED) //
						.color(new Color(189, 195, 199)));
		default -> doNothing();
		}

		switch (plotChart) {
		case SIMULATION_RESULT_ALL, SIMULATION_RESULT_PRICE -> plot //
				.series("Cost", gridCost, Plot.seriesOpts() //
						.yAxis(AXIS_MONEY) //
						.line(Line.DASHED) //
						.lineDash(new float[] { 1.0f, 10.0f }) //
						.color(Color.BLACK));
		default -> doNothing();
		}

		saveAndOpenFile(plot, "SimulationResult");
	}

	private static Plot initializePlot(int length) {
		return Plot.plot(//
				Plot.plotOpts() //
						.legend(Plot.LegendFormat.BOTTOM) //
						.gridColor(Color.WHITE)) //
				.xAxis("t", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT) //
						.range(0, length - 1)) //
				.yAxis(AXIS_POWER, Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT)) //
				.yAxis(AXIS_MONEY, Plot.axisOpts()) //
				.yAxis(AXIS_PERCENTAGE, Plot.axisOpts() //
						.range(0, 100));
	}

	private static void saveAndOpenFile(Plot plot, String filename) throws IOException {
		plot.save(filename, "png");
		Desktop.getDesktop().open(new File(filename + ".png"));
	}
}
