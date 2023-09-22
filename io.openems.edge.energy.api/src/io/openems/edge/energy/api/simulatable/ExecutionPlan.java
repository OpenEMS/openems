package io.openems.edge.energy.api.simulatable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.test.Plot;
import io.openems.edge.common.test.Plot.Data;
import io.openems.edge.energy.api.schedulable.Schedulable;
import io.openems.edge.energy.api.schedulable.Schedule;

/**
 * Holds one specific Execution Plan for {@link Schedulable} Controllers.
 */
public class ExecutionPlan {

	public static final int NO_OF_PERIODS = 24;
	// public static final int NO_OF_PERIODS = 96;

	private final Logger log = LoggerFactory.getLogger(ExecutionPlan.class);

	public static class Builder {
		private final Period[] periods;

		private Builder(Forecast forecast) {
			this.periods = IntStream.range(0, NO_OF_PERIODS) //
					.mapToObj(p -> new Period(p, forecast.getPeriod(p))) //
					.toArray(Period[]::new);
		}

		/**
		 * Add a Component with Presets.
		 * 
		 * @param componentId the Component-ID
		 * @param presets     the presets
		 * @return builder
		 */
		public Builder add(String componentId, Schedule.Preset[] presets) {
			IntStream.range(0, Math.min(NO_OF_PERIODS, presets.length)) //
					.forEach(p -> this.periods[p].presets.put(componentId, presets[p]));
			return this;
		}

		public ExecutionPlan build() {
			return new ExecutionPlan(this.periods);
		}
	}

	/**
	 * Create a builder.
	 *
	 * @param forecast the {@link Forecast}
	 * @return a {@link Builder}
	 */
	public static Builder create(Forecast forecast) {
		return new Builder(forecast);
	}

	public static class Period {
		public final int index;
		public final Forecast.Period forecast;

		private final Map<String, Schedule.Preset> presets = new HashMap<>();
		private final List<String> logs = new ArrayList<>();
		private final Map<String, Float> values = new HashMap<>();

		private Integer storage = null; // [Wh]; positive discharge; negative charge
		private Integer managedConsumption = null; // [Wh]

		protected Period(int index, Forecast.Period forecast) {
			this.index = index;
			this.forecast = forecast;
		}

		protected void addLog(String log) {
			this.logs.add(log);
		}

		protected void setValue(String key, float value) {
			this.values.put(key, value);
		}

		protected float getValue(String key) {
			return this.values.get(key);
		}

		public Integer getStorage() {
			return this.storage;
		}

		protected int getStorageOrZero() {
			if (this.storage == null) {
				return 0;
			}
			return this.storage;
		}

		public void setStorage(String componentId, Integer value) {
			this.storage = value;
		}

		protected Integer getGridEnergy() {
			var production = this.forecast.production;
			var consumption = this.forecast.consumption;
			if (production == null || consumption == null) {
				return null;
			}
			var result = consumption - production;
			if (this.storage != null) {
				result -= this.storage;
			}
			if (this.managedConsumption != null) {
				result += this.managedConsumption;
			}
			return result;
		}

		public Float getGridCost() {
			return this.forecast.getGridCost(this.getGridEnergy());
		}

		@SuppressWarnings("unchecked")
		protected <PRESET extends Schedule.Preset> PRESET getPreset(String componentId) {
			return (PRESET) this.presets.get(componentId);
		}
	}

	private final Period[] periods;

	private ExecutionPlan(Period[] periods) {
		this.periods = periods;
	}

	/**
	 * Provides a {@link Stream} of {@link Period}s.
	 * 
	 * @return periods
	 */
	public Stream<Period> periods() {
		return Stream.of(this.periods);
	}

	protected Period getPeriod(int p) throws IllegalArgumentException {
		if (p < NO_OF_PERIODS) {
			return this.periods[p];
		}
		throw new IllegalArgumentException("Period index [" + p + "] must be smaller than [" + NO_OF_PERIODS + "]");
	}

	// public int getManagedConsumptionOrZero(int p) {
	// if (this.managedConsumption.length > p) {
	// return this.managedConsumption[p];
	// }
	// return 0;
	// }

	public Double getTotalGridCost() {
		return this.periods() //
				.map(p -> p.getGridCost()) //
				.filter(Objects::nonNull) //
				.mapToDouble(p -> p) //
				.sum();
	}

	/**
	 * Nicely print the {@link ExecutionPlan}.
	 */
	public void print() {
		this.log.info(String.format("   %10s %10s %10s %10s %10s %10s %-40s %s", "Product.", "Consumpt.", "Storage",
				"Grid", "Grid-Price", "Grid-Cost", "Presets", "Logs"));
		this.periods().forEach(p -> {
			this.log.info(String.format("%2d %10d %10d %10d %10d %10.0f %10.0f %-40s %s", p.index,
					p.forecast.production, p.forecast.consumption, p.getStorage(), p.getGridEnergy(),
					p.forecast.buyFromGridCost, p.getGridCost(),
					p.presets.values().stream().map(Schedule.Preset::name).collect(Collectors.joining(",")),
					p.logs.stream().collect(Collectors.joining(","))));
		});
		this.log.info("Total Cost: " + this.getTotalGridCost());
	}

	protected void plot() {
		Data production = Plot.data();
		Data consumption = Plot.data();
		Data storageCharge = Plot.data();
		Data storageDischarge = Plot.data();
		Data storageSoc = Plot.data();
		Data gridBuy = Plot.data();
		Data gridSell = Plot.data();
		this.periods().forEach(p -> {
			var x = p.index;
			production.xy(x, p.forecast.production);
			consumption.xy(x, p.forecast.consumption);
			var storage = p.getStorage();
			if (storage != null) {
				if (storage > 0) {
					storageCharge.xy(x, 0);
					storageDischarge.xy(x, storage);
				} else {
					storageCharge.xy(x, storage * -1);
					storageDischarge.xy(x, 0);
				}
			}
			storageSoc.xy(x, p.getValue("ess/Soc") * 100);
			var grid = p.getGridEnergy();
			if (grid > 0) {
				gridBuy.xy(x, grid);
				gridSell.xy(x, 0);
			} else {
				gridBuy.xy(x, 0);
				gridSell.xy(x, grid * -1);
			}
		});
	}

	// public int calculateRevenue(Facts facts) {
	// var forecast = facts.forecast;
	//
	// // Calculate Revenue from Sell-To-Grid/Buy-From-Grid
	// var revenueGrid = Math.round(IntStream.range(0, Facts.PLANNING_PERIODS) //
	// .map(p -> {
	// var storage = this.periods[p].storage.energyPerPeriod;
	// var production = forecast.periods[p].production;
	// var consumption = forecast.periods[p].consumption;
	// var grid = consumption - storage - production;
	// final int revenue;
	// if (grid < 0) {
	// revenue = -1 * grid * forecast.periods[0].sellToGridRevenue;
	// } else {
	// revenue = -1 * grid * forecast.periods[0].buyFromGridCost;
	// }
	// this.periods[p].revenue = revenue;
	// return revenue;
	// }) //
	// .sum() //
	// // Standardize to interval by factor
	// / facts.revenueStandardizationFactor);
	//
	// // Calculate cost of too missing the target final energy in storage
	// var finalEnergy = this.periods[this.periods.length -
	// 1].storage.finalEnergy();
	// var costTargetEnergy = Math.round(Math.abs(finalEnergy -
	// facts.targetStorageEnergyInFinalPeriod));
	//
	// // Cost should avoid delta in wrong direction
	//
	// return revenueGrid - costTargetEnergy;
	// }
}
