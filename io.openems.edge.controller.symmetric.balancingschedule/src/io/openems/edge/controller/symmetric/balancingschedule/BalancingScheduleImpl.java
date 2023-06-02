package io.openems.edge.controller.symmetric.balancingschedule;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

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
import com.google.gson.JsonObject;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.BalancingSchedule", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BalancingScheduleImpl extends AbstractOpenemsComponent
		implements BalancingSchedule, Controller, OpenemsComponent, JsonApi {

	private final Logger log = LoggerFactory.getLogger(BalancingScheduleImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	private List<GridConnSchedule> schedule = new CopyOnWriteArrayList<>();

	public BalancingScheduleImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				BalancingSchedule.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		// parse Schedule
		try {
			if (!config.schedule().trim().isEmpty()) {
				var scheduleElement = JsonUtils.parse(config.schedule());
				var scheduleArray = JsonUtils.getAsJsonArray(scheduleElement);
				this.applySchedule(scheduleArray);
			}
			this._setScheduleParseFailed(false);

		} catch (OpenemsNamedException e) {
			this._setScheduleParseFailed(true);
			this.logError(this.log, "Unable to parse Schedule: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Calculates required charge/discharge power.
	 *
	 * @param offset the power offset
	 * @return the required charge/discharge power
	 * @throws InvalidValueException on error
	 */
	private int calculateRequiredPower(int offset) throws InvalidValueException {
		return this.meter.getActivePower().getOrError() /* current buy-from/sell-to grid */
				+ this.ess.getActivePower().getOrError() /* current charge/discharge Ess */
				- offset; /* the offset given by the schedule */
	}

	@Override
	public void run() throws OpenemsNamedException {
		/*
		 * Get the current grid connection setpoint from the schedule
		 */
		var gridConnSetPointOpt = this.getGridConnSetPoint();
		this._setGridActivePowerSetPoint(gridConnSetPointOpt.orElse(null));
		if (!gridConnSetPointOpt.isPresent()) {
			this._setNoActiveSetpoint(true);
			return;
		}
		this._setNoActiveSetpoint(false);
		int gridConnSetPoint = gridConnSetPointOpt.get();

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		var gridMode = this.ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			return;
		}

		/*
		 * Calculates required charge/discharge power
		 */
		var calculatedPower = this.calculateRequiredPower(gridConnSetPoint);

		/*
		 * set result
		 */
		this.ess.setActivePowerEqualsWithPid(calculatedPower);
		this.ess.setReactivePowerEquals(0);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.OWNER);

		switch (request.getMethod()) {

		case SetGridConnScheduleRequest.METHOD:
			return this.handleSetGridConnScheduleRequest(user, SetGridConnScheduleRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a SetGridConnScheduleRequest.
	 *
	 * @param user    the User
	 * @param request the SetGridConnScheduleRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetGridConnScheduleRequest(User user,
			SetGridConnScheduleRequest request) {
		this.schedule = request.getSchedule();
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), new JsonObject()));
	}

	/**
	 * Parses the Schedule and applies it to this Controller.
	 *
	 * @param j the {@link JsonArray} with the Schedule
	 * @throws OpenemsNamedException on error
	 */
	private void applySchedule(JsonArray j) throws OpenemsNamedException {
		this.schedule = SetGridConnScheduleRequest.GridConnSchedule.from(j);
	}

	/**
	 * Gets the currently valid GridConnSetPoint.
	 *
	 * @return the current setpoint.
	 */
	private Optional<Integer> getGridConnSetPoint() {
		// Is the Grid Active-Power Set-Point currently overwritten using the channel?
		var setPointFromChannel = this.getGridActivePowerSetPointChannel().getNextWriteValueAndReset();
		if (setPointFromChannel.isPresent()) {
			// Yes -> use the channel value
			return setPointFromChannel;
		}
		// No -> use the value from the Schedule
		var now = ZonedDateTime.now(this.componentManager.getClock()).toEpochSecond();
		for (GridConnSchedule e : this.schedule) {
			if (now >= e.getStartTimestamp() && now <= e.getStartTimestamp() + e.getDuration()) {
				// -> this entry is valid!
				return Optional.ofNullable(e.getActivePowerSetPoint());
			}
		}
		// Still no -> no value available
		return Optional.empty();
	}
}
