package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.predictor.api.hourly.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.hourly.ProductionHourlyPredictor;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;

/**
 * @author sagar.venu
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.Dynamiccharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DynamicCharge extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Config config = null;

	private final Logger log = LoggerFactory.getLogger(DynamicCharge.class);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	@Reference
	protected PredictorManager predictorManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private ZonedDateTime proLessThanCon = null;
	private ZonedDateTime proMoreThanCon = null;
	private TreeMap<ZonedDateTime, Integer> hourlyConsumption = new TreeMap<>();
	private TreeMap<ZonedDateTime, Integer> hourlyProduction = new TreeMap<>();
	private TreeMap<ZonedDateTime, Float> hourlyPrices = new TreeMap<>();
	private TreeMap<ZonedDateTime, Integer> chargeSchedule = new TreeMap<>();
	private boolean isPredictionValuesTaken = false;
	private boolean isAllowedToCalculateHours = false;
	private float currentMinPrice;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TOTAL_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("Total consmption for the night")),
		REMAINING_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("remaining consmption to charge from grid")),
		NUMBER_OF_TARGET_HOURS(Doc.of(OpenemsType.INTEGER) //
				.text("Target Hours")),
		AVAILABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.text("Available capcity in the battery during evening"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public DynamicCharge() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
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

		ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.systemDefault());

		// For Junit case.
//		ZonedDateTime now = ZonedDateTime.now().withHour(0).plusHours(16).withZoneSameInstant(ZoneId.systemDefault());

		/*
		 * Every Day at 14:00 the Hourly Prices are updated. we receive the predictions
		 * at 14:00 till next day 13:00.
		 * 
		 * 'isPredictionValuesTaken' to make sure the control logic executes only once
		 * during the hour.
		 * 
		 */
		if (now.getHour() == this.config.getPredictionsHour() && !this.isPredictionValuesTaken) {

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
			this.chargeSchedule.clear();
			this.proLessThanCon = null;
			this.proMoreThanCon = null;

			// Converts the given integer array to a TreeMap of one hour values.
			this.convertDataStructure(hourlyProduction, hourlyConsumption, predictionStartQuarterHour);

			// Calculates the boundary hours.
			this.getBoundaryHours(this.hourlyProduction, this.hourlyConsumption, predictionStartQuarterHour);

//			Integer[] productionValues = this.productionHourlyPredictor.get24hPrediction().getValues();
//			Integer[] consumptionValues = this.consumptionHourlyPredictor.get24hPrediction().getValues();
//			ZonedDateTime startHour = this.productionHourlyPredictor.get24hPrediction().getStart();

			// calculates the boundary hours, within which the controller needs to work
			// Boundary hours = Production < consumption, Consumption > production.
//			this.calculateBoundaryHours(productionValues, consumptionValues, startHour);

			// Print the Predicted Values and Storing in a global variables for later
			// calculations during the hour when the
			// production is less than the consumption.
//			for (int i = 0; i < 24; i++) {
//				log.info("Production[" + i + "] " + " - " + productionValues[i] + " Consumption[" + i + "] " + " - "
//						+ consumptionValues[i]);
//
//				// Structuring the data in TreeMap format for easy calculations later
//				if (consumptionValues[i] != null && productionValues[i] != null) {
//					this.hourlyProduction.put(startHour.plusHours(i).withZoneSameInstant(ZoneId.systemDefault()),
//							productionValues[i]);
//					this.hourlyConsumption.put(startHour.plusHours(i).withZoneSameInstant(ZoneId.systemDefault()),
//							consumptionValues[i]);
//				} else {
//					this.hourlyProduction.put(startHour.plusHours(i).withZoneSameInstant(ZoneId.systemDefault()), 0);
//					this.hourlyConsumption.put(startHour.plusHours(i).withZoneSameInstant(ZoneId.systemDefault()),
//							2850);
//				}
//			}

			if (this.hourlyProduction.isEmpty() || this.hourlyConsumption.isEmpty()) {
				return;
			}

			this.isAllowedToCalculateHours = true; // used to schedule only once
			this.isPredictionValuesTaken = true; // Used to take prediction values only once.

		}

		// Resetting the isPredictionValuesTaken to make it ready to execute next day.
		if (now.getHour() == (this.config.getPredictionsHour() + 1) && this.isPredictionValuesTaken) {
			this.isPredictionValuesTaken = false;
		}

		log.info("proLessThanCon " + proLessThanCon + " proMoreThanCon " + proMoreThanCon);

		// Start scheduling exactly when Production is less than Consumption
		if (this.proLessThanCon != null) {
			if (now.getHour() == this.proLessThanCon.getHour() //
					&& this.isAllowedToCalculateHours) {

				int nettCapacity = ess.getCapacity().getOrError();
				int maxApparentPower = ess.getMaxApparentPower().getOrError();
				int availableCapacity = (nettCapacity * ess.getSoc().getOrError()) / 100;

				// setting the channel id values
				IntegerReadChannel availableCapacityValue = this.channel(ChannelId.AVAILABLE_CAPACITY);
				availableCapacityValue.setNextValue(availableCapacity);

				log.info("availableCapacity = " + availableCapacity + " nett capacity " + nettCapacity
						+ " Max Apparent Power " + maxApparentPower);

				// Required Energy that needs to be taken from grid
				// Required Energy = Total Consumption - Available Battery capacity
				Integer totalConsumption = this.calculateConsumptionBetweenHours(proLessThanCon, proMoreThanCon);

				// setting the channel id values
				IntegerReadChannel totalConsumptionValue = this.channel(ChannelId.TOTAL_CONSUMPTION);
				totalConsumptionValue.setNextValue(totalConsumption);

				Integer remainingEnergyToCharge = totalConsumption - availableCapacity;

				// setting the channel id values
				IntegerReadChannel remainingConsumptionValue = this.channel(ChannelId.REMAINING_CONSUMPTION);
				remainingConsumptionValue.setNextValue(remainingEnergyToCharge);

				log.info("totalConsumption = " + totalConsumption + " remainingEnergy " + remainingEnergyToCharge);

				if (remainingEnergyToCharge > 0) {

					/*
					 * Only used for Junit Test case
					 */
//					JsonArray line = JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.priceConfig()));
//					ZonedDateTime timestamp = now.withMinute(0).withSecond(0).withNano(0);
//
//					for (JsonElement element : line) {
//						Float price = JsonUtils.getAsFloat(element, "marketprice");
//						this.hourlyPrices.put(timestamp, price);
//						timestamp = timestamp.plusHours(1);
//					}

					/*
					 * Collects the hourly Prices from the API
					 */
					this.hourlyPrices = PriceApi.houlryPrices();

					// Calls to get the schedule.
					this.getChargeSchedule(this.proLessThanCon, this.proMoreThanCon, totalConsumption,
							availableCapacity);

					// setting the channel id values
					IntegerReadChannel NoOfTargetHoursValue = this.channel(ChannelId.NUMBER_OF_TARGET_HOURS);
					NoOfTargetHoursValue.setNextValue(this.chargeSchedule.size());
				}
			}
			this.isAllowedToCalculateHours = false;
		}

		this.hourlyConsumption.entrySet().forEach(a -> {
			System.out.println("time: " + a.getKey() + " consumption: " + a.getValue() + " price: "
					+ this.hourlyPrices.get(a.getKey()));
		});

		/*
		 * Charges the battery if it is the Scheduled hour.
		 */
		if (!this.chargeSchedule.isEmpty()) {
			this.chargeSchedule.entrySet().forEach(a -> {
				log.info("time: " + a.getKey() + " value: " + a.getValue());
			});

			for (Entry<ZonedDateTime, Integer> entry : this.chargeSchedule.entrySet()) {
				if (now.getHour() == entry.getKey().getHour()) {

					int power = entry.getValue() * -1;

					int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
							power);
					this.ess.addPowerConstraintAndValidate("SymmetricDynamicChargePower", Phase.ALL, Pwr.ACTIVE,
							Relationship.EQUALS, calculatedPower);
				}
			}
		}
	}

	/**
	 * @param proLessThanCon
	 * @param proMoreThanCon
	 * @param totalConsumption
	 * @param availableEnergy
	 * @throws InvalidValueException
	 */
	private void getChargeSchedule(ZonedDateTime proLessThanCon, ZonedDateTime proMoreThanCon, Integer totalConsumption,
			Integer availableEnergy) throws InvalidValueException {

		// function to find the minimum priceHour
		ZonedDateTime cheapestHour = this.cheapHour(proLessThanCon, proMoreThanCon);

		Integer predictedConumption = this.calculateConsumptionBetweenHours(proLessThanCon, cheapestHour);

		Integer currentHourConsumption = this.hourlyConsumption.get(cheapestHour);

		Integer remainingConsumption = 0;

		log.info("cheapestHour " + cheapestHour + " totalConsumption " + totalConsumption + " predictedConumption "
				+ predictedConumption + " currentHourConsumption " + currentHourConsumption);

		int nettCapacity = ess.getCapacity().getOrError();
		int maxApparentPower = ess.getMaxApparentPower().getOrError();

		// Calculates the amount of energy that needs to be charged during the cheapest
		// price hours.
		if (totalConsumption > 0) {

			// if the battery has sufficient energy!
			if (availableEnergy >= predictedConumption) {
				totalConsumption -= predictedConumption;
				availableEnergy -= predictedConumption;

				log.info("totalConsumption " + totalConsumption + " availableEnergy " + availableEnergy);
				this.adjustRemainigConsumption(cheapestHour, proMoreThanCon, totalConsumption, availableEnergy,
						nettCapacity, maxApparentPower);
			} else {
				// if the battery does not has sufficient energy!
				Integer chargebleConsumption = totalConsumption - predictedConumption - currentHourConsumption;

				log.info("chargebleConsumption " + chargebleConsumption);

				if (chargebleConsumption > 0) {
					if (chargebleConsumption > maxApparentPower) {

						// forms a reference point.
						ZonedDateTime lastCheapTimeStamp = cheapestHour;

						// checking for next cheap hour if it is before or after the first cheapest
						// hour.
						// cheapest hour between 'evening' and 'cheapest hour'
						cheapestHour = this.cheapHour(proLessThanCon, lastCheapTimeStamp);
						float firstMinPrice = currentMinPrice;

						// cheapest hour between 'first cheapest hour' and the 'morning of the next
						// day'.
						cheapestHour = this.cheapHour(lastCheapTimeStamp.plusHours(1), proMoreThanCon);

						// next Cheap Hour is after the first one
						if (currentMinPrice < firstMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							this.adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1), proMoreThanCon,
									remainingConsumption, maxApparentPower, nettCapacity, maxApparentPower);
						} else {
							// next Cheap Hour is before the first one
							if (chargebleConsumption > nettCapacity) {
								remainingConsumption = chargebleConsumption - nettCapacity;
								this.adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1), proMoreThanCon,
										remainingConsumption, nettCapacity, nettCapacity, maxApparentPower);
							}
						}

						cheapestHour = lastCheapTimeStamp;
						chargebleConsumption = maxApparentPower;
					}
					totalConsumption = totalConsumption - chargebleConsumption - currentHourConsumption
							+ remainingConsumption;
					remainingConsumption = 0;

					// adding into charge Schedule
					this.chargeSchedule.put(cheapestHour, chargebleConsumption);
					this.getChargeSchedule(proLessThanCon, cheapestHour, totalConsumption, availableEnergy);
				} else {
					this.getChargeSchedule(proLessThanCon, cheapestHour, predictedConumption, availableEnergy);
				}
			}
		}
	}

	/**
	 * @param start
	 * @param end
	 * @param remainingConsumption
	 * @param availableEnergy
	 * @param nettCapacity
	 * @param maxApparentPower
	 * @throws InvalidValueException
	 */
	private void adjustRemainigConsumption(ZonedDateTime start, ZonedDateTime end, //
			Integer remainingConsumption, Integer availableEnergy, int nettCapacity, //
			int maxApparentPower) throws InvalidValueException {

		if (remainingConsumption > 0) {

			if (!start.isEqual(end)) {

				ZonedDateTime cheapTimeStamp = cheapHour(start, end);
				Integer currentHourConsumption = hourlyConsumption.get(cheapTimeStamp);
				int predictedConsumption = this.calculateConsumptionBetweenHours(start, cheapTimeStamp);
				int chargebleConsumption = remainingConsumption - predictedConsumption - currentHourConsumption;

				log.info("cheapTimeStamp " + cheapTimeStamp + " remainingConsumption " + remainingConsumption
						+ " predictedConsumption: " + predictedConsumption + " currentHourConsumption "
						+ currentHourConsumption + " chargebleConsumption " + chargebleConsumption);

				if (predictedConsumption > availableEnergy) {
					availableEnergy = 0;
				} else {
					availableEnergy -= predictedConsumption;
				}

				Integer allowedConsumption = nettCapacity - availableEnergy;

				if (allowedConsumption > 0) {
					if (allowedConsumption > maxApparentPower) {
						allowedConsumption = maxApparentPower;
					}

					if (chargebleConsumption > 0) {
						if (chargebleConsumption > allowedConsumption) {
							chargebleConsumption = allowedConsumption;
							remainingConsumption = remainingConsumption - allowedConsumption - currentHourConsumption
									- predictedConsumption;
							availableEnergy += allowedConsumption;

							// adding into charge Schedule
							this.chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
							this.adjustRemainigConsumption(cheapTimeStamp.plusHours(1), end, remainingConsumption,
									availableEnergy, nettCapacity, maxApparentPower);
						} else {
							// adding into charge Schedule
							remainingConsumption = remainingConsumption - chargebleConsumption - currentHourConsumption
									- predictedConsumption;
							this.chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
						}
					}

				} else {
					availableEnergy -= currentHourConsumption;
					remainingConsumption = remainingConsumption - currentHourConsumption - predictedConsumption;
					this.adjustRemainigConsumption(cheapTimeStamp.plusHours(1), end, remainingConsumption,
							availableEnergy, nettCapacity, maxApparentPower);
				}

			}

		}

	}

	/**
	 * returns the total consumption between start(inclusive) and end (exclusive)
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private Integer calculateConsumptionBetweenHours(ZonedDateTime start, ZonedDateTime end) {
		Integer consumption = 0;

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.subMap(start, end).entrySet()) {
			consumption += entry.getValue();
		}
		return consumption;
	}

	/**
	 * This method returns the cheapest hours within start(inclusive) and end
	 * (exclusive)
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private ZonedDateTime cheapHour(ZonedDateTime start, ZonedDateTime end) {
		float minPrice = Float.MAX_VALUE;
		ZonedDateTime cheapTimeStamp = null;

		for (Entry<ZonedDateTime, Float> entry : hourlyPrices.subMap(start, end).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}

		this.currentMinPrice = minPrice;
		return cheapTimeStamp;
	}

//	/**
//	 * returns the list of hours, during which calculations needs to be considered.
//	 * 
//	 * @param productionValues
//	 * @param consumptionValues
//	 * @param startHour
//	 */
//	private void calculateBoundaryHours(Integer[] productionValues, Integer[] consumptionValues,
//			ZonedDateTime startHour) {
//		for (int i = 0; i < 24; i++) {
//			Integer production = productionValues[i];
//			Integer consumption = consumptionValues[i];
//
//			// Detects the last time when the production is less than consumption and first
//			// time when the production is greater than consumption.
//			if (production != null && consumption != null) {
//				// last hour of the day when production was greater than consumption
//				if ((production > consumption) //
//						&& (startHour.plusHours(i).getDayOfYear() == ZonedDateTime.now().getDayOfYear())) {
//					this.proLessThanCon = startHour.plusHours(i);
//				}
//
//				// First hour of the day when production was greater than consumption
//				if ((production > consumption) //
//						&& (startHour.plusHours(i).getDayOfYear() == ZonedDateTime.now().plusDays(1).getDayOfYear()) //
//						&& (this.proMoreThanCon == null) //
//						&& (startHour.plusHours(i).getHour() <= 10)) {
//					this.proMoreThanCon = startHour.plusHours(i);
//				}
//			}
//		}
//
//		for (int i = 0; i < 96; i++) {
//			Integer production = productionValues[i];
//			Integer consumption = consumptionValues[i];
//
//			if (production != null && consumption != null) {
//
//			}
//
//		}
//
//		// if there is no enough production available.
//		if (this.proLessThanCon == null) {
//			this.proLessThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
//					.plusHours(config.maxEndHour());
//		}
//
//		if (this.proMoreThanCon == null) {
//			this.proMoreThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
//					.plusHours(config.maxStartHour()).plusDays(1);
//		}
//
//		// Print the boundary Hours
//		log.info("ProLessThanCon: " + this.proLessThanCon + " ProMoreThanCon " + this.proMoreThanCon);
//
//	}

	/**
	 * This method Calculates the boundary hours within which the whole logic works.
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
						&& (entry.getKey().getDayOfYear() == ZonedDateTime.now().getDayOfYear())) {
					this.proLessThanCon = entry.getKey();
				}

				// First hour of the day when production > consumption
				if ((production > consumption) //
						&& (entry.getKey().getDayOfYear() == ZonedDateTime.now().plusDays(1).getDayOfYear()) //
						&& (this.proMoreThanCon == null) //
						&& (entry.getKey().getHour() <= 10)) {
					this.proMoreThanCon = entry.getKey();
				}
			}
		}

		// if there is no production available, 'proLessThanCon' and 'proMoreThanCon'
		// are not calculated.
		if (this.proLessThanCon == null) {
			this.proLessThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
					.plusHours(config.maxEndHour());
		}

		if (this.proMoreThanCon == null) {
			this.proMoreThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
					.plusHours(config.maxStartHour()).plusDays(1);
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
