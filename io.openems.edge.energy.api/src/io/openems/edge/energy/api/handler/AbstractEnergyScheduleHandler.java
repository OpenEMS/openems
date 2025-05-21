package io.openems.edge.energy.api.handler;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
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

	protected final String parentFactoryPid;
	protected final String parentId;

	private final Serializer<?> serializer;
	private final Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction;
	private final Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction;

	protected Clock clock;
	protected OPTIMIZATION_CONTEXT coc;
	private JsonElement sourceLog;
	private Consumer<String> onRescheduleCallback;

	public AbstractEnergyScheduleHandler(String parentFactoryPid, String parentId, //
			Serializer<?> serializer, //
			Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction,
			Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction) {
		this.parentFactoryPid = parentFactoryPid;
		this.parentId = parentId;
		this.serializer = serializer;
		this.cocFunction = cocFunction;
		this.cscFunction = cscFunction;
	}

	public String getParentFactoryPid() {
		return this.parentFactoryPid;
	}

	public String getParentId() {
		return this.parentId;
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
		this.sourceLog = Optional.ofNullable(this.serializer) // Avoid NPE
				.map(Serializer::serialize) //
				.orElse(JsonNull.INSTANCE);
		return coc;
	}

	/**
	 * Create a ControllerScheduleContext.
	 * 
	 * @return the ControllerScheduleContext
	 */
	public SCHEDULE_CONTEXT createScheduleContext() {
		return this.cscFunction.apply(this.coc);
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

	/**
	 * Serialize.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJson() {
		var b = buildJsonObject() //
				.addProperty("factoryPid", this.parentFactoryPid) //
				.addProperty("id", this.parentId);
		var sourceLog = this.sourceLog;
		if (sourceLog != null) {
			b.add("source", this.sourceLog);
		}
		return b.build();
	}

	protected abstract void buildToString(MoreObjects.ToStringHelper toStringHelper);

	protected record Serializer<CONFIG>(JsonSerializer<CONFIG> serializer, Supplier<CONFIG> configSupplier) {

		/**
		 * Serialize the given CONFIG.
		 * 
		 * @return a {@link JsonElement}
		 */
		public JsonElement serialize() {
			var config = this.configSupplier.get();
			if (config == null) {
				return JsonNull.INSTANCE;
			}
			return this.serializer.serialize(config);
		}
	}

	protected abstract static class Builder<BUILDER, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		protected final String parentFactoryPid;
		protected final String parentId;

		protected Serializer<?> serializer = null;
		protected Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction = goc -> null;
		protected Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction = coc -> null;

		/**
		 * Sets the parent Factory-PID and Component-ID as unique ID for easier
		 * debugging.
		 * 
		 * @param parent the parent {@link OpenemsComponent}
		 */
		protected Builder(OpenemsComponent parent) {
			this(parent.serviceFactoryPid(), parent.id());
		}

		/**
		 * Sets the parent Factory-PID and Component-ID as unique ID for easier
		 * debugging.
		 * 
		 * @param parentFactoryPid the parent Factory-PID
		 * @param parentId         the parent ID
		 */
		protected Builder(String parentFactoryPid, String parentId) {
			this.parentFactoryPid = parentFactoryPid;
			this.parentId = parentId;
		}

		protected abstract BUILDER self();

		/**
		 * Sets the source serializer for use in RunOptimizerFromLogApp.
		 * 
		 * @param <CONFIG>       the type of the Config
		 * @param serializer     a {@link JsonSerializer} for a CONFIG
		 * @param configSupplier a supplier for a Config
		 * @return myself
		 */
		public final <CONFIG> BUILDER setSerializer(JsonSerializer<CONFIG> serializer,
				Supplier<CONFIG> configSupplier) {
			this.serializer = new Serializer<CONFIG>(serializer, configSupplier);
			return this.self();
		}

		/**
		 * Sets a {@link Function} to create a ControllerOptimizationContext from a
		 * {@link GlobalOptimizationContext}.
		 * 
		 * @param cocFunction the ControllerOptimizationContext function
		 * @return myself
		 */
		public final BUILDER setOptimizationContext(
				Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction) {
			this.cocFunction = cocFunction;
			return this.self();
		}

		/**
		 * Sets a {@link Supplier} to create a ControllerOptimizationContext.
		 * 
		 * @param cocSupplier the ControllerOptimizationContext supplier
		 * @return myself
		 */
		public final BUILDER setOptimizationContext(Supplier<OPTIMIZATION_CONTEXT> cocSupplier) {
			this.cocFunction = gsc -> cocSupplier.get();
			return this.self();
		}

		/**
		 * Sets a {@link Function} to create a ControllerScheduleContext.
		 * 
		 * @param cscFunction the ControllerScheduleContext function
		 * @return myself
		 */
		public final BUILDER setScheduleContext(Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction) {
			this.cscFunction = cscFunction;
			return this.self();
		}

		/**
		 * Sets a {@link Supplier} to create a ControllerScheduleContext.
		 * 
		 * @param cscSupplier the ControllerScheduleContext supplier
		 * @return myself
		 */
		public final BUILDER setScheduleContext(Supplier<SCHEDULE_CONTEXT> cscSupplier) {
			this.cscFunction = gsc -> cscSupplier.get();
			return this.self();
		}
	}
}