package io.openems.edge.predictor.persistant.model;

import java.time.LocalDateTime;
import java.util.TreeMap;


//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractCycleWorker;

import io.openems.edge.common.sum.Sum;

public class DataCollectorWorker extends AbstractCycleWorker {

//  private final Logger log = LoggerFactory.getLogger(DataCollectorWorker.class);

	private final PersistantModel parent;

//	private LocalDate dateOfT0 = null;
//	private long totalConsumption = 0;
//	private long currentConsumption = 0;
//	private long currentProduction = 0;
//	private static LocalDateTime t0 = null;
//	private static LocalDateTime t1 = null;
//	private LocalDate dateOfLastRun = null;
//	private LocalDateTime currentHour = null;
//
//	private int Max_Morning_hour = 5;
//	private int Max_Evening_hour = 17;

	private static TreeMap<LocalDateTime, Long> PreviosDayPred = new TreeMap<LocalDateTime, Long>();
	
	
	public static TreeMap<LocalDateTime, Long> getPreviosDayPred() {
		return PreviosDayPred;
	}

	public static void setPreviosDayPred(TreeMap<LocalDateTime, Long> previosDayPred) {
		PreviosDayPred = previosDayPred;
	}

	public DataCollectorWorker(PersistantModel parent) {
		this.parent = parent;
	}

//	private enum State {
//
//		PRODUCTION_LOWER_THAN_CONSUMPTION, PRODUCTION_DROPPED_BELOW_CONSUMPTION, PRODUCTION_HIGHER_THAN_CONSUMPTION,
//		PRODUCTION_EXCEEDED_CONSUMPTION
//	}
//
//	private State currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
	protected int counter = 0;
	public TreeMap<LocalDateTime, Long> hourlyConsumption = new TreeMap<LocalDateTime, Long>();

	public void calculateConsumption(Sum sum, LocalDateTime start, LocalDateTime end) {
		
		
		LocalDateTime nowDate = LocalDateTime.now();
		System.out.println("now date --> "+ nowDate.toString());
		System.out.println("start --> "+ start.toString());
		System.out.println("end --->" + end.toString());
		// First hour
		System.out.println("hourlyConsumption size-->" + hourlyConsumption.size());
		if (counter == 0  && nowDate.getHour() == start.getHour() && nowDate.getMinute() == start.getMinute() ) {
			long consumptionEnergy = sum.getConsumptionActiveEnergy().value().orElse(0L);
			hourlyConsumption.put(nowDate, consumptionEnergy);			
			counter++;
		}
		//Last hour
		else if (counter == 24  && nowDate.getHour() == end.getHour() && nowDate.getMinute() == end.getMinute() ) {
			//LocalDateTime newDate = start.plusHours(counter);
			LocalDateTime newDate = start.plusMinutes(counter);
			long consumptionEnergy = sum.getConsumptionActiveEnergy().value().orElse(0L);
			hourlyConsumption.put(newDate, consumptionEnergy);
			counter = 0;
			setPreviosDayPred(hourlyConsumption);	
			hourlyConsumption.clear();
		}
		// In between
		else {
			//LocalDateTime newDate = start.plusHours(counter);
			LocalDateTime newDate = start.plusMinutes(counter);
			long consumptionEnergy = sum.getConsumptionActiveEnergy().value().orElse(0L);
			hourlyConsumption.put(newDate, consumptionEnergy);
			counter++;
		}
		
		
		
		
		
		
		


//		int production = sum.getProductionActivePower().value().orElse(0);
//		int consumption = sum.getConsumptionActivePower().value().orElse(0);
//		long productionEnergy = sum.getProductionActiveEnergy().value().orElse(0L);
//		long consumptionEnergy = sum.getConsumptionActiveEnergy().value().orElse(0L);
//
//		
//		LocalDateTime now = LocalDateTime.now();
//
//		if (!hourlyConsumption.isEmpty()) {
//			log.info("first Key: " + hourlyConsumption.firstKey() + " last Key: " + hourlyConsumption.lastKey());
//		}
//
//		switch (currentState) {
//		case PRODUCTION_LOWER_THAN_CONSUMPTION:
//			log.info(" State: " + currentState);
//
//			if (t0 != null) {
//
//				// First time of the day when production > consumption.
//				// Avoids the fluctuations and shifts to next state only the next day.
//				// to avoid exceptional cases (production value might be minus during night)
//				if ((now.getHour() >= Max_Morning_hour) && dateOfT0.isBefore(nowDate)) {
//					if (production > consumption || now.getHour() > Max_Morning_hour) {
//						log.info(production + " is greater than " + consumption
//								+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
//						this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
//					}
//				}
//
//				// Detects the switching of hour
//				if (now.getHour() == currentHour.plusHours(1).getHour()) {
//					log.info(" Switching of the hour detected and updating " + " [ " + currentHour + " ] ");
//					this.totalConsumption = (consumptionEnergy - currentConsumption)
//							- (productionEnergy - currentProduction);
//					hourlyConsumption.put(currentHour.withNano(0).withMinute(0).withSecond(0), totalConsumption);
//					this.currentConsumption = consumptionEnergy;
//					this.currentProduction = productionEnergy;
//					currentHour = now;
//				}
//
//				log.info(" Total Consumption: " + totalConsumption);
//
//				// condition for initial run.
//			} else if (production > consumption || now.getHour() >= Max_Morning_hour) {
//				this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
//			}
//			break;
//
//		case PRODUCTION_EXCEEDED_CONSUMPTION:
//			log.info(" State: " + currentState);
//			if (t1 == null && t0 != null) {
//
//				// This is the first time of the day that "production > consumption".
//				this.totalConsumption = (consumptionEnergy - currentConsumption)
//						- (productionEnergy - currentProduction);
//				hourlyConsumption.put(now.withNano(0).withMinute(0).withSecond(0), totalConsumption);
//				t1 = now;
//				log.info(" t1 is set: " + t1);
//
//				// reset values
//				log.info("Resetting Values during " + now);
//				t0 = null;
//
//				this.dateOfLastRun = nowDate;
//				log.info("dateOfLastRun " + dateOfLastRun);
//			}
//
//			log.info(production + " is greater than " + consumption
//					+ " so switching the state from PRODUCTION EXCEEDING CONSUMPTION to PRODUCTION HIGHER THAN CONSUMPTION ");
//			this.currentState = State.PRODUCTION_HIGHER_THAN_CONSUMPTION;
//			break;
//
//		case PRODUCTION_HIGHER_THAN_CONSUMPTION:
//			log.info(" State: " + currentState);
//
//			// avoid switching to next state during the day.
//			if (production < consumption && now.getHour() >= Max_Evening_hour) {
//				log.info(production + " is lesser than " + consumption
//						+ " so switching the state from PRODUCTION HIGHER THAN CONSUMPTION to PRODUCTION DROPPED BELOW CONSUMPTION ");
//				this.currentState = State.PRODUCTION_DROPPED_BELOW_CONSUMPTION;
//			}
//			break;
//
//		case PRODUCTION_DROPPED_BELOW_CONSUMPTION:
//			log.info(" State: " + currentState);
//
//			t0 = now;
//			this.dateOfT0 = nowDate;
//			log.info("t0 is set at: " + dateOfT0);
//			currentHour = now;
//			currentConsumption = consumptionEnergy;
//			currentProduction = productionEnergy;
//
//			// avoids the initial run
//			if (dateOfLastRun != null) {
//				t1 = null;
//			}
//
//			// Resetting Values
//			log.info(production + "is lesser than" + consumption
//					+ "so switching the state from PRODUCTION DROPPED BELOW CONSUMPTION to PRODUCTION LOWER THAN CONSUMPTION");
//			hourlyConsumption.clear();
//			this.currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
//			break;
//		}

	}

	@Override
	protected void forever() throws Throwable {
		calculateConsumption(parent.sum, parent.start, parent.end);
	}

	@Override
	public void triggerNextRun() {
		super.triggerNextRun();
	}

}
