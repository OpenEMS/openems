package io.openems.edge.controller.symmetric.balancingschedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;
import io.openems.common.session.User;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Controller.Symmetric.BalancingSchedule", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BalancingSchedule extends AbstractOpenemsComponent implements Controller, OpenemsComponent, JsonApi {

	private final Logger log = LoggerFactory.getLogger(BalancingSchedule.class);

	@Reference
	protected ConfigurationAdmin cm;

	private List<GridConnSchedule> schedule = new ArrayList<>();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public BalancingSchedule() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		// parse Schedule
		try {
			if (!config.schedule().trim().isEmpty()) {
				JsonElement scheduleElement = JsonUtils.parse(config.schedule());
				JsonArray scheduleArray = JsonUtils.getAsJsonArray(scheduleElement);
				this.applySchedule(scheduleArray);
			}
		} catch (OpenemsNamedException e) {
			this.logError(log, "Unable to parse Schedule: " + e.getMessage());
		}
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Calculates required charge/discharge power
	 * 
	 * @throws InvalidValueException
	 */
	private int calculateRequiredPower(int offset) {
		return this.meter.getActivePower().orElse(0) /* current buy-from/sell-to grid */
				+ this.ess.getActivePower().orElse(0) /* current charge/discharge Ess */
				- offset; /* the offset given by the schedule */
	}

	@Override
	public void run() throws OpenemsException {
		/*
		 * Get the current grid connection setpoint from the schedule
		 */
		Optional<Integer> gridConnSetPointOpt = this.getGridConnSetPoint();
		if (!gridConnSetPointOpt.isPresent()) {
			this.logWarn(log, "No valid grid connection Set-Point existing in Schedule.");
			return;
		}
		int gridConnSetPoint = gridConnSetPointOpt.get();

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		GridMode gridMode = this.ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [" + gridMode + "]");
		}
		if (gridMode != GridMode.ON_GRID) {
			return;
		}
		/*
		 * Calculates required charge/discharge power
		 */
		int calculatedPower = this.calculateRequiredPower(gridConnSetPoint);

		// adjust value so that it fits into Min/MaxActivePower
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
				calculatedPower);

		/*
		 * set result
		 */
		this.ess.addPowerConstraintAndValidate("Balancing P", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
				calculatedPower); //
		this.ess.addPowerConstraintAndValidate("Balancing Q", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest message)
			throws OpenemsNamedException {
		SetGridConnScheduleRequest request = SetGridConnScheduleRequest.from(message);
		this.schedule = request.getSchedule();
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), new JsonObject()));
	}

	/**
	 * Parses the Schedule and applies it to this Controller
	 * 
	 * @param j
	 * @throws OpenemsNamedException
	 */
	private void applySchedule(JsonArray j) throws OpenemsNamedException {
		this.schedule = SetGridConnScheduleRequest.GridConnSchedule.from(j);
	}

	/**
	 * Gets the currently valid GridConnSetPoint
	 * 
	 * @return
	 */
	private Optional<Integer> getGridConnSetPoint() {
		long now = System.currentTimeMillis() / 1000; // in seconds
		for (GridConnSchedule e : this.schedule) {
			if (now > e.getStartTimestamp() && now < e.getStartTimestamp() + e.getDuration()) {
				// -> this entry is valid!
				return Optional.ofNullable(e.getActivePowerSetPoint());
			}
		}
		return Optional.empty();
	}
}
