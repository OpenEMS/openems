package io.openems.edge.energy.api.simulation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.clamp;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.periods.PeriodData;

public class EnergyFlow {

	private final int production;
	private final int excessProduction;
	private final int unmanagedConsumption;
	private final ImmutableSortedMap<String, Integer> managedConsumptions;
	private final int ess;
	private final int grid;

	private EnergyFlow(//
			int production, //
			int excessProduction, //
			int unmanagedConsumption, //
			ImmutableSortedMap<String, Integer> managedConsumptions, //
			int ess, //
			int grid) {
		this.production = production;
		this.excessProduction = excessProduction;
		this.unmanagedConsumption = unmanagedConsumption;
		this.managedConsumptions = managedConsumptions;
		this.ess = ess;
		this.grid = grid;
	}

	/**
	 * Returns the actual production after curtailment.
	 *
	 * @return the production value
	 */
	public int getProduction() {
		return this.production;
	}

	/**
	 * Returns the available production before curtailment.
	 *
	 * @return the available production
	 */
	public int getAvailableProduction() {
		return this.production + this.excessProduction;
	}

	/**
	 * Returns the excess, unused production.
	 *
	 * @return the excess production value
	 */
	public int getExcessProduction() {
		return this.excessProduction;
	}

	/**
	 * Returns the total consumption, which is the sum of unmanaged consumption and
	 * all managed consumptions.
	 *
	 * @return the total consumption value
	 */
	public int getConsumption() {
		return this.unmanagedConsumption//
				+ this.managedConsumptions.values().stream()//
						.mapToInt(Integer::intValue)//
						.sum();
	}

	/**
	 * Returns the unmanaged consumption.
	 *
	 * @return the unmanaged consumption value
	 */
	public int getUnmanagedConsumption() {
		return this.unmanagedConsumption;
	}

	/**
	 * Returns all managed consumptions.
	 *
	 * @return the managed consumptions
	 */
	public ImmutableSortedMap<String, Integer> getManagedConsumptions() {
		return this.managedConsumptions;
	}

	/**
	 * Returns the total managed consumption.
	 * 
	 * @return the total managed consumption value
	 */
	public int getManagedConsumption() {
		return this.managedConsumptions.values().stream()//
				.mapToInt(Integer::intValue)//
				.sum();
	}

	/**
	 * Returns the managed consumption for a given ID.
	 * 
	 * @param id an identifier, e.g., the component ID
	 * @return the managed consumption value, or 0 if not present
	 */
	public int getManagedConsumption(String id) {
		return this.managedConsumptions.getOrDefault(id, 0);
	}

	/**
	 * Returns the ess value (+: discharge, -: charge).
	 * 
	 * @return the ess value
	 */
	public int getEss() {
		return this.ess;
	}

	/**
	 * Returns the grid value (+: buy, -: sell).
	 *
	 * @return the grid value
	 */
	public int getGrid() {
		return this.grid;
	}

	@Override
	public String toString() {
		var managedConsStr = this.managedConsumptions.entrySet().stream()//
				.map(e -> e.getKey() + "=" + e.getValue())//
				.collect(Collectors.joining(", "));

		return toStringHelper(this)//
				.add("production", this.production)//
				.add("unmanagedConsumption", this.unmanagedConsumption)//
				.add("managedConsumptions", managedConsStr)//
				.add("ess", this.ess)//
				.add("grid", this.grid)//
				.toString();
	}

	/**
	 * Priority when accepting a constraint.
	 */
	public enum Curtailment {
		/** Accept constraints even if they require additional PV curtailment. */
		ALLOW,
		/** Preserve the currently maximum usable PV, including existing curtailment. */
		AVOID
	}

	/**
	 * Mutable energy flow for one period.
	 */
	public static class Model {

		private final int production;
		private final int unmanagedConsumption;
		private final Map<String, Integer> managedConsumptions;

		/* -: charge, +: discharge */
		private int essMin;
		private int essMax;

		/* -: sell, +: buy */
		private int gridMin;
		private int gridMax;

		private int consumption;

		public Model(//
				int production, //
				int unmanagedConsumption, //
				int essMaxCharge, //
				int essMaxDischarge, //
				int gridMaxBuy, //
				int gridMaxSell) {
			checkArgument(production >= 0, "production must not be negative");
			checkArgument(unmanagedConsumption >= 0, "unmanagedConsumption must not be negative");
			checkArgument(essMaxCharge >= 0 && essMaxDischarge >= 0, "ESS limits must not be negative");
			checkArgument(gridMaxBuy >= 0 && gridMaxSell >= 0, "Grid limits must not be negative");

			this.production = production;
			this.unmanagedConsumption = (int) min(unmanagedConsumption,
					(long) production + essMaxDischarge + gridMaxBuy);
			this.managedConsumptions = new HashMap<>();
			this.essMin = -essMaxCharge;
			this.essMax = essMaxDischarge;
			this.gridMin = -gridMaxSell;
			this.gridMax = gridMaxBuy;
			this.consumption = this.unmanagedConsumption;
		}

		private Model(Model other) {
			this.production = other.production;
			this.unmanagedConsumption = other.unmanagedConsumption;
			this.managedConsumptions = new HashMap<>(other.managedConsumptions);
			this.essMin = other.essMin;
			this.essMax = other.essMax;
			this.gridMin = other.gridMin;
			this.gridMax = other.gridMax;
			this.consumption = other.consumption;
		}

		/**
		 * Creates an {@link EnergyFlow.Model} based on the provided
		 * {@link GlobalScheduleContext} and {@link Period}.
		 * 
		 * @param gsc    the {@link GlobalScheduleContext}
		 * @param period the {@link Period}
		 * @return a new {@link EnergyFlow.Model}
		 */
		public static EnergyFlow.Model from(GlobalScheduleContext gsc, Period period) {
			final var essGlobal = gsc.goc.ess();
			final var essOne = gsc.ess;
			final var grid = gsc.goc.grid();

			final boolean isEssFull = gsc.ess.getInitialEnergy() == gsc.goc.ess().totalEnergy();

			return new EnergyFlow.Model(//
					/* production */ period.data().production(), //
					/* unmanagedConsumption */ period.data().consumption()//
							.map(PeriodData.Prediction::riskAdjusted)//
							.orElse(0), //
					/* essMaxCharge */ min(//
							period.duration().convertPowerToEnergy(essGlobal.maxChargePower()),
							essGlobal.totalEnergy() - essOne.getInitialEnergy()), //
					/* essMaxDischarge */ min(//
							period.duration().convertPowerToEnergy(essGlobal.maxDischargePower()),
							gsc.ess.getInitialEnergy()), //
					/* gridMaxBuy */ period.duration().convertPowerToEnergy(grid.maxBuyPower()), //
					/* gridMaxSell */ period.duration()
							.convertPowerToEnergy(isEssFull ? grid.maxSellPower() : grid.maxSellPowerWithBuffer()));
		}

		/**
		 * Creates a copy of an {@link EnergyFlow.Model}.
		 * 
		 * @param o the other {@link EnergyFlow.Model}
		 * @return a copy
		 */
		public static EnergyFlow.Model copyOf(EnergyFlow.Model o) {
			return new EnergyFlow.Model(o);
		}

		/**
		 * Raises the ESS lower bound without additional curtailment.
		 *
		 * @param target desired lower bound (negative: charge, positive: discharge)
		 * @return the accepted lower bound
		 */
		public int setEssGreaterOrEquals(int target) {
			return this.setEssGreaterOrEquals(target, Curtailment.AVOID);
		}

		/**
		 * Raises the ESS lower bound, preserving feasibility and commitments.
		 *
		 * @param target      desired lower bound
		 * @param curtailment whether additional PV curtailment is acceptable
		 * @return the accepted lower bound
		 */
		public int setEssGreaterOrEquals(int target, Curtailment curtailment) {
			final long maxAllowed = min(this.essMax,
					(long) this.consumption - this.requiredProduction(curtailment) - this.gridMin);
			this.essMin = (int) clamp(target, this.essMin, maxAllowed);
			return this.essMin;
		}

		/**
		 * Lowers the ESS upper bound without additional curtailment.
		 *
		 * @param target desired upper bound (negative: charge, positive: discharge)
		 * @return the accepted upper bound
		 */
		public int setEssLessOrEquals(int target) {
			final long minAllowed = max(this.essMin, (long) this.consumption - this.production - this.gridMax);
			this.essMax = (int) clamp(target, minAllowed, this.essMax);
			return this.essMax;
		}

		/**
		 * Raises the grid lower bound without additional curtailment.
		 *
		 * @param target desired lower bound (negative: export, positive: import)
		 * @return the accepted lower bound
		 */
		public int setGridGreaterOrEquals(int target) {
			return this.setGridGreaterOrEquals(target, Curtailment.AVOID);
		}

		/**
		 * Raises the grid lower bound, preserving feasibility and commitments.
		 *
		 * @param target      desired lower bound
		 * @param curtailment whether additional PV curtailment is acceptable
		 * @return the accepted lower bound
		 */
		public int setGridGreaterOrEquals(int target, Curtailment curtailment) {
			final long maxAllowed = min(this.gridMax,
					(long) this.consumption - this.requiredProduction(curtailment) - this.essMin);
			this.gridMin = (int) clamp(target, this.gridMin, maxAllowed);
			return this.gridMin;
		}

		/**
		 * Lowers the grid upper bound without additional curtailment.
		 *
		 * @param target desired upper bound (negative: export, positive: import)
		 * @return the accepted upper bound
		 */
		public int setGridLessOrEquals(int target) {
			final long minAllowed = max(this.gridMin, (long) this.consumption - this.production - this.essMax);
			this.gridMax = (int) clamp(target, minAllowed, this.gridMax);
			return this.gridMax;
		}

		/**
		 * Commits ESS energy without additional curtailment.
		 *
		 * @param target desired ESS energy (positive: discharge, negative: charge)
		 * @return the accepted, irrevocable ESS energy
		 */
		public int commitEss(int target) {
			return this.commitEss(target, Curtailment.AVOID);
		}

		/**
		 * Commits the nearest feasible ESS energy. Later requests cannot change it.
		 *
		 * @param target      desired ESS energy
		 * @param curtailment whether additional PV curtailment is acceptable
		 * @return the accepted, irrevocable ESS energy
		 */
		public int commitEss(int target, Curtailment curtailment) {
			final long minAllowed = max(this.essMin, (long) this.consumption - this.production - this.gridMax);
			final long maxAllowed = min(this.essMax,
					(long) this.consumption - this.requiredProduction(curtailment) - this.gridMin);
			final int value = (int) clamp(target, minAllowed, maxAllowed);
			this.essMin = value;
			this.essMax = value;
			return value;
		}

		/**
		 * Commits grid energy without additional curtailment.
		 *
		 * @param target desired grid energy (positive: import, negative: export)
		 * @return the accepted, irrevocable grid energy
		 */
		public int commitGrid(int target) {
			return this.commitGrid(target, Curtailment.AVOID);
		}

		/**
		 * Commits the nearest feasible grid energy. Later requests cannot change it.
		 *
		 * @param target      desired grid energy
		 * @param curtailment whether additional PV curtailment is acceptable
		 * @return the accepted, irrevocable grid energy
		 */
		public int commitGrid(int target, Curtailment curtailment) {
			final long minAllowed = max(this.gridMin, (long) this.consumption - this.production - this.essMax);
			final long maxAllowed = min(this.gridMax,
					(long) this.consumption - this.requiredProduction(curtailment) - this.essMin);
			final int value = (int) clamp(target, minAllowed, maxAllowed);
			this.gridMin = value;
			this.gridMax = value;
			return value;
		}

		private int requiredProduction(Curtailment curtailment) {
			return switch (requireNonNull(curtailment)) {
			case ALLOW -> 0;
			case AVOID -> this.usableProduction();
			};
		}

		private int usableProduction() {
			return (int) min(this.production, (long) this.consumption - this.essMin - this.gridMin);
		}

		/**
		 * Adds an irrevocable consumption, limited by remaining supply. Repeated IDs
		 * return their original allocation.
		 *
		 * @param id     identifier, e.g. component ID
		 * @param target nonnegative desired consumption
		 * @return the accepted consumption
		 */
		public int addManagedConsumption(String id, int target) {
			requireNonNull(id, "id must not be null");
			checkArgument(target >= 0, "target must not be negative");
			final var existing = this.managedConsumptions.get(id);
			if (existing != null) {
				return existing;
			}

			final long headroom = min((long) Integer.MAX_VALUE - this.consumption,
					(long) this.production + this.essMax + this.gridMax - this.consumption);
			final int accepted = (int) min(target, headroom);
			this.managedConsumptions.put(id, accepted);
			this.consumption += accepted;
			return accepted;
		}

		/**
		 * Returns the available production before curtailment.
		 *
		 * @return the available production in Wh
		 */
		public int getProduction() {
			return this.production;
		}

		/**
		 * Returns the total consumption, which is the sum of unmanaged consumption and
		 * all managed consumptions.
		 *
		 * @return the total consumption value
		 */
		public int getConsumption() {
			return this.consumption;
		}

		/**
		 * Returns the total managed consumption.
		 * 
		 * @return the total managed consumption value
		 */
		public int getManagedConsumption() {
			return this.consumption - this.unmanagedConsumption;
		}

		/**
		 * Returns the managed consumption for a given ID.
		 * 
		 * @param id an identifier, e.g., the component ID
		 * @return the managed consumption value, or 0 if not present
		 */
		public int getManagedConsumption(String id) {
			return this.managedConsumptions.getOrDefault(id, 0);
		}

		/**
		 * Returns the surplus (production - consumption).
		 * 
		 * @return the surplus value
		 */
		public int getSurplus() {
			return this.production - this.consumption;
		}

		/**
		 * Solves without modifying the model. Maximizes PV use, then minimizes absolute
		 * grid exchange within the remaining intervals.
		 * 
		 * @return an immutable, balanced energy flow
		 */
		public EnergyFlow solve() {
			final int actualProduction = this.usableProduction();
			final int ess = this.getEss();
			final int grid = (int) ((long) this.consumption - actualProduction - ess);

			return new EnergyFlow(//
					actualProduction, //
					this.production - actualProduction, //
					this.unmanagedConsumption, //
					ImmutableSortedMap.copyOf(this.managedConsumptions), //
					ess, //
					grid);
		}

		@VisibleForTesting
		int getEss() {
			final long residual = (long) this.consumption - this.usableProduction();
			final long minAllowed = max(this.essMin, residual - this.gridMax);
			final long maxAllowed = min(this.essMax, residual - this.gridMin);
			return (int) clamp(residual, minAllowed, maxAllowed);
		}

		@VisibleForTesting
		int getGrid() {
			return (int) ((long) this.consumption - this.usableProduction() - this.getEss());
		}
	}
}
