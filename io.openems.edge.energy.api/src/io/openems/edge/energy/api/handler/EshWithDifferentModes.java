package io.openems.edge.energy.api.handler;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulationsProvider;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.DifferentModes.PostProcessor;
import io.openems.edge.energy.api.handler.DifferentModes.PreProcessor;
import io.openems.edge.energy.api.handler.DifferentModes.Simulator;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public final class EshWithDifferentModes<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		extends AbstractEnergyScheduleHandler<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		implements EnergyScheduleHandler.WithDifferentModes {

	private final BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, Modes<MODE>> modesFunction;
	private final InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> initialPopulationsProvider;
	private final PreProcessor<MODE, OPTIMIZATION_CONTEXT> preProcessor;
	private final Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator;
	private final PostProcessor<MODE, OPTIMIZATION_CONTEXT> postProcessor;
	private final SortedMap<ZonedDateTime, DifferentModes.Period<MODE, OPTIMIZATION_CONTEXT>> schedule = new TreeMap<>();

	private Modes<MODE> modes = Modes.empty();

	protected EshWithDifferentModes(//
			String parentFactoryPid, String parentId, //
			Serializer<?> serializer, //
			BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, Modes<MODE>> modesFunction, //
			Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction, //
			Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction, //
			InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> initialPopulationsProvider, //
			PreProcessor<MODE, OPTIMIZATION_CONTEXT> preProcessor, //
			Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator, //
			PostProcessor<MODE, OPTIMIZATION_CONTEXT> postProcessor) {
		super(parentFactoryPid, parentId, serializer, cocFunction, cscFunction);
		this.modesFunction = modesFunction;
		this.initialPopulationsProvider = initialPopulationsProvider;
		this.preProcessor = preProcessor;
		this.simulator = simulator;
		this.postProcessor = postProcessor;
	}

	@Override
	public OPTIMIZATION_CONTEXT initialize(GlobalOptimizationContext goc) {
		var context = super.initialize(goc);
		this.modes = this.modesFunction.apply(goc, context);
		return context;
	}

	@Override
	public ImmutableList<InitialPopulation.Transition> getInitialPopulation(GlobalOptimizationContext goc) {
		return this.initialPopulationsProvider.get(goc, this.coc, this.modes).stream() //
				.map(ip -> ip.toTansition(this.modes::getIndex)) //
				.collect(toImmutableList());
	}

	@Override
	public Modes<?> modes() {
		return this.modes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final SCHEDULE_CONTEXT createScheduleContext() {
		return super.createScheduleContext();
	}

	@Override
	public int preProcessPeriod(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, int modeIndex) {
		var oldMode = this.modes.get(modeIndex);
		var newMode = this.preProcessor.preProcess(period, this.coc, oldMode);
		return this.modes.getIndex(newMode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
			EnergyFlow.Model ef, int modeIndex, Fitness fitness) {
		this.simulator.simulate(this.parentId, period, gsc, this.coc, (SCHEDULE_CONTEXT) csc, ef,
				this.modes.get(modeIndex), fitness);
	}

	@Override
	public int postProcessPeriod(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, EnergyFlow ef,
			int modeIndex) {
		var oldMode = this.modes.get(modeIndex);
		var newMode = this.postProcessor.postProcess(this.parentId, period, gsc, ef, this.coc, oldMode);
		return this.modes.getIndex(newMode);
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
			if (this.modes.isEmpty()) {
				System.err.println("Modes is empty!"); // TODO proper log
				return;
			}
			schedule.forEach((k, t) -> {
				this.schedule.put(k, DifferentModes.Period.fromTransitionRecord(t, this.modes::get, coc));
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
	protected void buildToString(MoreObjects.ToStringHelper toStringHelper) {
		var modes = this.modes;
		if (modes != null) {
			toStringHelper.add("modes", this.modes.toString());
		}
	}
}