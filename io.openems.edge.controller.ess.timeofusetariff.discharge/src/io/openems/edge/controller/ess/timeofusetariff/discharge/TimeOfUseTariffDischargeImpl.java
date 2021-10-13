package io.openems.edge.controller.ess.timeofusetariff.discharge;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
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

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.EmergencyCapacityReserveImpl;
import io.openems.edge.controller.ess.limittotaldischarge.LimitTotalDischargeController;
import io.openems.edge.controller.ess.timeofusetariff.discharge.tariff.AwattarProvider;
import io.openems.edge.controller.ess.timeofusetariff.discharge.tariff.TimeOfUseTariff;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.predictor.api.manager.PredictorManager;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Time-Of-Use-Tariff.Discharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffDischargeImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, TimeOfUseTariffDischarge {

	private static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	private static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private PredictorManager predictorManager;

	private final List<Controller> limitUsableCapacityControllers = new CopyOnWriteArrayList<>();

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(|(service.factoryPid=Controller.Ess.LimitTotalDischarge)(|(service.factoryPid=Controller.Ess.EmergencyCapacityReserve))))")
	protected synchronized void addCtrlLimitTotalDischarge(Controller controller) {
		this.limitUsableCapacityControllers.add(controller);
	}

	protected synchronized void removeCtrlLimitTotalDischarge(Controller controller) {
		this.limitUsableCapacityControllers.remove(controller);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private final TimeOfUseTariff timeOfUseTariff;

	private Config config = null;
	private boolean isPredictionValuesTaken = false;
	private BoundarySpace boundarySpace = null;
	private TreeMap<ZonedDateTime, Integer> consumptionMap = new TreeMap<>();
	private TreeMap<ZonedDateTime, Integer> productionMap = new TreeMap<>();
	private List<ZonedDateTime> targetPeriods = new ArrayList<ZonedDateTime>();
	private TreeMap<ZonedDateTime, Float> quarterlyPrices = new TreeMap<>();
	private TreeMap<ZonedDateTime, Integer> socWithoutLogic = new TreeMap<>();
	private ZonedDateTime lastAccessedTime = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());

	public TimeOfUseTariffDischargeImpl(TimeOfUseTariff timeOfUseTariff) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimeOfUseTariffDischarge.ChannelId.values() //
		);
		this.timeOfUseTariff = timeOfUseTariff;
	}

	public TimeOfUseTariffDischargeImpl() {
		this(new AwattarProvider());
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		// Current Date Time rounded off to 15 minutes.
		ZonedDateTime now = roundZonedDateTimeDownToMinutes(ZonedDateTime.now(this.componentManager.getClock()), 15);

		this.calculateBoundarySpace(now);

		this.calculateTargetPeriodsWithinBoundarySpace(now);

		this.avoidDischargeDuringTargetPeriods();

		this.updateVisualizationChannels(now);
	}

	/*
	 * Every Day at 14:00 the Hourly Prices are updated. we receive the predictions
	 * at 14:00 till next day 13:00.
	 * 
	 * 'isPredictionValuesTaken' to make sure the control logic executes only once
	 * during the hour.
	 */
	private void calculateBoundarySpace(ZonedDateTime now) {
		if (now.getHour() == 14 && !this.isPredictionValuesTaken) {

			// Predictions as Integer array in 15 minute intervals.
			final Integer[] predictionProduction = this.predictorManager.get24HoursPrediction(SUM_PRODUCTION) //
					.getValues();
			final Integer[] predictionConsumption = this.predictorManager.get24HoursPrediction(SUM_CONSUMPTION) //
					.getValues();
			final ZonedDateTime predictionStartQuarterHour = roundZonedDateTimeDownToMinutes(now, 15);

			// resetting values
			this.quarterlyPrices.clear();

			// Converts the given 15 minute integer array to a TreeMap values.
			this.convertDataStructure(predictionProduction, predictionConsumption, predictionStartQuarterHour);

			// calculates the boundary space, within which the controller needs to work.
			this.boundarySpace = BoundarySpace.from(predictionStartQuarterHour, this.productionMap, this.consumptionMap,
					this.config.maxStartHour(), this.config.maxEndHour());

			// Update Channels
			this.channel(TimeOfUseTariffDischarge.ChannelId.PRO_MORE_THAN_CON)
					.setNextValue(this.boundarySpace.proMoreThanCon.getHour());
			this.channel(TimeOfUseTariffDischarge.ChannelId.PRO_LESS_THAN_CON)
					.setNextValue(this.boundarySpace.proLessThanCon.getHour());

			// Hourly Prices from API
			this.quarterlyPrices = this.timeOfUseTariff.getPrices();

			// setting the channel id values
			if (this.quarterlyPrices.isEmpty()) {
				this.channel(TimeOfUseTariffDischarge.ChannelId.QUATERLY_PRICES_TAKEN).setNextValue(false);
				return;

			} else {
				this.channel(TimeOfUseTariffDischarge.ChannelId.QUATERLY_PRICES_TAKEN).setNextValue(true);
			}

			this.isPredictionValuesTaken = true; // boolean used to take prediction values only once.
		}

		// resets the 'isPredictionValuesTaken' to be ready for next day.
		if (now.getHour() == 15 && this.isPredictionValuesTaken) {
			this.isPredictionValuesTaken = false;
		}
	}

	/**
	 * Calculate the Target Periods every 15 minutes within the boundary period.
	 * 
	 * @param now Current Date Time rounded off to 15 minutes.
	 * @throws InvalidValueException on error
	 */
	private void calculateTargetPeriodsWithinBoundarySpace(ZonedDateTime now) throws InvalidValueException {
		// Initializing with Default values.
		this.channel(TimeOfUseTariffDischarge.ChannelId.TARGET_HOURS_CALCULATED).setNextValue(false);

		// if the boundary space are calculated, start scheduling only during boundary
		// space.
		if (this.boundarySpace != null && this.boundarySpace.isWithinBoundary(now)) {

			// Runs every 15 minutes.
			if (now.isAfter(this.lastAccessedTime)) {

				int netCapacity = this.ess.getCapacity().getOrError();
				int soc = this.ess.getSoc().getOrError();

				// Usable capacity based on min soc from Limit total discharge and emergency
				// reserve controllers.
				int limitSoc = 0;
				for (Controller ctrl : limitUsableCapacityControllers) {
					if (ctrl.serviceFactoryPid().equals("Controller.Ess.EmergencyCapacityReserve")) {
						limitSoc = Math.max(limitSoc, ((EmergencyCapacityReserveImpl) ctrl).getMinSoc());
					} else {
						limitSoc = Math.max(limitSoc, ((LimitTotalDischargeController) ctrl).getMinSoc());
					}
				}
				this.channel(TimeOfUseTariffDischarge.ChannelId.MIN_SOC).setNextValue(limitSoc);

				// Calculating available energy and usable energy [Wmsec] in the battery.
				long availableEnergy = (long) (((double) netCapacity /* [Wh] */ * 3600 /* [Wsec] */ * 1000 /* [Wmsec] */
						/ 100 /* [%] */) * soc /* [current SoC] */);

				// Value is divided by 3600 * 1000 to convert from [Wmsec] to [Wh].
				this.channel(TimeOfUseTariffDischarge.ChannelId.AVAILABLE_CAPACITY)
						.setNextValue(availableEnergy / 3600000);

				long limitEnergy = (long) (((double) netCapacity /* [Wh] */ * 3600 /* [Wsec] */ * 1000 /* [Wmsec] */
						/ 100 /* [%] */) * limitSoc /* [current SoC] */);

				availableEnergy = Math.max(0, (availableEnergy - limitEnergy));

				// Value is divided by 3600 * 1000 to convert from [Wmsec] to [Wh].
				this.channel(TimeOfUseTariffDischarge.ChannelId.USABLE_CAPACITY)
						.setNextValue(availableEnergy / 3600000);

				// To estimate the soc curve when controller logic is not applied
				if (now.equals(this.boundarySpace.proLessThanCon)) {
					this.socWithoutLogic = this.generateSocCurveWithoutLogic(netCapacity, availableEnergy, limitEnergy,
							this.consumptionMap, soc, now, this.boundarySpace);
				}

				long remainingEnergy = this.getRemainingCapacity(availableEnergy, productionMap, consumptionMap, now,
						boundarySpace);

				// Resetting
				this.targetPeriods.clear();

				// list of periods calculation.
				if (remainingEnergy > 0) {
					// Initiating the calculation
					this.targetPeriods = this.calculateTargetPeriods(this.consumptionMap, this.quarterlyPrices,
							remainingEnergy, this.boundarySpace);
					this.channel(TimeOfUseTariffDischarge.ChannelId.TARGET_HOURS_CALCULATED).setNextValue(true);
				}

				this.channel(TimeOfUseTariffDischarge.ChannelId.NUMBER_OF_TARGET_HOURS)
						.setNextValue(this.targetPeriods.size());

				this.lastAccessedTime = now;
			}
		}
	}

	/**
	 * This method returns the map of 15 minutes soc curve values when no controller
	 * logic is applied.
	 * 
	 * @param netCapacity     Net Capacity of the battery.
	 * @param availableEnergy available energy in the battery.
	 * @param limitEnergy     energy restricted to used based on min soc.
	 * @param consumptionMap  map of predicted consumption values.
	 * @param soc             current SoC of the battery.
	 * @param now             current time.
	 * @param boundarySpace   the {@link BoundarySpace}
	 */
	private TreeMap<ZonedDateTime, Integer> generateSocCurveWithoutLogic(int netCapacity, long availableEnergy,
			long limitEnergy, TreeMap<ZonedDateTime, Integer> consumptionMap, int soc, ZonedDateTime now,
			BoundarySpace boundarySpace) {

		TreeMap<ZonedDateTime, Integer> socWithoutLogic = new TreeMap<>();

		// current values.
		socWithoutLogic.put(now, soc);

		for (Entry<ZonedDateTime, Integer> entry : consumptionMap.subMap(now, boundarySpace.proMoreThanCon)
				.entrySet()) {

			long duration = 15 * 60 * 1000;
			long currentConsumptionEnergy = entry.getValue() * duration;

			if (availableEnergy > limitEnergy) {
				availableEnergy -= currentConsumptionEnergy;
			}

			double calculatedSoc = availableEnergy //
					/ (netCapacity * 3600. /* [Wsec] */ * 1000 /* [Wmsec] */) //
					* 100 /* [SoC] */;

			if (calculatedSoc > 100) {
				soc = 100;
			} else if (calculatedSoc < 0) {
				soc = 0;
			} else {
				soc = (int) Math.round(calculatedSoc);
			}

			socWithoutLogic.put(entry.getKey().plusMinutes(15), soc);
		}

		return socWithoutLogic;
	}

	/**
	 * Apply the actual logic of avoiding to discharge the battery during target
	 * periods.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void avoidDischargeDuringTargetPeriods() throws OpenemsNamedException {
		this.channel(TimeOfUseTariffDischarge.ChannelId.TARGET_HOURS_IS_EMPTY)
				.setNextValue(this.targetPeriods.isEmpty());

		ZonedDateTime currentQuarterHour = roundZonedDateTimeDownToMinutes(
				ZonedDateTime.now(this.componentManager.getClock()), 15) //
						.withZoneSameInstant(ZoneId.systemDefault());

		if (this.targetPeriods.contains(currentQuarterHour)) {
			// set result
			this.ess.setActivePowerLessOrEquals(0);

			this.channel(TimeOfUseTariffDischarge.ChannelId.BUYING_FROM_GRID).setNextValue(true);

		} else {
			this.channel(TimeOfUseTariffDischarge.ChannelId.BUYING_FROM_GRID).setNextValue(false);
		}
	}

	/**
	 * This is only to visualize data for better debugging.
	 * 
	 * @param now Current Date Time rounded off to 15 minutes.
	 */
	private void updateVisualizationChannels(ZonedDateTime now) {
		// Storing quarterly prices in channel for visualization in Grafana.
		if (!this.quarterlyPrices.isEmpty()) {
			for (Entry<ZonedDateTime, Float> entry : this.quarterlyPrices.entrySet()) {

				if (now.isEqual(entry.getKey())) {
					this.channel(TimeOfUseTariffDischarge.ChannelId.QUARTERLY_PRICES) //
							.setNextValue(entry.getValue());
				}
			}
		}

		// Storing Production and Consumption in channel for visualization in Grafana.
		if (!this.productionMap.isEmpty()) {
			for (Entry<ZonedDateTime, Integer> entry : this.productionMap.entrySet()) {

				if (now.isEqual(entry.getKey())) {
					this.channel(TimeOfUseTariffDischarge.ChannelId.PRODUCTION) //
							.setNextValue(entry.getValue());
					this.channel(TimeOfUseTariffDischarge.ChannelId.CONSUMPTON) //
							.setNextValue(this.consumptionMap.get(entry.getKey()));
				}
			}
		}

		if (!this.socWithoutLogic.isEmpty()) {
			if (this.boundarySpace.isWithinBoundary(now)) {
				for (Entry<ZonedDateTime, Integer> entry : this.socWithoutLogic.entrySet()) {
					if (now.isEqual(entry.getKey())) {
						this.channel(TimeOfUseTariffDischarge.ChannelId.PREDICTED_SOC_WITHOUT_LOGIC) //
								.setNextValue(entry.getValue());
					}
				}
			}
		} else {
			this.socWithoutLogic.clear();
			this.channel(TimeOfUseTariffDischarge.ChannelId.PREDICTED_SOC_WITHOUT_LOGIC) //
					.setNextValue(null);
		}
	}

	/**
	 * This method converts the 15 minute integer array values to a one hour
	 * {@link TreeMap} format for ease in later calculations.
	 * 
	 * @param productionValues  list of 96 production values predicted, comprising
	 *                          for next 24 hours.
	 * @param consumptionValues list of 96 consumption values predicted, comprising
	 *                          for next 24 hours.
	 * @param startHour         start hour of the predictions.
	 */
	private void convertDataStructure(Integer[] productionValues, Integer[] consumptionValues,
			ZonedDateTime startHour) {
		this.productionMap.clear();
		this.consumptionMap.clear();

		for (int i = 0; i < 96; i++) {
			Integer production = productionValues[i];
			Integer consumption = consumptionValues[i];
			ZonedDateTime time = startHour.plusMinutes(i * 15);

			if (production != null && consumption != null) {
				this.productionMap.put(time, production);
				this.consumptionMap.put(time, consumption);
			}
		}
	}

	/**
	 * This Method Returns the remaining Capacity that needs to be taken from Grid.
	 * 
	 * @param availableCapacity Amount of energy available in the ess based on SoC.
	 * @param productionMap     predicted production data along with time in
	 *                          {@link TreeMap} format.
	 * @param consumptionMap    predicted consumption data along with time in
	 *                          {@link TreeMap} format.
	 * @param now               Current Date Time rounded off to 15 minutes.
	 * @param boundarySpace     the {@link BoundarySpace}
	 * @return remainingCapacity Amount of energy that should be covered from grid
	 *         for consumption in night.
	 */
	private long getRemainingCapacity(long availableEnergy, TreeMap<ZonedDateTime, Integer> productionMap,
			TreeMap<ZonedDateTime, Integer> consumptionMap, ZonedDateTime now, BoundarySpace boundarySpace) {

		long consumptionEnergy = 0;
		long remainingEnergy = 0;

		for (Entry<ZonedDateTime, Integer> entry : consumptionMap //
				.subMap(now, boundarySpace.proMoreThanCon) //
				.entrySet()) {

			long duration = 15 * 60 * 1000;
			long currentConsumptionEnergy = entry.getValue() * duration;
			long currentProductionEnergy = productionMap.get(entry.getKey()) * duration;

			consumptionEnergy = consumptionEnergy + currentConsumptionEnergy - currentProductionEnergy;
		}

		// remaining amount of energy that should be covered from grid.
		remainingEnergy = consumptionEnergy - availableEnergy;

		// Update Channels
		// Values are divided by 3600 * 1000 to convert from [Wmsec] to [Wh].
		this.channel(TimeOfUseTariffDischarge.ChannelId.TOTAL_CONSUMPTION).setNextValue((consumptionEnergy / 3600000));
		this.channel(TimeOfUseTariffDischarge.ChannelId.REMAINING_CONSUMPTION)
				.setNextValue((remainingEnergy / 3600000));

		return remainingEnergy;
	}

	/**
	 * This method returns the list of periods, during which ESS is avoided for
	 * consumption.
	 * 
	 * @param consumptionMap  predicted consumption data along with time in
	 *                        {@link TreeMap} format.
	 * @param quarterlyPrices {@link TreeMap} consisting of hourly electricity
	 *                        prices along with time.
	 * @param remainingEnergy Amount of energy that should be covered from grid for
	 *                        consumption in night.
	 * @param boundarySpace   the {@link BoundarySpace}
	 * @return {@link List} list of target periods to avoid charging/discharging of
	 *         the battery.
	 */
	private List<ZonedDateTime> calculateTargetPeriods(TreeMap<ZonedDateTime, Integer> consumptionMap,
			TreeMap<ZonedDateTime, Float> quarterlyPrices, long remainingEnergy, BoundarySpace boundarySpace) {

		List<ZonedDateTime> targetHours = new ArrayList<ZonedDateTime>();
		ZonedDateTime currentQuarterHour = roundZonedDateTimeDownToMinutes(
				ZonedDateTime.now(this.componentManager.getClock()), 15) //
						.withZoneSameInstant(ZoneId.systemDefault());

		List<Entry<ZonedDateTime, Float>> priceList = new ArrayList<>(quarterlyPrices //
				.subMap(currentQuarterHour, boundarySpace.proMoreThanCon) //
				.entrySet());
		priceList.sort(Entry.comparingByValue());
		long duration = 15 * 60 * 1000;

		for (Entry<ZonedDateTime, Float> entry : priceList) {
			targetHours.add(entry.getKey());

			remainingEnergy = remainingEnergy - (consumptionMap.get(entry.getKey()) * duration);

			// checks if we have sufficient capacity.
			if (remainingEnergy <= 0) {
				break;
			}
		}

		return targetHours;
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to required(m) minutes.
	 * 
	 * @param d the {@link ZonedDateTime}
	 * @param m the {@link Integer} custom minutes to roundoff to.
	 * @return the rounded result
	 */
	private static ZonedDateTime roundZonedDateTimeDownToMinutes(ZonedDateTime d, int m) {
		int minuteOfDay = d.get(ChronoField.MINUTE_OF_DAY);
		return d.with(ChronoField.NANO_OF_DAY, 0).plus(minuteOfDay / m * m, ChronoUnit.MINUTES);
	}
}
