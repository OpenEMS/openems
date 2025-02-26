package io.openems.edge.energy.api.handler;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.MoreObjects;

import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

/**
 * The only allowed implementation of {@link EnergyScheduleHandler}.
 * 
 * <p>
 * Implementing it like this allows us to hide the generics types.
 * 
 * @param <OPTIMIZATION_CONTEXT> The ControllerOptimizationContext - commonly
 *                               abbreviated as `coc` - is a Context that is
 *                               valid throughout an entire Optimization for one
 *                               Controller/EnergyScheduleHandler. It's
 *                               typically recreated once per 15-minutes.
 * @param <SCHEDULE_CONTEXT>     The ControllerScheduleContext - commonly
 *                               abbreviated as `csc` - is a Context that is
 *                               valid for simulation of one Schedule of one
 *                               Controller/EnergyScheduleHandler. It's
 *                               typically recreated multiple times per second.
 */
public abstract sealed class AbstractEnergyScheduleHandler<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>
		permits EshWithDifferentModes, EshWithOnlyOneMode {

	private final String id;
	private final Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction;
	private final Supplier<SCHEDULE_CONTEXT> cscSupplier;

	protected Clock clock;
	protected OPTIMIZATION_CONTEXT coc;
	private Consumer<String> onRescheduleCallback;

	public AbstractEnergyScheduleHandler(String id,
			Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction,
			Supplier<SCHEDULE_CONTEXT> cscSupplier) {
		this.id = id;
		this.cocFunction = cocFunction;
		this.cscSupplier = cscSupplier;
	}

	public String getId() {
		return this.id;
	}

	/**
	 * Initialize the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This method is called internally before a Optimization is started.
	 * 
	 * @param goc the {@link GlobalOptimizationContext}
	 * @return the newly created ControllerOptimizationContext
	 */
	public OPTIMIZATION_CONTEXT initialize(GlobalOptimizationContext goc) {
		this.clock = goc.clock();
		var coc = this.cocFunction.apply(goc);
		this.coc = coc;
		return coc;
	}

	/**
	 * Create a ControllerScheduleContext.
	 * 
	 * @return the ControllerScheduleContext
	 */
	public SCHEDULE_CONTEXT createScheduleContext() {
		return this.cscSupplier.get();
	}

	/**
	 * This method sets the callback for events that require Rescheduling.
	 * 
	 * @param callback the {@link Consumer} callback with a reason
	 */
	public synchronized void setOnRescheduleCallback(Consumer<String> callback) {
		this.onRescheduleCallback = callback;
	}

	/**
	 * This method removes the callback.
	 */
	public synchronized void removeOnRescheduleCallback() {
		this.onRescheduleCallback = null;
	}

	/**
	 * Trigger Re-Schedule.
	 * 
	 * @param reason a reason for debug logging
	 */
	public void triggerReschedule(String reason) {
		var onRescheduleCallback = this.onRescheduleCallback;
		if (onRescheduleCallback != null) {
			onRescheduleCallback.accept(reason);
		}
	}

	protected ZonedDateTime getNow() {
		var clock = this.clock;
		if (clock != null) {
			return ZonedDateTime.now(clock);
		}
		return ZonedDateTime.now();
	}

	@Override
	public final String toString() {
		var toStringHelper = toStringHelper(this.id);
		var coc = this.coc;
		if (coc != null) {
			toStringHelper.addValue(coc);
		}
		return toStringHelper.toString();
	}

	protected abstract void buildToString(MoreObjects.ToStringHelper toStringHelper);
}