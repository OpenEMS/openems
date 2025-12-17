package io.openems.edge.energy.api.simulation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;

public class EnergyFlow {

	private final int production;
	private final int unmanagedConsumption;
	private final ImmutableSortedMap<String, Integer> managedConsumptions;
	private final int ess;
	private final int grid;

	private EnergyFlow(//
			int production, //
			int unmanagedConsumption, //
			ImmutableSortedMap<String, Integer> managedConsumptions, //
			int ess, //
			int grid) {
		this.production = production;
		this.unmanagedConsumption = unmanagedConsumption;
		this.managedConsumptions = managedConsumptions;
		this.ess = ess;
		this.grid = grid;
	}

	/**
	 * Returns the production.
	 *
	 * @return the production value
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
		return this.unmanagedConsumption //
				+ this.managedConsumptions.values().stream() //
						.mapToInt(Integer::intValue) //
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
		return this.managedConsumptions.values().stream() //
				.mapToInt(Integer::intValue) //
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

	public static class Model {

		private final int production;
		private final int unmanagedConsumption;

		/* -: charge, +: discharge */
		private Integer ess;
		private int essMaxCharge;
		private int essMaxDischarge;

		/* -: sell, +: buy */
		private Integer grid;
		private int gridMaxBuy;
		private int gridMaxSell;

		private int consumption;
		private int surplus;

		private final Map<String, Integer> managedConsumptions;

		private State state;

		public Model(//
				int production, //
				int unmanagedConsumption, //
				int essMaxCharge, //
				int essMaxDischarge, //
				int gridMaxBuy, //
				int gridMaxSell) throws OpenemsException {
			this.production = production;
			this.unmanagedConsumption = unmanagedConsumption;

			this.ess = null;
			this.essMaxCharge = essMaxCharge;
			this.essMaxDischarge = essMaxDischarge;

			this.grid = null;
			this.gridMaxBuy = gridMaxBuy;
			this.gridMaxSell = gridMaxSell;

			this.consumption = this.unmanagedConsumption;
			this.surplus = this.production - this.consumption;

			this.managedConsumptions = new HashMap<>();

			this.state = State.UNSET;

			// Check that initial setup is solvable
			int minPossibleSurplus = -this.essMaxDischarge - this.gridMaxBuy;
			int maxPossibleSurplus = this.essMaxCharge + this.gridMaxSell;
			if (this.surplus < minPossibleSurplus || this.surplus > maxPossibleSurplus) {
				throw new OpenemsException("Initial setup not solvable");
			}
		}

		/**
		 * Creates an {@link EnergyFlow.Model} based on the provided
		 * {@link GlobalScheduleContext} and {@link GlobalOptimizationContext.Period}.
		 * 
		 * @param gsc    the {@link GlobalScheduleContext}
		 * @param period the {@link GlobalOptimizationContext.Period}
		 * @return a new {@link EnergyFlow.Model}
		 * @throws OpenemsException if initial setup not solvable
		 */
		public static EnergyFlow.Model from(GlobalScheduleContext gsc, GlobalOptimizationContext.Period period)
				throws OpenemsException {
			final var essGlobal = gsc.goc.ess();
			final var essOne = gsc.ess;
			final var grid = gsc.goc.grid();

			return new EnergyFlow.Model(//
					/* production */ switch (period) {
					case Period.WithPrediction p -> p.production();
					default -> 0;
					}, //
					/* unmanagedConsumption */ switch (period) {
					case Period.WithPrediction p -> p.consumption();
					default -> 0;
					}, //
					/* essMaxCharge */ min(//
							period.duration().convertPowerToEnergy(essGlobal.maxChargePower()),
							essGlobal.totalEnergy() - essOne.getInitialEnergy()), //
					/* essMaxDischarge */ min(//
							period.duration().convertPowerToEnergy(essGlobal.maxDischargePower()),
							gsc.ess.getInitialEnergy()), //
					/* gridMaxBuy */ period.duration().convertPowerToEnergy(grid.maxBuyPower()), //
					/* gridMaxSell */ period.duration().convertPowerToEnergy(grid.maxSellPower()));
		}

		/**
		 * Sets the maximum allowed ESS charge, adjusting if necessary to satisfy
		 * constraints.
		 *
		 * @param target the desired maximum ESS charge
		 * @return the actual maximum ESS charge that was set
		 */
		public int setEssMaxCharge(int target) {
			checkArgument(target >= 0, "target must not be negative");

			if (target >= this.essMaxCharge) {
				return this.essMaxCharge;
			}

			int minRequiredCharge = switch (this.state) {
			case UNSET -> max(0, this.surplus - this.gridMaxSell);
			case ESS_SET, GRID_SET -> -this.ess;
			};

			this.essMaxCharge = max(target, minRequiredCharge);
			return this.essMaxCharge;
		}

		/**
		 * Sets the maximum allowed ESS discharge, adjusting if necessary to satisfy
		 * constraints.
		 * 
		 * @param target the desired maximum ESS discharge
		 * @return the actual maximum ESS discharge that was set
		 */
		public int setEssMaxDischarge(int target) {
			checkArgument(target >= 0, "target must not be negative");

			if (target >= this.essMaxDischarge) {
				return this.essMaxDischarge;
			}

			int minRequiredDischarge = switch (this.state) {
			case UNSET -> max(0, -this.surplus - this.gridMaxBuy);
			case ESS_SET, GRID_SET -> this.ess;
			};

			this.essMaxDischarge = max(target, minRequiredDischarge);
			return this.essMaxDischarge;
		}

		/**
		 * Sets the maximum allowed grid sell, adjusting if necessary to satisfy
		 * constraints.
		 *
		 * @param target the desired maximum grid sell
		 * @return the actual maximum grid sell that was set
		 */
		public int setGridMaxSell(int target) {
			checkArgument(target >= 0, "target must not be negative");

			if (target >= this.gridMaxSell) {
				return this.gridMaxSell;
			}

			int minRequiredSell = switch (this.state) {
			case UNSET -> max(0, this.surplus - this.essMaxCharge);
			case ESS_SET, GRID_SET -> -this.grid;
			};

			this.gridMaxSell = max(target, minRequiredSell);
			return this.gridMaxSell;
		}

		/**
		 * Sets the maximum allowed grid buy, adjusting if necessary to satisfy
		 * constraints.
		 *
		 * @param target the desired maximum grid buy
		 * @return the actual maximum grid buy that was set
		 */
		public int setGridMaxBuy(int target) {
			checkArgument(target >= 0, "target must not be negative");

			if (target >= this.gridMaxBuy) {
				return this.gridMaxBuy;
			}

			int minRequiredBuy = switch (this.state) {
			case UNSET -> max(0, -this.surplus - this.essMaxDischarge);
			case ESS_SET, GRID_SET -> this.grid;
			};

			this.gridMaxBuy = max(target, minRequiredBuy);
			return this.gridMaxBuy;
		}

		/**
		 * Adds a managed consumption for the specified ID, adjusted if necessary to
		 * satisfy constraints.
		 *
		 * @param id     the identifier, e.g., the component ID
		 * @param target the desired consumption value
		 * @return the actual managed consumption that was set
		 */
		public int addManagedConsumption(String id, int target) {
			checkArgument(target >= 0, "target must not be negative");

			if (this.managedConsumptions.containsKey(id)) {
				return this.managedConsumptions.get(id);
			}

			int maxPossibleManagedConsumption = switch (this.state) {
			case UNSET -> this.surplus + this.essMaxDischarge + this.gridMaxBuy;
			case ESS_SET -> this.surplus + this.ess + this.gridMaxBuy;
			case GRID_SET -> this.surplus + this.essMaxDischarge + this.grid;
			};

			int actualManagedConsumption = min(target, maxPossibleManagedConsumption);

			this.managedConsumptions.put(id, actualManagedConsumption);
			this.consumption += actualManagedConsumption;
			this.surplus = this.production - this.consumption;

			switch (this.state) {
			case UNSET -> doNothing();
			case ESS_SET -> this.grid = -(this.surplus + this.ess);
			case GRID_SET -> this.ess = -(this.surplus + this.grid);
			}

			return actualManagedConsumption;
		}

		/**
		 * Sets the ess value (+: discharge, -: charge), adjusted if necessary to
		 * satisfy constraints.
		 *
		 * @param target the desired ess value
		 * @return the actual ess value that was set
		 */
		public int setEss(int target) {
			switch (this.state) {
			case UNSET -> {
				int maxPossibleCharge = min(this.essMaxCharge, this.surplus + this.gridMaxBuy);
				int maxPossibleDischarge = min(this.essMaxDischarge, -this.surplus + this.gridMaxSell);
				this.ess = fitWithin(-maxPossibleCharge, maxPossibleDischarge, target);

				this.grid = -(this.surplus + this.ess);
				this.state = State.ESS_SET;
			}
			case ESS_SET, GRID_SET -> doNothing();
			}

			return this.ess;
		}

		/**
		 * Sets the grid value (+: buy, -: sell), adjusted if necessary to satisfy
		 * constraints.
		 *
		 * @param target the desired grid value
		 * @return the actual grid value that was set
		 */
		public int setGrid(int target) {
			switch (this.state) {
			case UNSET -> {
				int maxPossibleSell = min(this.gridMaxSell, this.surplus + this.essMaxDischarge);
				int maxPossibleBuy = min(this.gridMaxBuy, -this.surplus + this.essMaxCharge);
				this.grid = fitWithin(-maxPossibleSell, maxPossibleBuy, target);

				this.ess = -(this.surplus + this.grid);
				this.state = State.GRID_SET;
			}
			case ESS_SET, GRID_SET -> doNothing();
			}

			return this.grid;
		}

		/**
		 * Returns the production.
		 *
		 * @return the production value
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
			return this.managedConsumptions.values().stream() //
					.mapToInt(Integer::intValue) //
					.sum();
		}

		/**
		 * Returns the surplus (production - consumption).
		 * 
		 * @return the surplus value
		 */
		public int getSurplus() {
			return this.surplus;
		}

		/**
		 * Solves the {@link EnergyFlow.Model} and returns an {@link EnergyFlow}.
		 * 
		 * @return the {@link EnergyFlow}; null if this {@link EnergyFlow.Model} is
		 *         unsolvable
		 */
		public EnergyFlow solve() {
			if (this.state == State.UNSET) {
				// Apply balancing
				this.setEss(-this.surplus);
			}

			return new EnergyFlow(//
					this.production, //
					this.unmanagedConsumption, //
					ImmutableSortedMap.copyOf(this.managedConsumptions), //
					this.ess, //
					this.grid);
		}

		private enum State {
			UNSET, //
			ESS_SET, //
			GRID_SET
		}

		@VisibleForTesting
		int getEss() {
			return this.ess;
		}

		@VisibleForTesting
		int getGrid() {
			return this.grid;
		}
	}
}
