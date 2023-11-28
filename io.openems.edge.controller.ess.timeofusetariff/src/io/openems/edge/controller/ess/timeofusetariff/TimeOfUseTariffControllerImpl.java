package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.common.utils.DateUtils.roundZonedDateTimeDownToMinutes;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateCharge100;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleRequest;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Context;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Optimizer;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Period;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Time-Of-Use-Tariff", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffControllerImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariffController, Controller, OpenemsComponent, TimedataProvider, JsonApi {

	/** Delayed Time is aggregated also after restart of OpenEMS. */
	private final CalculateActiveTime calculateDelayedTime = new CalculateActiveTime(this,
			TimeOfUseTariffController.ChannelId.DELAYED_TIME);

	private final ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();
	private final ScheduledExecutorService triggerExecutor = Executors.newSingleThreadScheduledExecutor();

	/** Charged Time is aggregated also after restart of OpenEMS. */
	private final CalculateActiveTime calculateChargedTime = new CalculateActiveTime(this,
			TimeOfUseTariffController.ChannelId.CHARGED_TIME);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private PredictorManager predictorManager;

	@Reference
	private TimeOfUseTariff timeOfUseTariff;

	@Reference
	private Sum sum;

	private Config config = null;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(isReserveSocEnabled=true))")
	private List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves = new CopyOnWriteArrayList<>();

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private Optimizer optimizer = null;

	public TimeOfUseTariffControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimeOfUseTariffController.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		/* Run Worker once now and afterwards every 15 minutes */
		this.optimizer = new Optimizer(() -> Context.create() //
				.predictorManager(this.predictorManager) //
				.timeOfUseTariff(this.timeOfUseTariff) //
				.ess(this.ess) //
				.ctrlEmergencyCapacityReserves(this.ctrlEmergencyCapacityReserves) //
				.ctrlLimitTotalDischarges(this.ctrlLimitTotalDischarges) //
				.controlMode(config.controlMode()) //
				.maxChargePowerFromGrid(config.maxChargePowerFromGrid()) //
				.maxChargePowerFromGrid(config.maxChargePowerFromGrid()) //
				.solveDurationChannel(this.channel(TimeOfUseTariffController.ChannelId.SOLVE_DURATION)) //
				.build());
		final AtomicReference<Future<?>> future = new AtomicReference<>();
		future.set(this.taskExecutor.submit(this.optimizer));

		var now = ZonedDateTime.now();
		var nextRun = roundZonedDateTimeDownToMinutes(now, 15).plusMinutes(5);

		this.triggerExecutor.scheduleAtFixedRate(() -> {
			// Cancel previous run
			future.get().cancel(true);
			future.set(this.taskExecutor.submit(this.optimizer));
		}, //

				// Run 10 minutes before full 15 minutes
				Duration.between(now, nextRun).toMillis(), //
				// then execute every 15 minutes
				Duration.of(15, ChronoUnit.MINUTES).toMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		shutdownAndAwaitTermination(this.taskExecutor, 0);
		shutdownAndAwaitTermination(this.triggerExecutor, 0);
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Mode given from the configuration.
		switch (this.config.mode()) {
		case AUTOMATIC -> this.modeAutomatic();
		case OFF -> this.modeOff();
		}

		this.updateVisualizationChannels();
	}

	private Period getCurrentPeriod() {
		var optimizer = this.optimizer;
		if (optimizer == null) {
			return null;
		}
		final var period = optimizer.getCurrentPeriod();
		if (period == null) {
			return null;
		}
		return period;
	}

	/**
	 * Apply the Schedule.
	 *
	 * @throws OpenemsNamedException on error
	 */
	private void modeAutomatic() throws OpenemsNamedException {
		final var period = this.getCurrentPeriod();
		if (period == null) {
			this.setDefaultValues();
			return;
		}

		// Handle StateMachine
		var state = period.state();
		this._setStateMachine(period.state());

		// Battery full? Always BALANCING.
		var soc = this.ess.getSoc().get();
		if (soc != null && soc > ESS_FULL_SOC_THRESHOLD) {
			state = BALANCING;
		}

		// Update the timer.
		this.calculateChargedTime.update(state == CHARGE);
		this.calculateDelayedTime.update(state == DELAY_DISCHARGE);

		// Get and apply ActivePower Less-or-Equals Set-Point
		var activePower = switch (period.state()) {
		case CHARGE -> calculateCharge100(this.ess, this.sum, this.config.maxChargePowerFromGrid());
		case DELAY_DISCHARGE -> 0;
		case BALANCING -> null;
		};

		if (activePower != null) {
			this.ess.setActivePowerLessOrEquals(activePower);
		}
	}

	/**
	 * Apply the mode OFF logic.
	 */
	private void modeOff() {
		this.setDefaultValues();
	}

	/**
	 * This is only to visualize data for better debugging.
	 */
	private void updateVisualizationChannels() {
		final Float quarterlyPrice;
		final Integer consumptionPrediction;
		final Integer productionPrediction;
		final Integer gridEnergy;
		final Integer chargeDischargeEnergy;
		var period = this.getCurrentPeriod();
		if (period == null) {
			// Values are not available.
			quarterlyPrice = this.timeOfUseTariff.getPrices().getValues()[0];
			consumptionPrediction = null;
			productionPrediction = null;
			gridEnergy = null;
			chargeDischargeEnergy = null;

		} else {
			// First period is always the current period.
			quarterlyPrice = period.price();
			consumptionPrediction = period.consumption();
			productionPrediction = period.production();
			gridEnergy = period.grid();
			chargeDischargeEnergy = period.essChargeDischarge();
		}

		// Set the channels
		this._setQuarterlyPrices(quarterlyPrice);
		this._setPredictedConsumption(consumptionPrediction);
		this._setPredictedProduction(productionPrediction);
		this._setGridEnergyChannel(gridEnergy);
		this._setChargeDischargeEnergyChannel(chargeDischargeEnergy);
	}

	/**
	 * Sets the Default values to the channels, if the Periods are not yet
	 * calculated or if the Mode is 'OFF'.
	 */
	private void setDefaultValues() {
		// Update the timer.
		this.calculateChargedTime.update(false);
		this.calculateDelayedTime.update(false);

		// Default State Machine.
		this._setStateMachine(null);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.GUEST);
		return switch (request.getMethod()) {
		case GetScheduleRequest.METHOD -> this.handleGetScheduleRequest(user, GetScheduleRequest.from(request));
		default -> throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		};
	}

	/**
	 * Handles a {@link GetScheduleRequest}.
	 *
	 * @param user    the User
	 * @param request the GetScheduleRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGetScheduleRequest(User user,
			GetScheduleRequest request) throws OpenemsNamedException {
		final var now = TimeOfUseTariffUtils.getNowRoundedDownToMinutes(this.componentManager.getClock(), 15);
		final var fromDate = now.minusHours(3);
		final var channelQuarterlyPrices = new ChannelAddress(this.id(), "QuarterlyPrices");
		final var channelStateMachine = new ChannelAddress(this.id(), "StateMachine");

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult = new TreeMap<>();

		// Query database for the last three hours, with 15-minute resolution.
		queryResult = this.timedata.queryHistoricData(null, fromDate, now,
				Set.of(channelQuarterlyPrices, channelStateMachine), new Resolution(15, ChronoUnit.MINUTES));
		return CompletableFuture.completedFuture(Utils.handleGetScheduleRequest(this.optimizer, request.getId(),
				queryResult, channelQuarterlyPrices, channelStateMachine));
	}

	@Override
	public String debugLog() {
		var period = this.getCurrentPeriod();
		if (period == null) {
			return "NONE";
		}
		return period.state().toString();
	}
}
