package io.openems.edge.energy.optimizer.app;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GocUtils.PeriodDuration;
import io.openems.edge.energy.optimizer.SimulationResult;

public class PlotUtils {

	private PlotUtils() {
	}

	public enum PlotSettings {
		DISABLE, //
		GLOBAL_OPTIMIZATION_CONTEXT, //
		SIMULATION_RESULT, //
	}

	private static final Color COLOR_ESS_CHARGE = new Color(14, 190, 84);
	private static final Color COLOR_ESS_DISCHARGE = new Color(255, 98, 63);
	private static final Color COLOR_ESS_SOC = new Color(189, 195, 199);
	private static final Color COLOR_GRID_BUY = new Color(77, 106, 130);
	private static final Color COLOR_PRICE = new Color(255, 153, 0);
	private static final Color COLOR_GRID_SELL = new Color(91, 92, 214);
	private static final Color COLOR_PROD = new Color(54, 174, 209);
	private static final Color COLOR_CONS = new Color(255, 206, 0);

	private static final Map<String, Color> ESS_MODE_COLORS = Map.of(//
			"BALANCING", new Color(51, 102, 0), //
			"CHARGE_GRID", new Color(0, 204, 204), //
			"PEAK_SHAVING", new Color(218, 120, 8), //
			"DELAY_DISCHARGE", new Color(0, 0, 0));

	protected static void plotGlobalOptimizationContext(GlobalOptimizationContext goc) {
		final var domainAxis = new DateAxis("Time");
		domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));

		final var consumptionSeries = new XYSeries("Consumption");
		final var consumptionAdjustedSeries = new XYSeries("Consumption (adjusted)");
		final var priceSeries = new XYSeries("Price");
		final var priceNormalizedSeries = new XYSeries("Price (normalized)");

		for (var p : goc.periods().stream().toList()) {
			final long t = p.time().toInstant().toEpochMilli();
			final IntFunction<Integer> toPower = p.duration()::convertEnergyToPower;

			if (p instanceof Period.WithPrediction wp) {
				consumptionSeries.add(t, toPower.apply(wp.prediction().consumptionPredicted()));
				consumptionAdjustedSeries.add(t, toPower.apply(wp.prediction().consumptionRiskAdjusted()));
			}
			if (p instanceof Period.WithPrice wp) {
				priceSeries.add(t, wp.price().actual() / 10.);
				priceNormalizedSeries.add(t, wp.price().normalized());
			}
		}

		final var axisPower = new NumberAxis("Power [W]");
		final var powerMinMax = getGlobalMinMax(consumptionSeries, consumptionAdjustedSeries);
		axisPower.setRange(powerMinMax.min(), powerMinMax.max() * 1.05);

		final var axisPrice = new NumberAxis("Price [ct/kWh]");

		final var axisPriceNormalized = new NumberAxis("Price (normalized)");
		axisPriceNormalized.setRange(0., 1.05);

		final var subplots = List.of(
				// Consumption Plot
				new XyPlotBuilder()//
						.addDataset(axisPower, ds -> {
							ds.addSeries(consumptionSeries, COLOR_CONS);
							ds.addSeries(consumptionAdjustedSeries, COLOR_CONS, true);
						})//
						.build(), //

				// Price Plot
				new XyPlotBuilder()//
						.addDataset(axisPrice, ds -> {
							ds.addSeries(priceSeries, COLOR_PRICE);
						}, true)//
						.addDataset(axisPriceNormalized, ds -> {
							ds.addSeries(priceNormalizedSeries, COLOR_PRICE, true);
						}, true)//
						.build());

		final var combinedPlot = combine(domainAxis, subplots);

		final var chart = new JFreeChart("GlobalOptimizationContext", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot,
				true);
		showChartInJFrame(new ChartPanel(chart), "GlobalOptimizationContext");
	}

	protected static void plotSimulationResult(GlobalOptimizationContext goc, SimulationResult sr) {
		final var essCapacity = goc.ess().totalEnergy() / 100f;
		final IntFunction<Integer> toPower = PeriodDuration.QUARTER::convertEnergyToPower;

		final var domainAxis = new DateAxis("Time");
		domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));

		final var productionSeries = new XYSeries("Production");
		final var consumptionSeries = new XYSeries("Consumption");
		final var essChargeSeries = new XYSeries("ESS Charge");
		final var essDischargeSeries = new XYSeries("ESS Discharge");
		final var essSocSeries = new XYSeries("SoC");
		final var gridBuySeries = new XYSeries("Grid Buy");
		final var gridSellSeries = new XYSeries("Grid Sell");
		final var priceSeries = new XYSeries("Price");

		for (var entry : sr.periods().entrySet()) {
			final long t = entry.getKey().toInstant().toEpochMilli();

			final var p = entry.getValue();
			final var ef = p.energyFlow();

			final int prod = toPower.apply(ef.getProduction());
			final int cons = toPower.apply(ef.getConsumption());
			final int ess = toPower.apply(ef.getEss());
			final int grid = toPower.apply(ef.getGrid());

			productionSeries.add(t, prod);
			consumptionSeries.add(t, cons);

			essChargeSeries.add(t, ess < 0 ? -ess : 0);
			essDischargeSeries.add(t, ess > 0 ? ess : 0);

			essSocSeries.add(t, p.essInitialEnergy() / essCapacity);

			gridBuySeries.add(t, grid > 0 ? grid : 0);
			gridSellSeries.add(t, grid < 0 ? -grid : 0);

			if (p.period() instanceof Period.WithPrice wp) {
				priceSeries.add(t, wp.price().actual() / 10.);
			}
		}

		final var axisPower = new NumberAxis("Power [W]");
		final var powerMinMax = getGlobalMinMax(essChargeSeries, essDischargeSeries, gridBuySeries, gridSellSeries,
				productionSeries, consumptionSeries);
		axisPower.setRange(powerMinMax.min(), powerMinMax.max() * 1.05);

		final var axisSoC = new NumberAxis("SoC [%]");
		axisSoC.setRange(0., 100. * 1.05);

		final var axisPrice = new NumberAxis("Price [ct/kWh]");

		final var subplots = List.of(
				// ESS Plot
				new XyPlotBuilder()//
						.addDataset(axisPower, ds -> {
							ds.addSeries(essChargeSeries, COLOR_ESS_CHARGE);
							ds.addSeries(essDischargeSeries, COLOR_ESS_DISCHARGE);
						})//
						.addDataset(axisSoC, ds -> {
							ds.addSeries(essSocSeries, COLOR_ESS_SOC, true);
						})//
						.build(), //

				// Grid Buy Plot
				new XyPlotBuilder()//
						.addDataset(axisPower, ds -> {
							ds.addSeries(gridBuySeries, COLOR_GRID_BUY);
						})//
						.addDataset(axisPrice, ds -> {
							ds.addSeries(priceSeries, COLOR_PRICE);
						}, true)//
						.build(), //

				// Mode Plot
				buildModePlot(sr, ESS_MODE_COLORS), //

				// Prod Cons Plot
				new XyPlotBuilder()//
						.addDataset(axisPower, ds -> {
							ds.addSeries(productionSeries, COLOR_PROD);
							ds.addSeries(consumptionSeries, COLOR_CONS);
						})//
						.build(), //

				// Grid Sell Plot
				new XyPlotBuilder()//
						.addDataset(axisPower, ds -> {
							ds.addSeries(gridSellSeries, COLOR_GRID_SELL);
						})//
						.build());

		final var combinedPlot = combine(domainAxis, subplots);

		final var chart = new JFreeChart("Simulation Result", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
		showChartInJFrame(new ChartPanel(chart), "Simulation Result");
	}

	private static class XyPlotBuilder {

		private final XYPlot plot = new XYPlot();
		private int datasetIndex = 0;

		public XyPlotBuilder addDataset(NumberAxis axis, Consumer<DatasetBuilder> config) {
			return this.addDataset(axis, config, false);
		}

		public XyPlotBuilder addDataset(NumberAxis axis, Consumer<DatasetBuilder> config, boolean stepped) {
			final var ds = new XYSeriesCollection();
			final var renderer = stepped //
					? new XYStepRenderer() //
					: new XYLineAndShapeRenderer(true, false);

			final var builder = new DatasetBuilder(ds, renderer);
			config.accept(builder);

			this.plot.setDataset(this.datasetIndex, ds);
			this.plot.setRenderer(this.datasetIndex, renderer);
			this.plot.setRangeAxis(this.datasetIndex, axis);
			this.plot.mapDatasetToRangeAxis(this.datasetIndex, this.datasetIndex);

			this.datasetIndex++;
			return this;
		}

		public XYPlot build() {
			return this.plot;
		}

		public static class DatasetBuilder {

			private final XYSeriesCollection ds;
			private final XYLineAndShapeRenderer renderer;

			public DatasetBuilder(XYSeriesCollection ds, XYLineAndShapeRenderer renderer) {
				this.ds = ds;
				this.renderer = renderer;
			}

			public DatasetBuilder addSeries(XYSeries series, Color color) {
				return this.addSeries(series, color, false);
			}

			public DatasetBuilder addSeries(XYSeries series, Color color, boolean dashed) {
				final int idx = this.ds.getSeriesCount();
				this.ds.addSeries(series);

				this.renderer.setSeriesPaint(idx, color);

				if (dashed) {
					this.renderer.setSeriesStroke(idx, new BasicStroke(//
							2.0f, //
							BasicStroke.CAP_BUTT, //
							BasicStroke.JOIN_BEVEL, //
							0, new float[] { 5.0f, 5.0f }, 0));
				} else {
					this.renderer.setSeriesStroke(idx, new BasicStroke(2.0f));
				}

				return this;
			}
		}
	}

	private record MinMax(double min, double max) {
	}

	private static MinMax getGlobalMinMax(XYSeries... series) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;

		for (var s : series) {
			for (int i = 0; i < s.getItemCount(); i++) {
				double y = s.getY(i).doubleValue();
				min = Math.min(min, y);
				max = Math.max(max, y);
			}
		}

		if (!Double.isFinite(min)) {
			min = 0;
		}
		if (!Double.isFinite(max)) {
			max = 1;
		}
		if (min == max) {
			max = min + 1;
		}

		return new MinMax(min, max);
	}

	private static XYPlot buildModePlot(SimulationResult sr, Map<String, Color> modeColors) {
		final var seriesMap = new HashMap<String, XYSeries>();
		modeColors.keySet().forEach(m -> seriesMap.put(m, new XYSeries((m))));

		for (var entry : sr.periods().entrySet()) {
			final var time = entry.getKey();
			final var t = time.toInstant().toEpochMilli();

			String modeName = null;
			for (var schEntry : sr.schedules().entrySet()) {
				final var schedule = schEntry.getValue();
				final var transition = schedule.get(time);
				if (transition != null) {
					modeName = schEntry.getKey().modes().getAsString(transition.modeIndex());
					break;
				}
			}

			for (var e : seriesMap.entrySet()) {
				e.getValue().add(t, e.getKey().equals(modeName) ? 1. : 0.);
			}
		}

		final var dataset = new XYSeriesCollection();
		seriesMap.values().forEach(dataset::addSeries);

		final var barDataset = new XYBarDataset(dataset, Duration.ofMinutes(15).toMillis());

		final var renderer = new XYBarRenderer();
		renderer.setBarPainter(new StandardXYBarPainter());
		renderer.setShadowVisible(false);

		int idx = 0;
		for (String m : modeColors.keySet()) {
			renderer.setSeriesPaint(idx++, modeColors.get(m));
		}

		final var axis = new NumberAxis();
		axis.setRange(0., 1.);
		axis.setVisible(false);

		return new XYPlot(barDataset, null, axis, renderer);
	}

	private static CombinedDomainXYPlot combine(ValueAxis domainAxis, List<XYPlot> subplots) {
		final var cp = new CombinedDomainXYPlot(domainAxis);
		subplots.forEach(cp::add);
		cp.setGap(10.0);
		return cp;
	}

	private static void showChartInJFrame(ChartPanel chartPanel, String title) {
		final var frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setContentPane(chartPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
