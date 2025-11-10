package io.openems.edge.energy.api.handler;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulationsProvider;
import io.openems.edge.energy.api.handler.DifferentModes.PostProcessor;
import io.openems.edge.energy.api.handler.DifferentModes.Simulator;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public final class EshWithDifferentModes<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		extends AbstractEnergyScheduleHandler<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		implements EnergyScheduleHandler.WithDifferentModes {

	private final MODE defaultMode;
	private final BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, MODE[]> availableModesFunction;
	private final InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> initialPopulationsProvider;
	private final Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator;
	private final PostProcessor<MODE, OPTIMIZATION_CONTEXT> postProcessor;
	private final SortedMap<ZonedDateTime, DifferentModes.Period<MODE, OPTIMIZATION_CONTEXT>> schedule = new TreeMap<>();

	private MODE[] availableModes;

	protected EshWithDifferentModes(//
			String parentFactoryPid, String parentId, //
			Serializer<?> serializer, //
			MODE defaultMode, //
			BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, MODE[]> availableModesFunction, //
			Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction, //
			Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction, //
			InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> initialPopulationsProvider, //
			Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator, //
			PostProcessor<MODE, OPTIMIZATION_CONTEXT> postProcessor) {
		super(parentFactoryPid, parentId, serializer, cocFunction, cscFunction);
		this.defaultMode = defaultMode;
		this.availableModesFunction = availableModesFunction;
		this.initialPopulationsProvider = initialPopulationsProvider;
		this.simulator = simulator;
		this.postProcessor = postProcessor;
	}

	@Override
	public OPTIMIZATION_CONTEXT initialize(GlobalOptimizationContext goc) {
		var context = super.initialize(goc);
		this.availableModes = this.availableModesFunction.apply(goc, context);
		return context;
	}

	@Override
	public ImmutableList<InitialPopulation.Transition> getInitialPopulation(GlobalOptimizationContext goc) {
		return this.initialPopulationsProvider.get(goc, this.coc, this.availableModes).stream() //
				.map(ip -> ip.toTansition(this::getModeIndex)) //
				.collect(toImmutableList());
	}

	/**
	 * Gets the default Mode.
	 * 
	 * @return the default Mode
	 */
	public MODE getDefaultMode() {
		return this.defaultMode;
	}

	@Override
	public int getDefaultModeIndex() {
		var modes = this.availableModes;
		if (modes == null) {
			throw new IllegalAccessError(
					"EnergySchedulerHandler is uninitialized. `initialize()` must be called first.");
		}
		return IntStream.range(0, modes.length) //
				.filter(i -> modes[i] == this.defaultMode) //
				.findFirst() //
				.orElse(0 /* fallback */);
	}

	@Override
	public int getNumberOfAvailableModes() {
		return this.availableModes.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final SCHEDULE_CONTEXT createScheduleContext() {
		return super.createScheduleContext();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
			EnergyFlow.Model ef, int modeIndex, Fitness fitness) {
		this.simulator.simulate(this.parentId, period, gsc, this.coc, (SCHEDULE_CONTEXT) csc, ef,
				this.availableModes[modeIndex], fitness);
	}

	@Override
	public int postProcessPeriod(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, EnergyFlow ef,
			int modeIndex) {
		return this.getModeIndex(this.postProcessor.postProcess(this.parentId, period, gsc, ef, this.coc,
				this.availableModes[modeIndex]));
	}

	@Override
	public void applySchedule(ImmutableSortedMap<ZonedDateTime, DifferentModes.Period.Transition> schedule) {
		final var thisQuarter = roundDownToQuarter(this.getNow());
		final var nextQuarter = thisQuarter.plusMinutes(15);
		final var coc = this.coc;
		synchronized (this.schedule) {
			// Clear outdated entries
			this.schedule.headMap(thisQuarter).clear();

			// Remove future entries
			this.schedule.tailMap(nextQuarter).clear();

			// Update entries from param
			var modes = this.availableModes;
			if (modes.length == 0) {
				System.err.println("Modes is empty!"); // TODO proper log
				return;
			}
			schedule.forEach((k, t) -> {
				this.schedule.put(k, DifferentModes.Period.fromTransitionRecord(t, this::getMode, coc));
			});
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ImmutableSortedMap<ZonedDateTime, DifferentModes.Period<MODE, OPTIMIZATION_CONTEXT>> getSchedule() {
		synchronized (this.schedule) {
			return ImmutableSortedMap.copyOfSorted(this.schedule);
		}
	}

	/**
	 * Gets the current {@link Period} record.
	 *
	 * @return the record of the currently scheduled Period; possibly null
	 */
	public DifferentModes.Period<MODE, OPTIMIZATION_CONTEXT> getCurrentPeriod() {
		synchronized (this.schedule) {
			final var thisQuarter = roundDownToQuarter(this.getNow());
			return this.schedule.get(thisQuarter);
		}
	}

	@Override
	public String toModeString(int modeIndex) {
		return this.getMode(modeIndex).toString();
	}

	/**
	 * Gets the MODE for the given modeIndex.
	 * 
	 * @param modeIndex the modeIndex
	 * @return the STATE
	 */
	private MODE getMode(int modeIndex) {
		var modes = this.availableModes;
		return modeIndex < modes.length //
				? modes[modeIndex] //
				: this.defaultMode;
	}

	/**
	 * Gets the modeIndex for the given MODE.
	 * 
	 * @param mode the MODE
	 * @return the modeIndex; or zero if not found
	 */
	private int getModeIndex(MODE mode) {
		var modes = this.availableModes;
		for (var i = 0; i < modes.length; i++) {
			if (modes[i] == mode) {
				return i;
			}
		}
		return 0;
	}

	@Override
	protected void buildToString(MoreObjects.ToStringHelper toStringHelper) {
		var availableModes = this.availableModes;
		if (availableModes != null) {
			toStringHelper.add("availableModes", Arrays.toString(availableModes));
		}
	}
}