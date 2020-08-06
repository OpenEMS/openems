package io.openems.edge.controller.dynamicdischarge;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
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
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.HourlyPredictor;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

public class DynamicDischarge extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, HourlyPredictor {

	private final Logger log = LoggerFactory.getLogger(DynamicDischarge.class);
	private final Prices prices = new Prices();

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	private Config config = null;
	private boolean isTargetHoursCalculated = false;
	private Integer remainingCapacity = 0;
	private LocalDateTime startHour = null;
	private LocalDateTime proLessThanCon = null;
	private LocalDateTime proMoreThanCon = null;
	private LocalDateTime cheapTimeStamp = null;
//	private Integer[] productionValues = new Integer[24];
//	private Integer[] consumptionValues = new Integer[24];
	private List<LocalDateTime> cheapHours = new ArrayList<LocalDateTime>();

	public DynamicDischarge() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	public DynamicDischarge(Integer[] productionValues, Integer[] consumptionValues, LocalDateTime startHour) {

		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
		);

//		this.productionValues = productionValues;
//		this.consumptionValues = consumptionValues;
		this.startHour = startHour;
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BUYING_FROM_GRID(Doc.of(Level.INFO)//
				.text("The controller selfoptimization ran succefully"));
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
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
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		LocalDateTime now = LocalDateTime.now();
		int nettcapacity = ess.getCapacity().value().getOrError();
		Integer availableCapacity = (100 / ess.getSoc().value().getOrError()) * nettcapacity;

		if (now.getHour() == this.config.startHour() && !isTargetHoursCalculated) {
			Integer[] productionValues = productionHourlyPredictor.get24hPrediction().getValues();
			Integer[] consumptionValues = consumptionHourlyPredictor.get24hPrediction().getValues();
			this.startHour = productionHourlyPredictor.get24hPrediction().getStart();

			TreeMap<LocalDateTime, Integer> hourlyProduction = new TreeMap<LocalDateTime, Integer>();
			TreeMap<LocalDateTime, Integer> hourlyConsumption = new TreeMap<LocalDateTime, Integer>();

			for (int i = 0; i <= 24; i++) {
				if (consumptionValues[i] != null && productionValues[i] != null) {
					hourlyProduction.put(startHour.plusHours(i), productionValues[i]);
					hourlyConsumption.put(startHour.plusHours(i), consumptionValues[i]);
				}
			}

			// resetting values
			this.cheapHours.clear();
			this.remainingCapacity = 0;
			this.proLessThanCon = null;
			this.proMoreThanCon = null;

			TreeMap<LocalDateTime, Float> hourlyPrices = this.prices.houlryPrices(config.url(), config.apikey());
			
			if(hourlyPrices==null) {
				return;
			}

			// calculates the boundary hours, within which the controller needs to work
			this.calculateBoundaryHours(hourlyProduction, hourlyConsumption);

			// if the target hours are calculated.
			if (this.proLessThanCon != null && this.proMoreThanCon != null) {

				this.calculateRemainingCapacity(availableCapacity, hourlyProduction, hourlyConsumption);

				// list of hours, during which battery is avoided.
				this.calculateTargetHours(hourlyConsumption, hourlyPrices);
				this.isTargetHoursCalculated = true;
			}
		}

		if (now.getHour() == this.config.startHour() + 1 && this.isTargetHoursCalculated) {
			this.isTargetHoursCalculated = false;
		}

		// Avoiding Discharging during cheapest hours
		if (!this.cheapHours.isEmpty()) {
			for (LocalDateTime entry : cheapHours) {
				if (now.getHour() == entry.getHour()) {
					// set result
					ess.addPowerConstraintAndValidate("SymmetricActivePower", Phase.ALL, Pwr.ACTIVE,
							Relationship.EQUALS, 0);
					this.channel(ChannelId.BUYING_FROM_GRID).setNextValue(true);
				}
			}
		} else {
			this.channel(ChannelId.BUYING_FROM_GRID).setNextValue(false);
		}
	}

	private void calculateBoundaryHours(TreeMap<LocalDateTime, Integer> hourlyProduction,
			TreeMap<LocalDateTime, Integer> hourlyConsumption) {

		for (LocalDateTime key : hourlyProduction.keySet()) {
			int production = hourlyProduction.get(key);
			int consumption = hourlyConsumption.get(key);

			// last hour of the day when production was greater than consumption
			if ((production > consumption) //
					&& (this.startHour.getDayOfYear() == LocalDateTime.now().getDayOfYear())) {
				this.proLessThanCon = key;
			}

			// First hour of the day when production was greater than consumption
			if ((production > consumption) //
					&& (this.startHour.plusDays(1).getDayOfYear() == LocalDateTime.now().getDayOfYear())
					&& (this.proMoreThanCon == null)) {
				this.proMoreThanCon = key;
			}
		}

		// if there is no enough production available.
		if (this.proLessThanCon == null) {
			LocalDateTime now = LocalDateTime.now();
			this.proLessThanCon = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
					.plusHours(config.Max_Evening_hour());
		}
		if (this.proMoreThanCon == null) {
			LocalDateTime now = LocalDateTime.now();
			this.proMoreThanCon = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
					.plusHours(config.Max_Evening_hour()).plusDays(1);
		}
	}

	private void calculateRemainingCapacity(int availableCapacity, TreeMap<LocalDateTime, Integer> hourlyProduction,
			TreeMap<LocalDateTime, Integer> hourlyConsumption) {

		int consumptionTotal = 0;

		for (Entry<LocalDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			if (entry.getKey().isAfter(this.proLessThanCon) && entry.getKey().isBefore(this.proMoreThanCon)) {

				// TODO Confirm production is needed here?
				consumptionTotal += entry.getValue() - hourlyProduction.get(entry.getKey());
			}
		}

		// remaining amount of energy that should be covered from grid.
		this.remainingCapacity = consumptionTotal - availableCapacity;
	}
	
//	@SuppressWarnings("unused")
//	private void calculateBoundaryhours(Integer[] productionValues, Integer[] consumptionValues,
//			LocalDateTime startHour) {
//
//		for (int i = 0; i < 24; i++) {
//			Integer production = productionValues[i];
//			Integer consumption = consumptionValues[i];
//
//			if (production != null || consumption != null) {
//
//				// last hour of the day when production was greater than consumption
//				if ((production > consumption) //
//						&& (startHour.getDayOfYear() == LocalDateTime.now().getDayOfYear())) {
//					this.proLessThanCon = startHour.plusHours(i);
//				}
//
//				// First hour of the day when production was greater than consumption
//				if ((production > consumption) //
//						&& (startHour.plusDays(1).getDayOfYear() == LocalDateTime.now().getDayOfYear())
//						&& (this.proMoreThanCon == null)) {
//					this.proMoreThanCon = startHour.plusHours(i);
//				}
//			}
//		}
//
//		// if there is no enough production available.
//		if (this.proLessThanCon == null) {
//			LocalDateTime now = LocalDateTime.now();
//			this.proLessThanCon = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
//					.plusHours(config.MaxEveningHour());
//		}
//		if (this.proMoreThanCon == null) {
//			LocalDateTime now = LocalDateTime.now();
//			this.proMoreThanCon = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
//					.plusHours(config.MaxMorningHour()).plusDays(1);
//		}
//	}

//	@SuppressWarnings("unused")
//	private TreeMap<LocalDateTime, Integer> calculateAdjustedHourlyProduction(int availableCapacity) {
//
//		TreeMap<LocalDateTime, Integer> hourlyPro = new TreeMap<LocalDateTime, Integer>();
//		int j = productionValues.length;
//		float diffRate = 0;
//		int value = 0;
//		for (int i = 0; i <= productionValues.length; i++) {
//			if (productionValues[i] > productionValues[j]) {
//				diffRate = productionValues[i] / productionValues[j];
//				value = (int) (((diffRate * productionValues[i]) + productionValues[j]) / (diffRate + 1));
//			} else {
//				diffRate = productionValues[j] / productionValues[i];
//				value = (int) (((diffRate * productionValues[j]) + productionValues[i]) / (diffRate + 1));
//			}
//			hourlyPro.put(startHour.plusHours(i), value);
//			j = j - 1;
//		}
//		return hourlyPro;
//	}

	// list of hours, during which battery is avoided.
	private void calculateTargetHours(TreeMap<LocalDateTime, Integer> hourlyConsumption,
			TreeMap<LocalDateTime, Float> hourlyPrices) {
		Float minPrice = Float.MAX_VALUE;
		Integer remainingEnergy = this.remainingCapacity;

			for (Map.Entry<LocalDateTime, Float> entry : hourlyPrices.entrySet()) {
				if (!this.cheapHours.contains(entry.getKey())) {
					if (entry.getValue() < minPrice) {
						this.cheapTimeStamp = entry.getKey();
						minPrice = entry.getValue();
					}
				}
			}
		this.cheapHours.add(this.cheapTimeStamp);
		log.info("cheapTimeStamp: " + this.cheapTimeStamp);

		// check -> consumption for cheap hour for previous day and compare with
		// remaining energy needed for today.
		for (Map.Entry<LocalDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			if (!this.cheapHours.isEmpty()) {
				for (LocalDateTime hours : cheapHours) {
					if (entry.getKey().getHour() == hours.getHour()) {
						remainingEnergy -= entry.getValue();
					}
				}
			}
		}

		// if we need more cheap hours
		if (remainingEnergy > 0) {
			this.calculateTargetHours(hourlyConsumption, hourlyPrices);
		}
	}

	@Override
	public HourlyPrediction get24hPrediction() {
		return null;
	}
}
