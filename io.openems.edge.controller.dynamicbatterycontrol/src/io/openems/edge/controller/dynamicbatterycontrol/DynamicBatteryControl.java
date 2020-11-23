package io.openems.edge.controller.dynamicbatterycontrol;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
		name = "Controller.dynamicbatterycontrol", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DynamicBatteryControl extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Config config = null;

	private final Logger log = LoggerFactory.getLogger(DynamicBatteryControl.class);
	private final Bci bci = new Bci();
	private boolean isPredictionValuesTaken = false;
	private boolean isAllowedToCalculateHours = false;
	private ZonedDateTime proLessThanCon = null;
	private ZonedDateTime proMoreThanCon = null;
	private List<ZonedDateTime> cheapHours = new ArrayList<ZonedDateTime>();

	// For Debugging
	private ZonedDateTime startHour = null;
	private Integer[] productionValues = new Integer[24];
	private Integer[] consumptionValues = new Integer[24];

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

	public DynamicBatteryControl() {
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

		ZonedDateTime now = ZonedDateTime.now();

		// Every Day at 14:00 the BCI list is updated, SO we receive the predictions at
		// 14:00 till next day 15:00.
		if (now.getHour() == 15 && !this.isPredictionValuesTaken) {
			Integer[] productionValues = productionHourlyPredictor.get24hPrediction().getValues();
			Integer[] consumptionValues = consumptionHourlyPredictor.get24hPrediction().getValues();
			ZonedDateTime startHour = productionHourlyPredictor.get24hPrediction().getStart();

			// resetting values
			this.cheapHours.clear();
			this.proLessThanCon = null;
			this.proMoreThanCon = null;

			// Storing in a global variables for later calculations during the hour when the
			// production is less than the consumption.
			this.productionValues = productionValues;
			this.consumptionValues = consumptionValues;
			this.startHour = startHour;

			// Print the Predicted Values
			for (int i = 0; i < 24; i++) {
				log.info("Production[" + i + "] " + " - " + productionValues[i] + " Consumption[" + i + "] " + " - "
						+ consumptionValues[i]);
			}

			// calculates the boundary hours, within which the controller needs to work
			this.calculateBoundaryHours(productionValues, consumptionValues, startHour);

			this.isAllowedToCalculateHours = true;
			this.isPredictionValuesTaken = true;

		}

		if (now.getHour() == 16 && this.isPredictionValuesTaken) {
			this.isPredictionValuesTaken = false;
		}

		if (this.proLessThanCon != null) {
			if (ZonedDateTime.now().getHour() == this.proLessThanCon.getHour() && this.isAllowedToCalculateHours) { // ***
				int nettCapacity = ess.getCapacity().getOrError();

				int availableCapacity = (nettCapacity * ess.getSoc().getOrError()) / 100;

				log.info("availableCapacity = " + availableCapacity);

				// Structuring the data in TreeMap format for easy calculations later
				TreeMap<ZonedDateTime, Integer> hourlyProduction = new TreeMap<ZonedDateTime, Integer>();
				TreeMap<ZonedDateTime, Integer> hourlyConsumption = new TreeMap<ZonedDateTime, Integer>();

				for (int i = 0; i < 24; i++) {
					if (this.consumptionValues[i] != null && this.productionValues[i] != null) {
						hourlyProduction.put(this.startHour.plusHours(i), this.productionValues[i]);
						hourlyConsumption.put(this.startHour.plusHours(i), this.consumptionValues[i]);
					}
				}

				if (hourlyProduction.isEmpty() || hourlyConsumption.isEmpty()) {
					return;
				}

				Integer requiredEnergy = this.calculateRequiredEnergy(availableCapacity, hourlyProduction,
						hourlyConsumption);

				log.info("requiredEnergy: " + requiredEnergy);

				if (requiredEnergy != null) {

					TreeMap<ZonedDateTime, Float> bciList = this.bci.houlryPrices(config.url());

					if (bciList != null) {

						// Printing Bci List
						for (Map.Entry<ZonedDateTime, Float> Entry : bciList.entrySet()) {
							log.info("Time: " + Entry.getKey() + " bci: " + Entry.getValue());
						}

						// list of hours, during which battery should be avoided.
						this.calculateTargetHours(requiredEnergy, hourlyConsumption, bciList);

						// Print the Cheap Hours calculated.
						this.cheapHours.forEach(value -> System.out.println(value));

					}
				}

				this.isAllowedToCalculateHours = false;

			}
		}

		// Avoiding Discharging during cheapest hours
		if (!this.cheapHours.isEmpty()) {
			for (ZonedDateTime entry : cheapHours) {
				if (now.getHour() == entry.getHour()) {
					// set result
					ess.addPowerConstraintAndValidate("DynamicBatteryControl", Phase.ALL, Pwr.ACTIVE,
							Relationship.EQUALS, 0);
				}
			}
		}

	}

	private Integer calculateRequiredEnergy(int availableCapacity, TreeMap<ZonedDateTime, Integer> hourlyProduction,
			TreeMap<ZonedDateTime, Integer> hourlyConsumption) {

		int consumptionTotal = 0;
		Integer requiredEnergy = null;

		if (hourlyConsumption.containsKey(proLessThanCon) && hourlyConsumption.containsKey(proMoreThanCon)) {
			for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.subMap(proLessThanCon, proMoreThanCon)
					.entrySet()) {
				consumptionTotal += entry.getValue() - hourlyProduction.get(entry.getKey());
			}

			// remaining amount of energy that should be covered from grid.
			requiredEnergy = consumptionTotal - availableCapacity;
		}

		return requiredEnergy;
	}

	// list of hours, during which battery should be avoided.
	private void calculateTargetHours(Integer requiredEnergy, TreeMap<ZonedDateTime, Integer> hourlyConsumption,
			TreeMap<ZonedDateTime, Float> bciList) {
		Float minPrice = Float.MAX_VALUE;
		Integer remainingEnergy = requiredEnergy;
		ZonedDateTime cheapTimeStamp = null;

		for (Map.Entry<ZonedDateTime, Float> entry : bciList.subMap(proLessThanCon, proMoreThanCon).entrySet()) {
			if (!this.cheapHours.contains(entry.getKey())) {
				if (entry.getValue() < minPrice) {
					cheapTimeStamp = entry.getKey();
					minPrice = entry.getValue();
				}
			}
		}

		if (cheapTimeStamp != null) {
			this.cheapHours.add(cheapTimeStamp);
			log.info("cheapTimeStamp: " + cheapTimeStamp);
		}

		// check -> consumption for cheap hour for previous day and compare with
		// remaining energy needed for today.
		for (Map.Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			if (!this.cheapHours.isEmpty()) {
				for (ZonedDateTime hours : cheapHours) {
					if (entry.getKey().getHour() == hours.getHour()) {
						remainingEnergy -= entry.getValue();
					}
				}
			}
		}

		// if we need more cheap hours
		if (remainingEnergy > 0) {
			this.calculateTargetHours(requiredEnergy, hourlyConsumption, bciList);
		}
	}

	private void calculateBoundaryHours(Integer[] productionValues, Integer[] consumptionValues,
			ZonedDateTime startHour) {

		for (int i = 0; i < 24; i++) {
			Integer production = productionValues[i];
			Integer consumption = consumptionValues[i];

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
					.plusHours(config.maxStratHour()).plusDays(1);
		}

		// Print the boundary Hours
		log.info("ProLessThanCon: " + this.proLessThanCon + " ProMoreThanCon " + this.proMoreThanCon);
	}
}
