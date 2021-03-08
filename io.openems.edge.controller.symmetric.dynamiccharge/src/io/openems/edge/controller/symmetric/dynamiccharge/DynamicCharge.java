package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.ZonedDateTime;
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

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.predictor.api.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.Dynamiccharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DynamicCharge extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Config config = null;
	private static TreeMap<ZonedDateTime, Float> hourlyPrices = new TreeMap<>();
	private static TreeMap<ZonedDateTime, Integer> chargeSchedule = new TreeMap<ZonedDateTime, Integer>();

	private boolean isPredictionValuesTaken = false;

	private final Logger log = LoggerFactory.getLogger(DynamicCharge.class);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private ZonedDateTime proLessThanCon = null;
	private ZonedDateTime proMoreThanCon = null;
	private static TreeMap<ZonedDateTime, Integer> hourlyConsumption = new TreeMap<ZonedDateTime, Integer>();
	private static TreeMap<ZonedDateTime, Integer> hourlyProduction = new TreeMap<ZonedDateTime, Integer>();
	private boolean isAllowedToCalculateHours = false;
	private static int nettCapacity;
	private static int maxApparentPower;
	private float currentMinPrice;

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

		ZonedDateTime now = ZonedDateTime.now();

		// Every Day at 14:00 the Hourly Prices are updated.
		// we receive the predictions at 14:00 till next day 13:00.
		// isPredictionValuesTaken is there to make sure the control logic executes only
		// once during the hour.
		if (now.getHour() == 14 && !this.isPredictionValuesTaken) {
			Integer[] productionValues = this.productionHourlyPredictor.get24hPrediction().getValues();
			Integer[] consumptionValues = this.consumptionHourlyPredictor.get24hPrediction().getValues();
			ZonedDateTime startHour = this.productionHourlyPredictor.get24hPrediction().getStart();

			// resetting values
			chargeSchedule.clear();
			this.proLessThanCon = null;
			this.proMoreThanCon = null;

			// calculates the boundary hours, within which the controller needs to work
			// Boundary hours = Production < consumption, Consumption > production.
			this.calculateBoundaryHours(productionValues, consumptionValues, startHour);

			// Print the Predicted Values
			for (int i = 0; i < 24; i++) {
				log.info("Production[" + i + "] " + " - " + productionValues[i] + " Consumption[" + i + "] " + " - "
						+ consumptionValues[i]);

				// Storing in a global variables for later calculations during the hour when the
				// production is less than the consumption.
				// Structuring the data in TreeMap format for easy calculations later
				if (consumptionValues[i] != null && productionValues[i] != null) {
					hourlyProduction.put(startHour.plusHours(i), productionValues[i]);
					hourlyConsumption.put(startHour.plusHours(i), consumptionValues[i]);
				}
			}

			if (hourlyProduction.isEmpty() || hourlyConsumption.isEmpty()) {
				return;
			}

			this.isAllowedToCalculateHours = true; // used to schedule only once
			this.isPredictionValuesTaken = true; // Used to take prediction values only once.

		}

		// Resetting the isPredictionValuesTaken to make it ready to execute next day.
		if (now.getHour() == 15 && this.isPredictionValuesTaken) {
			this.isPredictionValuesTaken = false;
		}

		// Start scheduling exactly when Production is less than Consumption
		if (this.proLessThanCon != null) {
			if (ZonedDateTime.now().getHour() == this.proLessThanCon.getHour() && this.isAllowedToCalculateHours) {

				nettCapacity = ess.getCapacity().getOrError();
				maxApparentPower = ess.getMaxApparentPower().getOrError();
				int availableCapacity = (nettCapacity * ess.getSoc().getOrError()) / 100;

				log.info("availableCapacity = " + availableCapacity + " nett capacity " + nettCapacity
						+ " Max Apparent Power " + maxApparentPower);

				// Required Energy that needs to be taken from grid
				// Required Energy = Total Consumption - Available Battery capacity
				Integer totalConsumption = this.calculateTotalConsumption(proLessThanCon, proMoreThanCon);

				Integer remainingEnergy = totalConsumption - availableCapacity;

				log.info("totalConsumption = " + totalConsumption + " remainingEnergy " + remainingEnergy);

				if (remainingEnergy > 0) {

					log.info("Collecting hourly Prices: ");

					hourlyPrices = PriceApi.houlryPrices();

					hourlyPrices.entrySet().forEach(p -> {
						log.info("hour" + p.getKey() + " value: " + p.getValue());
					});

					log.info(" Getting schedule: ");

					getChargeSchedule(this.proLessThanCon, this.proMoreThanCon.plusHours(1), totalConsumption,
							availableCapacity);
				}
			}
			this.isAllowedToCalculateHours = false;
		}

		if (!chargeSchedule.isEmpty()) {
			for (Entry<ZonedDateTime, Integer> entry : chargeSchedule.entrySet()) {
				if (now.getHour() == entry.getKey().getHour()) {

					int power = entry.getValue() * -1;

					int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
							power);
					ess.addPowerConstraintAndValidate("SymmetricDynamicChargePower", Phase.ALL, Pwr.ACTIVE,
							Relationship.EQUALS, calculatedPower);
				}
			}
		}
	}

	private void getChargeSchedule(ZonedDateTime proLessThanCon, ZonedDateTime proMoreThanCon, Integer totalConsumption,
			Integer availableEnergy) throws InvalidValueException {

		// function to find the minimum priceHour
		ZonedDateTime cheapestHour = cheapHour(proLessThanCon, proMoreThanCon);

		Integer demandTillCheapestHour = calculateDemandTillThishour(proLessThanCon, cheapestHour);

		Integer currentHourConsumption = hourlyConsumption.get(cheapestHour);

		Integer remainingConsumption = 0;

		// Calculates the amount of energy that needs to be charged during the cheapest
		// price hours.

		if (totalConsumption > 0) {

			// if the battery has sufficient energy!
			if (availableEnergy >= demandTillCheapestHour) {
				totalConsumption -= availableEnergy;
				adjustRemainigConsumption(cheapestHour, proMoreThanCon, totalConsumption, availableEnergy,
						demandTillCheapestHour);
			} else {
				// if the battery does not has sufficient energy!
				Integer chargebleConsumption = totalConsumption - demandTillCheapestHour - currentHourConsumption;

				if (chargebleConsumption > 0) {
					if (chargebleConsumption > maxApparentPower) {

						ZonedDateTime lastCheapTimeStamp = cheapestHour;

						// checking for next cheap hour if it is before or after the first cheapest
						// hour.
						
						// From evening to the cheapest hour
						cheapestHour = cheapHour(proLessThanCon, cheapestHour);
						float firstMinPrice = currentMinPrice;

						// From cheapest hour to the morning of the next day.
						cheapestHour = cheapHour(lastCheapTimeStamp.plusHours(1), proMoreThanCon);

						if (currentMinPrice < firstMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
									hourlyConsumption.lastKey().plusDays(1), remainingConsumption, maxApparentPower,
									demandTillCheapestHour);
						} else {
							if (chargebleConsumption > nettCapacity) {
								remainingConsumption = chargebleConsumption - nettCapacity;
								adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
										hourlyConsumption.lastKey().plusDays(1), remainingConsumption, nettCapacity,
										demandTillCheapestHour);
							}

						}

						cheapestHour = lastCheapTimeStamp;
						chargebleConsumption = maxApparentPower;

					}
					totalConsumption = totalConsumption - chargebleConsumption - currentHourConsumption
							- remainingConsumption;
					remainingConsumption = 0;

					// adding into charge Schedule
					chargeSchedule.put(cheapestHour, chargebleConsumption);
					getChargeSchedule(proLessThanCon, cheapestHour, totalConsumption, availableEnergy);

				} else {
					totalConsumption -= currentHourConsumption;
					getChargeSchedule(proLessThanCon, cheapestHour, totalConsumption, availableEnergy);
				}

			}

		}

	}

	private void adjustRemainigConsumption(ZonedDateTime cheapestHour, ZonedDateTime proMoreThanCon,
			Integer remainingConsumption, Integer availableEnergy, Integer demandTillCheapestHour)
			throws InvalidValueException {

		if (!cheapestHour.isEqual(proMoreThanCon)) {

			if (remainingConsumption > 0) {

				ZonedDateTime cheapTimeStamp = cheapHour(cheapestHour, proMoreThanCon);
				Integer currentHourConsumption = hourlyConsumption.get(cheapTimeStamp);

				if (demandTillCheapestHour > availableEnergy) {
					demandTillCheapestHour -= availableEnergy;
					availableEnergy = 0;
				} else {
					availableEnergy -= demandTillCheapestHour;
					demandTillCheapestHour = 0;
				}

				Integer allowedConsumption = nettCapacity - availableEnergy;

				if (allowedConsumption > 0) {
					if (allowedConsumption > maxApparentPower) {
						allowedConsumption = maxApparentPower;
					}
					remainingConsumption = remainingConsumption - currentHourConsumption - demandTillCheapestHour;

					if (remainingConsumption > 0) {
						if (remainingConsumption > allowedConsumption) {
							remainingConsumption -= allowedConsumption;
							availableEnergy += allowedConsumption;

							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, allowedConsumption);
							adjustRemainigConsumption(cheapTimeStamp.plusHours(1), proMoreThanCon, remainingConsumption,
									availableEnergy, demandTillCheapestHour);
						} else {
							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, remainingConsumption);
						}
					}

				} else {

					availableEnergy -= currentHourConsumption;
					adjustRemainigConsumption(cheapTimeStamp.plusHours(1), proMoreThanCon, remainingConsumption,
							availableEnergy, demandTillCheapestHour);
				}

			}

		}

	}

	private static Integer calculateDemandTillThishour(ZonedDateTime proLessThanCon, ZonedDateTime cheapestHour) {
		Integer demand = 0;

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			if ((entry.getKey().isEqual(proLessThanCon) || entry.getKey().isAfter(proLessThanCon))
					&& entry.getKey().isBefore(cheapestHour)) {
				demand += entry.getValue();
			}
		}
		return demand;
	}

	private ZonedDateTime cheapHour(ZonedDateTime proLessThanCon, ZonedDateTime proMoreThanCon) {
		float minPrice = Float.MAX_VALUE;
		ZonedDateTime cheapTimeStamp = null;

		for (Entry<ZonedDateTime, Float> entry : hourlyPrices.subMap(proLessThanCon, proMoreThanCon).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}

		this.currentMinPrice = minPrice;
		return cheapTimeStamp;
	}

	private Integer calculateTotalConsumption(ZonedDateTime proLessThanCon, ZonedDateTime proMoreThanCon) {
		int consumptionTotal = 0;

		if (hourlyConsumption.containsKey(proLessThanCon) && hourlyConsumption.containsKey(proMoreThanCon)) {
			for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.subMap(proLessThanCon, proMoreThanCon)
					.entrySet()) {
				consumptionTotal += entry.getValue() - hourlyProduction.get(entry.getKey());
			}
		}
		return consumptionTotal;
	}

	// list of hours, during which battery should be avoided.
	private void calculateBoundaryHours(Integer[] productionValues, Integer[] consumptionValues,
			ZonedDateTime startHour) {
		for (int i = 0; i < 24; i++) {
			Integer production = productionValues[i];
			Integer consumption = consumptionValues[i];

			// Detects the last time when the production is less than consumption and first
			// time when the production is greater than consumption.
			if (production != null && consumption != null) {
				// last hour of the day when production was greater than consumption
				if ((production > consumption) //
						&& (startHour.plusHours(i).getDayOfYear() == ZonedDateTime.now().getDayOfYear())) {
					this.proLessThanCon = startHour.plusHours(i);
				}

				// First hour of the day when production was greater than consumption
				if ((production > consumption) //
						&& (startHour.plusHours(i).getDayOfYear() == ZonedDateTime.now().plusDays(1).getDayOfYear()) //
						&& (this.proMoreThanCon == null) //
						&& (startHour.plusHours(i).getHour() <= 10)) {
					this.proMoreThanCon = startHour.plusHours(i);
				}
			}
		}

		// if there is no enough production available.
		if (this.proLessThanCon == null) {
			this.proLessThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
					.plusHours(config.maxEndHour());
		}

		if (this.proMoreThanCon == null) {
			this.proMoreThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
					.plusHours(config.maxStartHour()).plusDays(1);
		}

		// Print the boundary Hours
		log.info("ProLessThanCon: " + this.proLessThanCon + " ProMoreThanCon " + this.proMoreThanCon);

	}
}
