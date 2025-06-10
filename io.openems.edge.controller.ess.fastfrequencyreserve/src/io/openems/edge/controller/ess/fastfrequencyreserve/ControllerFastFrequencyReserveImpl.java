package io.openems.edge.controller.ess.fastfrequencyreserve;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.controller.ess.fastfrequencyreserve.jsonrpc.SetActivateFastFreqReserveRequest;
import io.openems.edge.controller.ess.fastfrequencyreserve.jsonrpc.SetActivateFastFreqReserveRequest.ActivateFastFreqReserveSchedule;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.Context;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.FastFrequencyReserve", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerFastFrequencyReserveImpl extends AbstractOpenemsComponent
		implements ControllerFastFrequencyReserve, Controller, OpenemsComponent, ComponentJsonApi {

	private final Logger log = LoggerFactory.getLogger(ControllerFastFrequencyReserveImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private Config config = null;
	private List<ActivateFastFreqReserveSchedule> schedule = new CopyOnWriteArrayList<>();

	private static final Function<ActivateFastFreqReserveSchedule, Integer> OBTAIN_DICHARGE_POWER = ActivateFastFreqReserveSchedule::dischargePowerSetPoint;
	private static final Function<ActivateFastFreqReserveSchedule, Integer> OBTAIN_FREQ_LIMIT = ActivateFastFreqReserveSchedule::frequencyLimit;
	private static final Function<ActivateFastFreqReserveSchedule, Long> OBTAIN_STARTTIME_STAMP = ActivateFastFreqReserveSchedule::startTimestamp;
	private static final Function<ActivateFastFreqReserveSchedule, Integer> OBTAIN_DURATION = ActivateFastFreqReserveSchedule::duration;
	private static final Function<ActivateFastFreqReserveSchedule, ActivationTime> OBTAIN_ACTIVATION_TIME = ActivateFastFreqReserveSchedule::activationRunTime;
	private static final Function<ActivateFastFreqReserveSchedule, SupportDuration> OBTAIN_SUPPORT_DURATION = ActivateFastFreqReserveSchedule::supportDuration;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ElectricityMeter meter;

	public ControllerFastFrequencyReserveImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerFastFrequencyReserve.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SetActivateFastFreqReserveRequest.METHOD, //
				endpoint -> {
					endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
				}, call -> {
					this.handleSetActivateFastFreqReserveRequest(
							SetActivateFastFreqReserveRequest.from(call.getRequest()));

					return new GenericJsonrpcResponseSuccess(call.getRequest().getId(), JsonUtils.buildJsonObject() //
							.addProperty("startTimestamp", "recieved") //
							.build());
				});
	}

	/**
	 * Updates the configuration for the component, setting control mode,
	 * references, and activation schedule.
	 *
	 * @throws OpenemsNamedException On Exception.
	 */
	private void updateConfig() throws OpenemsNamedException {
		this._setControlMode(this.config.controlMode());

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", //
				this.config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", //
				this.config.meter_id())) {
			return;
		}
		try {
			if (!this.config.activationScheduleJson().trim().isEmpty()) {
				final var scheduleElement = JsonUtils.parse(this.config.activationScheduleJson());
				final var scheduleArray = JsonUtils.getAsJsonArray(scheduleElement);
				this.applySchedule(scheduleArray);
				this._setScheduleParseFailed(false);
			}
		} catch (IllegalStateException | OpenemsNamedException e) {
			this._setScheduleParseFailed(true);
			this.logError(this.log, "Unable to parse Schedule: " + e.getMessage());
		}
	}

	/**
	 * Updates the configuration for activating fast frequency reserve based on the
	 * provided request.
	 *
	 * @param request The request containing the schedule information.
	 */
	private void updateConfig(SetActivateFastFreqReserveRequest request) {
		var scheduleString = SetActivateFastFreqReserveRequest.listToString(request.getSchedule());
		OpenemsComponent.updateConfigurationProperty(this.cm, this.servicePid(), "activationScheduleJson",
				scheduleString);
	}

	private void applySchedule(JsonArray jsonArray) throws OpenemsNamedException {
		this.schedule = SetActivateFastFreqReserveRequest.ActivateFastFreqReserveSchedule.from(jsonArray);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		switch (this.config.controlMode()) {
		case MANUAL_ON -> {
			this.getConfigParams();
			this.handleStatemachine();
		}
		case MANUAL_OFF -> {
			// Do nothing
		}
		}
		this._setControlMode(this.config.controlMode());
	}

	private void getConfigParams() {

		this.setChannelValueIfNeeded(ControllerFastFrequencyReserve.ChannelId.DISCHARGE_POWER_SET_POINT,
				OBTAIN_DICHARGE_POWER);
		final var channelValue = this.getDischargePowerSetPoint();

		// Avoid calling
		if (!channelValue.isDefined()) {
			return;
		}
		this.setChannelValueIfNeeded(ControllerFastFrequencyReserve.ChannelId.FREQUENCY_LIMIT, //
				OBTAIN_FREQ_LIMIT);
		this.setChannelValueIfNeeded(ControllerFastFrequencyReserve.ChannelId.DURATION, //
				OBTAIN_DURATION);
		this.setChannelValueIfNeeded(ControllerFastFrequencyReserve.ChannelId.START_TIMESTAMP, //
				OBTAIN_STARTTIME_STAMP);
		// TODO get it for the activation time and support time, But currently this
		// tested for long activation time and long support time, other enums are for
		// future
		this.setChannelValueIfNeeded(ControllerFastFrequencyReserve.ChannelId.ACTIVATION_TIME, //
				OBTAIN_ACTIVATION_TIME);
		this.setChannelValueIfNeeded(ControllerFastFrequencyReserve.ChannelId.SUPPORT_DURATIN, //
				OBTAIN_SUPPORT_DURATION);
	}

	/**
	 * Sets the value for the specified {@code FastFrequencyReserve.ChannelId} based
	 * on the provided {@code Function}, only if needed.
	 *
	 * @param channelId      The channel to set the value for.
	 * @param obtainFunction A {@code Function} to retrieve the corresponding value
	 *                       based on the provided schedule entry.
	 */
	private void setChannelValueIfNeeded(ControllerFastFrequencyReserve.ChannelId channelId,
			Function<ActivateFastFreqReserveSchedule, ?> obtainFunction) {
		WriteChannel<?> channel = this.channel(channelId);
		var setPointFromChannel = channel.value();
		if (setPointFromChannel.isDefined()) {
			return;
		}

		var currentTime = this.componentManager.getClock().withZone(ZoneId.systemDefault());
		var now = Instant.now(currentTime).getEpochSecond();

		for (var scheduleEntry : this.schedule) {
			var endTime = scheduleEntry.startTimestamp() + scheduleEntry.duration();

			// Configurable minutes, and convert into seconds
			var preActivationTimeBeforeStartTime = this.config.preActivationTime() * 60;
			if (now >= scheduleEntry.startTimestamp() - preActivationTimeBeforeStartTime && now <= endTime) {
				channel.setNextValue(obtainFunction.apply(scheduleEntry));
				return;
			}
		}
		channel.setNextValue(null);
		return;
	}

	private void handleStatemachine() {
		if (this.checkGridMode()) {
			return;
		}

		var state = this.stateMachine.getCurrentState();
		this._setStateMachine(state);

		if (!this.areChannelsDefined()) {
			return;
		}

		var context = new Context(this, //
				this.componentManager.getClock(), //
				this.ess, //
				this.meter, //
				this.getStartTimestamp().get(), //
				this.getDuration().get(), //
				this.getDischargePowerSetPoint().get(), //
				this.getFrequencyLimit().get(), //
				// TODO if other version of FFR needed, need to test first with the Inverter
				// Capabilities
				this.getActivationTime(), //
				this.getSupportDuration());

		try {
			this.stateMachine.run(context);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	/**
	 * Checks the grid mode and returns a boolean value based on the grid mode
	 * state. If the grid mode is "ON_GRID" or "UNDEFINED," it returns false and
	 * logs a warning message when the grid mode is "UNDEFINED." If the grid mode is
	 * "OFF_GRID," it returns true.
	 *
	 * @return true if the grid mode is "OFF_GRID," false otherwise.
	 */
	private boolean checkGridMode() {
		return switch (this.ess.getGridMode()) {
		case ON_GRID -> false;
		case UNDEFINED -> {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
			yield false;
		}
		case OFF_GRID -> true;
		};
	}

	private boolean areChannelsDefined() {
		return Stream.of(//
				this.getDischargePowerSetPoint(), //
				this.getFrequencyLimit(), //
				this.getDuration(), //
				this.getStartTimestamp())//
				.allMatch(Value::isDefined);
	}

	private void handleSetActivateFastFreqReserveRequest(SetActivateFastFreqReserveRequest request)
			throws OpenemsNamedException {
		this.schedule = request.getSchedule();

		// get current schedule
		var currentSchedule = (String) this.getComponentContext()//
				.getProperties()//
				.get("activationScheduleJson");
		var currentScheduleArray = JsonUtils.getAsJsonArray(JsonUtils.parse(currentSchedule).getAsJsonArray());
		var currentScheduleList = ActivateFastFreqReserveSchedule.from(currentScheduleArray);

		if (this.schedule.size() == currentScheduleArray.size() && currentScheduleList.equals(this.schedule)) {
			return;
		}
		this.updateConfig(request);
	}
}
