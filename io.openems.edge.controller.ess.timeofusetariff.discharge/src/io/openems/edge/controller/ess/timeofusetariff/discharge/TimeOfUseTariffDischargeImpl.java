package io.openems.edge.controller.ess.timeofusetariff.discharge;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

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
				int availableCapacity = Math.round((netCapacity * soc) / 100F);

				this.channel(TimeOfUseTariffDischarge.ChannelId.AVAILABLE_CAPACITY).setNextValue(availableCapacity);

				int remainingCapacity = this.getRemainingCapacity(availableCapacity, this.productionMap,
						this.consumptionMap, now, this.boundarySpace);

				// Resetting
				this.targetPeriods.clear();

				// list of periods calculation.
				if (remainingCapacity > 0) {
					// Initiating the calculation
					this.targetPeriods = this.calculateTargetPeriods(this.consumptionMap, this.quarterlyPrices,
							remainingCapacity, this.boundarySpace);
					this.channel(TimeOfUseTariffDischarge.ChannelId.TARGET_HOURS_CALCULATED).setNextValue(true);
				}

				this.channel(TimeOfUseTariffDischarge.ChannelId.NUMBER_OF_TARGET_HOURS)
						.setNextValue(this.targetPeriods.size());

				this.lastAccessedTime = now;
			}
		}
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
			for (Entry<ZonedDateTime, Float> entry : this.quarterlyPrices
					.subMap(this.boundarySpace.proLessThanCon, this.boundarySpace.proMoreThanCon).entrySet()) {

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
	private int getRemainingCapacity(int availableCapacity, TreeMap<ZonedDateTime, Integer> productionMap,
			TreeMap<ZonedDateTime, Integer> consumptionMap, ZonedDateTime now, BoundarySpace boundarySpace) {

		int consumptionTotal = 0;
		int remainingCapacity = 0;

		for (Entry<ZonedDateTime, Integer> entry : consumptionMap //
				.subMap(now, boundarySpace.proMoreThanCon) //
				.entrySet()) {

			consumptionTotal = consumptionTotal + entry.getValue() - productionMap.get(entry.getKey());
		}

		// remaining amount of energy that should be covered from grid.
		remainingCapacity = consumptionTotal - availableCapacity;

		// Update Channels
		this.channel(TimeOfUseTariffDischarge.ChannelId.TOTAL_CONSUMPTION).setNextValue(consumptionTotal);
		this.channel(TimeOfUseTariffDischarge.ChannelId.REMAINING_CONSUMPTION).setNextValue(remainingCapacity);

		return remainingCapacity;
	}

	/**
	 * This method returns the list of periods, during which ESS is avoided for
	 * consumption.
	 * 
	 * @param consumptionMap    predicted consumption data along with time in
	 *                          {@link TreeMap} format.
	 * @param quarterlyPrices   {@link TreeMap} consisting of hourly electricity
	 *                          prices along with time.
	 * @param remainingCapacity Amount of energy that should be covered from grid
	 *                          for consumption in night.
	 * @param boundarySpace     the {@link BoundarySpace}
	 * @return {@link List} list of target periods to avoid charging/discharging of
	 *         the battery.
	 */
	private List<ZonedDateTime> calculateTargetPeriods(TreeMap<ZonedDateTime, Integer> consumptionMap,
			TreeMap<ZonedDateTime, Float> quarterlyPrices, Integer remainingCapacity, BoundarySpace boundarySpace) {

		List<ZonedDateTime> targetHours = new ArrayList<ZonedDateTime>();
		ZonedDateTime currentQuarterHour = roundZonedDateTimeDownToMinutes(
				ZonedDateTime.now(this.componentManager.getClock()), 15) //
						.withZoneSameInstant(ZoneId.systemDefault());

		List<Entry<ZonedDateTime, Float>> priceList = new ArrayList<>(quarterlyPrices //
				.subMap(currentQuarterHour, boundarySpace.proMoreThanCon) //
				.entrySet());
		priceList.sort(Entry.comparingByValue());

		for (Entry<ZonedDateTime, Float> entry : priceList) {
			targetHours.add(entry.getKey());

			remainingCapacity -= consumptionMap.get(entry.getKey());

			// checks if we have sufficient capacity.
			if (remainingCapacity <= 0) {
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
