package io.openems.edge.controller.dynamicdischarge;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;

/**
 * @author sagar.venu
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.DynamicDischarge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DynamicDischargeImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, DynamicDischarge {

	private final Logger log = LoggerFactory.getLogger(DynamicDischargeImpl.class);
	private final Prices prices = new Prices();

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected PredictorManager predictorManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private Config config = null;
	private boolean isPredictionValuesTaken = false;
	private boolean isAllowedToCalculateHours = false;
	private ZonedDateTime proLessThanCon = null;
	private ZonedDateTime proMoreThanCon = null;
	private TreeMap<ZonedDateTime, Integer> hourlyConsumption = new TreeMap<>();
	private TreeMap<ZonedDateTime, Integer> hourlyProduction = new TreeMap<>();
	private List<ZonedDateTime> cheapHours = new ArrayList<ZonedDateTime>();

	public DynamicDischargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				DynamicDischarge.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Get required variables
		ZonedDateTime now = ZonedDateTime.now(this.componentManager.getClock())
				.withZoneSameInstant(ZoneId.systemDefault());

		/*
		 * Every Day at 14:00 the Hourly Prices are updated. we receive the predictions
		 * at 14:00 till next day 13:00.
		 * 
		 * 'isPredictionValuesTaken' to make sure the control logic executes only once
		 * during the hour.
		 * 
		 */
		if (now.getHour() == this.config.predictionStartHour() && !isPredictionValuesTaken) {

			// Predictions
			Prediction24Hours hourlyPredictionProduction = this.predictorManager
					.get24HoursPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
			Prediction24Hours hourlyPredictionConsumption = this.predictorManager
					.get24HoursPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));
			ZonedDateTime predictionStartQuarterHour = (roundZonedDateTimeDownTo15Minutes(now));

			// Predictions as Integer array in 15 minute intervals.
			Integer[] hourlyProduction = hourlyPredictionProduction.getValues();
			Integer[] hourlyConsumption = hourlyPredictionConsumption.getValues();

			// resetting values
			this.cheapHours.clear();
			this.proLessThanCon = null;
			this.proMoreThanCon = null;

			// Converts the given 15 minute integer array to a TreeMap of one hour values.
			this.convertDataStructure(hourlyProduction, hourlyConsumption, predictionStartQuarterHour);

			this.hourlyConsumption.entrySet().forEach(a -> {
				System.out.println("time: " + a.getKey() + " consumption: " + a.getValue() + " Production: "
						+ this.hourlyProduction.get(a.getKey()));
			});

			// calculates the boundary hours, within which the controller needs to work
			this.getBoundaryHours(this.hourlyProduction, this.hourlyConsumption, predictionStartQuarterHour);

			this.isPredictionValuesTaken = true; // Used to take prediction values only once.
			this.isAllowedToCalculateHours = true; // used to schedule only once
		}

		// resets the 'isPredictionValuesTaken' to be ready for next day.
		if (now.getHour() == this.config.predictionStartHour() + 1 && this.isPredictionValuesTaken) {
			this.isPredictionValuesTaken = false;
		}

		// if the boundary hours are calculated, start scheduling only during boundary
		// hour.
		if (this.proLessThanCon != null && this.proMoreThanCon != null) {

			// When Production < Consumption
			if (now.getHour() == this.proLessThanCon.getHour() //
					&& this.isAllowedToCalculateHours) {

				Integer nettCapacity = this.ess.getCapacity().getOrError();
				Integer availableCapacity = (nettCapacity * this.ess.getSoc().getOrError()) / 100;

				// setting the channel id values
				IntegerReadChannel availableCapacityValue = this.channel(DynamicDischarge.ChannelId.AVAILABLE_CAPACITY);
				availableCapacityValue.setNextValue(availableCapacity);

				int remainingCapacity = this.getRemainingCapacity(availableCapacity, this.hourlyProduction,
						this.hourlyConsumption);

				TreeMap<ZonedDateTime, Float> hourlyPrices = this.prices.houlryPrices(config.url(), config.apikey());

				/*
				 * Only used for Junit Test case
				 * 
				 * Instead of hourly prices taken from API, we use it from config.
				 */
//				JsonArray line = JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.priceConfig()));
//				ZonedDateTime timestamp = now.withMinute(0).withSecond(0).withNano(0);
//				TreeMap<ZonedDateTime, Float> hourlyPrices = new TreeMap<>();
//
//				for (JsonElement element : line) {
//					Float price = JsonUtils.getAsFloat(element, "marketprice");
//					hourlyPrices.put(timestamp, price);
//					timestamp = timestamp.plusHours(1);
//				}

//				this.hourlyConsumption.entrySet().forEach(a -> {
//					System.out.println("time: " + a.getKey() + " consumption: " + a.getValue() + " Production: "
//							+ this.hourlyProduction.get(a.getKey()) + " price: " + hourlyPrices.get(a.getKey()));
//				});

				// setting the channel id values
				if (hourlyPrices.isEmpty()) {
					this.channel(DynamicDischarge.ChannelId.HOURLY_PRICES_TAKEN).setNextValue(false);
					return;
				}

				this.channel(DynamicDischarge.ChannelId.HOURLY_PRICES_TAKEN).setNextValue(true);

				// list of hours, during which battery is avoided.
				this.calculateTargetHours(this.hourlyConsumption, hourlyPrices, remainingCapacity);

				this.isAllowedToCalculateHours = false; // used to schedule only once
			}
		}

		// Avoiding Discharging during cheapest hours
		if (!this.cheapHours.isEmpty()) {
			for (ZonedDateTime entry : cheapHours) {
				if (now.getHour() == entry.getHour()) {
					// set result
					this.ess.addPowerConstraintAndValidate("SymmetricDynamicDischargePower", Phase.ALL, Pwr.ACTIVE,
							Relationship.EQUALS, 0);
					this.channel(DynamicDischarge.ChannelId.BUYING_FROM_GRID).setNextValue(true);
				}
			}
		} else {
			this.channel(DynamicDischarge.ChannelId.BUYING_FROM_GRID).setNextValue(false);
		}
	}

	/**
	 * This method converts the 15 minute integer array values to a one hour TreeMap
	 * format for ease in later calculations.
	 * 
	 * @param productionValues
	 * @param consumptionValues
	 * @param startHour
	 */
	private void convertDataStructure(Integer[] productionValues, Integer[] consumptionValues,
			ZonedDateTime startHour) {

		ZonedDateTime startTime = startHour;
		int productionValue = 0;
		int consumptionValue = 0;

		for (int i = 0; i < 96; i++) {
			Integer currProduction = productionValues[i];
			Integer currConsumption = consumptionValues[i];
			ZonedDateTime currTime = startHour.plusMinutes(i * 15);

			if (currProduction != null && currConsumption != null) {

				if (currTime.getHour() == startTime.getHour()) {
					productionValue += currProduction;
					consumptionValue += currConsumption;
				} else {
					this.hourlyProduction.put(startTime.withMinute(0).withSecond(0).withNano(0), productionValue);
					this.hourlyConsumption.put(startTime.withMinute(0).withSecond(0).withNano(0), consumptionValue);
					startTime = currTime;
					productionValue = currProduction;
					consumptionValue = currConsumption;
				}
			}
		}
	}

	/**
	 * This method Calculates the boundary hours within which the schedule logic
	 * works.
	 * 
	 * @param productionData
	 * @param consumptionData
	 * @param startHour
	 */
	private void getBoundaryHours(TreeMap<ZonedDateTime, Integer> productionData,
			TreeMap<ZonedDateTime, Integer> consumptionData, ZonedDateTime startHour) {

		for (Entry<ZonedDateTime, Integer> entry : consumptionData.entrySet()) {
			Integer production = productionData.get(entry.getKey());
			Integer consumption = entry.getValue();

			if (production != null && consumption != null) {

				// Last hour of the day when Production < Consumption.
				if ((production > consumption) //
						&& (entry.getKey().getDayOfYear() == ZonedDateTime.now(this.componentManager.getClock())
								.getDayOfYear())) {
					this.proLessThanCon = entry.getKey();
				}

				// First hour of the day when production > consumption
				if ((production > consumption) //
						&& (entry.getKey().getDayOfYear() == ZonedDateTime.now(this.componentManager.getClock())
								.plusDays(1).getDayOfYear()) //
						&& (this.proMoreThanCon == null) //
						&& (entry.getKey().getHour() <= 10)) {
					this.proMoreThanCon = entry.getKey();
				}
			}
		}

		// if there is no production available, 'proLessThanCon' and 'proMoreThanCon'
		// are not calculated.
		if (this.proLessThanCon == null) {
			this.proLessThanCon = ZonedDateTime.now(this.componentManager.getClock()).withHour(0).withMinute(0)
					.withSecond(0).withNano(0).plusHours(config.maxEndHour());
		}

		if (this.proMoreThanCon == null) {
			this.proMoreThanCon = ZonedDateTime.now(this.componentManager.getClock()).withHour(0).withMinute(0)
					.withSecond(0).withNano(0).plusHours(config.maxStartHour()).plusDays(1);
		}

		log.info("proLessThanCon " + this.proLessThanCon + "  proMoreThanCon " + this.proMoreThanCon);
	}

	/**
	 * This Method Returns the remaining Capacity that needs to be taken from Grid.
	 * 
	 * @param availableCapacity
	 * @param hourlyProduction
	 * @param hourlyConsumption
	 * @return remainingCapacity {@link Integer}
	 */
	private Integer getRemainingCapacity(int availableCapacity, TreeMap<ZonedDateTime, Integer> hourlyProduction,
			TreeMap<ZonedDateTime, Integer> hourlyConsumption) {

		int consumptionTotal = 0;
		int remainingCapacity = 0;

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.subMap(proLessThanCon, proMoreThanCon)
				.entrySet()) {

			consumptionTotal += entry.getValue() - hourlyProduction.get(entry.getKey());

		}

		// remaining amount of energy that should be covered from grid.
		remainingCapacity = consumptionTotal - availableCapacity;

		// Update Channels
		this.channel(DynamicDischarge.ChannelId.TOTAL_CONSUMPTION).setNextValue(consumptionTotal);
		this.channel(DynamicDischarge.ChannelId.REMAINING_CONSUMPTION).setNextValue(remainingCapacity);

		return remainingCapacity;

	}

	/**
	 * This method calculates the list of hours, during which battery is avoided.
	 * 
	 * @param hourlyConsumption
	 * @param hourlyPrices
	 */
	private void calculateTargetHours(TreeMap<ZonedDateTime, Integer> hourlyConsumption,
			TreeMap<ZonedDateTime, Float> hourlyPrices, Integer remainingcapacity) {
		Float minPrice = Float.MAX_VALUE;
		ZonedDateTime cheapTimeStamp = null;

		for (Map.Entry<ZonedDateTime, Float> entry : hourlyPrices.entrySet()) {
			if (!this.cheapHours.contains(entry.getKey())) {
				if (entry.getValue() < minPrice) {
					cheapTimeStamp = entry.getKey();
					minPrice = entry.getValue();
				}
			}
		}
		// adds the cheapest hour.
		this.cheapHours.add(cheapTimeStamp);
		log.info("cheapTimeStamp: " + cheapTimeStamp);

		/*
		 * check -> consumption for cheap hour for previous day and compare with
		 * remaining energy needed for today.
		 * 
		 */
		if (!this.cheapHours.isEmpty()) {
			for (Map.Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {
				for (ZonedDateTime hours : cheapHours) {
					if (entry.getKey().getHour() == hours.getHour()) {
						remainingcapacity -= entry.getValue();
					}
				}
			}
		}

		// if we need more cheap hours
		if (remainingcapacity > 0) {
			this.calculateTargetHours(hourlyConsumption, hourlyPrices, remainingcapacity);
		}

		// Update the Channels.
		if (!this.cheapHours.isEmpty()) {
			this.channel(DynamicDischarge.ChannelId.TARGET_HOURS_CALCULATED).setNextValue(true);
			this.channel(DynamicDischarge.ChannelId.NUMBER_OF_TARGET_HOURS).setNextValue(this.cheapHours.size());
		}
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to 15 minutes.
	 * 
	 * @param d the {@link ZonedDateTime}
	 * @return the rounded result
	 */
	private static ZonedDateTime roundZonedDateTimeDownTo15Minutes(ZonedDateTime d) {
		int minuteOfDay = d.get(ChronoField.MINUTE_OF_DAY);
		return d.with(ChronoField.NANO_OF_DAY, 0).plus(minuteOfDay / 15 * 15, ChronoUnit.MINUTES);
	}
}
