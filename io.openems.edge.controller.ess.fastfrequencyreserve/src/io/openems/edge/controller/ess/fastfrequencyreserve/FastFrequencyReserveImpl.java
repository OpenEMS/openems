package io.openems.edge.controller.ess.fastfrequencyreserve;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SetActivateFastFreqReserveRequest;
import io.openems.common.jsonrpc.request.SetActivateFastFreqReserveRequest.ActivateFastFreqReserveSchedule;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.ComponentManagerProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.Mode;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.Context;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

//CHECKSTYLE:OFF
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Fastfrequencyreserve", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class FastFrequencyReserveImpl extends AbstractOpenemsComponent
		implements FastFrequencyReserve, Controller, OpenemsComponent, ComponentManagerProvider, JsonApi {

	private final Logger log = LoggerFactory.getLogger(FastFrequencyReserveImpl.class);

	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private Config config = null;

	private Mode mode = Mode.MANUAL_OFF;

	private List<ActivateFastFreqReserveSchedule> schedule = new CopyOnWriteArrayList<>();

	private Function<ActivateFastFreqReserveSchedule, Integer> GET_DISCHARGE_POWER = ActivateFastFreqReserveSchedule::getDischargePowerSetPoint;
	private Function<ActivateFastFreqReserveSchedule, Integer> GET_FREQ_LIMIT = ActivateFastFreqReserveSchedule::getFrequencyLimit;
	private Function<ActivateFastFreqReserveSchedule, Long> GET_STARTTIME_STAMP = ActivateFastFreqReserveSchedule::getStartTimestamp;
	private Function<ActivateFastFreqReserveSchedule, Integer> GET_DURATION = ActivateFastFreqReserveSchedule::getDuration;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Override
	public ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Reference
	protected ManagedSymmetricEss ess;

	@Reference
	private ElectricityMeter meter;

	public FastFrequencyReserveImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				FastFrequencyReserve.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	private void updateConfig(Config config) throws OpenemsNamedException {
		this.config = config;
		this.mode = config.mode();
		this.channel(FastFrequencyReserve.ChannelId.MODE).setNextValue(this.mode);

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", //
				config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", //
				config.meter_id())) {
			return;
		}
		if (!this.config.activationScheduleJson().trim().isEmpty()) {
			var scheduleElement = JsonUtils.parse(config.activationScheduleJson());
			var scheduleArray = JsonUtils.getAsJsonArray(scheduleElement);
			this.applySchedule(scheduleArray);

		}
		
		this._setScheduleParseFailed(false);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		boolean modeChanged;
		do {
			modeChanged = false;
			switch (this.mode) {
			case MANUAL_ON:
				this.getConfigParams();
				this.handleFfrStatemachine();
				modeChanged = this.changeMode(Mode.MANUAL_ON);
				break;
			case MANUAL_OFF:
				modeChanged = this.changeMode(Mode.MANUAL_OFF);
				break;
			}
		} while (modeChanged);
		this.channel(FastFrequencyReserve.ChannelId.MODE).setNextValue(this.mode);
	}

	private void handleFfrStatemachine() {

		if (checkGridMode()) {
			return;
		}

		if (!this.areChannelsDefined()) {
			this.logInfo(this.log, "Params are not initialized, or not in time schedule");
			return;
		}

		var state = this.stateMachine.getCurrentState();

		this.channel(FastFrequencyReserve.ChannelId.STATE_MACHINE).setNextValue(state);

		int dischargePower = this.getDischargeActivePowerSetPoint().get();
		long startTimestamp = this.getStartTimeStamp().get();
		int duration = this.getDuration().get();
		int freqLimit = this.getFrequencyLimitSetPoint().get();
		ActivationTime activationRunTime = ActivationTime.LONG_ACTIVATION_RUN;
		SupportDuration supportDuration = SupportDuration.LONG_SUPPORT_DURATION;

		var context = new Context(this, //
				this.getComponentManager(), //
				this.ess, //
				this.meter, //
				startTimestamp, //
				duration, //
				dischargePower, //
				freqLimit, //
				activationRunTime, //
				supportDuration);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}

	}

	private boolean changeMode(Mode nextMode) {
		if (this.mode != nextMode) {
			this.mode = nextMode;
			return true;
		}
		return false;
	}

	private boolean checkGridMode() {
		var gridMode = this.ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			return false;
		case OFF_GRID:
		default:
			return true;
		}
	}

	private boolean areChannelsDefined() {
		return Stream.of(//
				this.getDischargeActivePowerSetPoint(), //
				this.getFrequencyLimitSetPoint(), //
				this.getDuration(), //
				this.getStartTimeStamp())//
				.allMatch(Value::isDefined);
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.GUEST);
		switch (request.getMethod()) {
		case SetActivateFastFreqReserveRequest.METHOD -> {
			return this.handleSetActivateFastFreqReserveRequest(user, SetActivateFastFreqReserveRequest.from(request));
		}
		default -> throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleSetActivateFastFreqReserveRequest(User user,
			SetActivateFastFreqReserveRequest request) throws OpenemsNamedException {
		this.schedule = request.getSchedule();
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), new JsonObject()));
	}

	private void applySchedule(JsonArray j) throws OpenemsNamedException {
		this.schedule = SetActivateFastFreqReserveRequest.ActivateFastFreqReserveSchedule.from(j);
	}

	private void setPoint(//
			FastFrequencyReserve.ChannelId channelId, //
			Function<ActivateFastFreqReserveSchedule, ?> getMethod) {

		WriteChannel<?> channel = this.channel(channelId);
		var setPointFromChannel = channel.getNextWriteValueAndReset();
		if (setPointFromChannel.isPresent()) {
			var setValue = setPointFromChannel.get();
			channel.setNextValue(setValue);
			return;
		}
		var now = Instant.now(this.componentManager.getClock()).getEpochSecond();

		for (var e : this.schedule) {
			 this.printer(now, e);
			if (now >= e.getStartTimestamp() - 900 && now <= e.getStartTimestamp() + e.getDuration()) {
				channel.setNextValue(getMethod.apply(e));
				return;
			}
		}
		channel.setNextValue(null);
	}
	
	private void printer(long now, ActivateFastFreqReserveSchedule e) {
		System.out.println("Now : " + Instant.ofEpochSecond(now));
		System.out.println("Start : " + Instant.ofEpochSecond(e.getStartTimestamp()));
		System.out.println("now >= e.getStartTimestamp() : " + (now >= e.getStartTimestamp()));
		System.out.println("end : " + Instant.ofEpochSecond(e.getStartTimestamp() + e.getDuration()));
		System.out.println(
				"now <= e.getStartTimestamp() + e.getDuration() : " + (now <= e.getStartTimestamp() + e.getDuration()));
	}

	private void getConfigParams() {

		this.setPoint(FastFrequencyReserve.ChannelId.DISCHARGE_POWER_SET_POINT, //
				this.GET_DISCHARGE_POWER);

		this.setPoint(FastFrequencyReserve.ChannelId.FREQUENCY_LIMIT, //
				this.GET_FREQ_LIMIT);

		this.setPoint(FastFrequencyReserve.ChannelId.DURATION, //
				this.GET_DURATION);

		this.setPoint(FastFrequencyReserve.ChannelId.START_TIMESTAMP, //
				this.GET_STARTTIME_STAMP);

	}



	// CHECKSTYLE:ON
}
