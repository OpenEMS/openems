package io.openems.edge.energy;

public class Utils {

	private Utils() {

	}

//	public static EssFixActivePower.ScheduleMode[] createEssFixActivePowerScheduleFromConfig(Config config) {
//		return new EssFixActivePower.ScheduleMode[] { config.essFix00(), config.essFix01(), config.essFix02(),
//				config.essFix03(), config.essFix04(), config.essFix05(), config.essFix06(), config.essFix07(),
//				config.essFix08(), config.essFix09(), config.essFix10(), config.essFix11(), config.essFix12(),
//				config.essFix13(), config.essFix14(), config.essFix15(), config.essFix16(), config.essFix17(),
//				config.essFix18(), config.essFix19(), config.essFix20(), config.essFix21(), config.essFix22(),
//				config.essFix23() };
//	}

//	public static EvcsController.ScheduleMode[] createEvcsScheduleFromConfig(Config config) {
//		return new EvcsController.ScheduleMode[] { config.evcs00(), config.evcs01(), config.evcs02(), config.evcs03(),
//				config.evcs04(), config.evcs05(), config.evcs06(), config.evcs07(), config.evcs08(), config.evcs09(),
//				config.evcs10(), config.evcs11(), config.evcs12(), config.evcs13(), config.evcs14(), config.evcs15(),
//				config.evcs16(), config.evcs17(), config.evcs18(), config.evcs19(), config.evcs20(), config.evcs21(),
//				config.evcs22(), config.evcs23() };
//	}

//	/**
//	 * Sums quarterly values (array of 96 Integer values) to hourly values (array of
//	 * 24 Integer values).
//	 * 
//	 * @param values
//	 * @return
//	 */
//	public static Integer[] sumQuartersToHours(Integer[] values) {
//		List<Integer> result = new ArrayList<>();
//		Integer sum = null;
//		for (var i = 0; i < values.length; i++) {
//			if (i % 4 == 0) {
//				sum = null;
//			}
//			sum = TypeUtils.sum(sum, values[i]);
//			if (i % 4 == 3) {
//				result.add(sum);
//			}
//		}
//		return result.stream().toArray(Integer[]::new);
//	}

//	/**
//	 * Averages quarterly values (array of 96 Float values) to hourly values (array
//	 * of 24 Float values).
//	 * 
//	 * @param values
//	 * @return
//	 */
//	public static Float[] avgQuartersToHours(Float[] values) {
//		List<Float> result = new ArrayList<>();
//		Float sum = 0F;
//		for (var i = 0; i < values.length; i++) {
//			if (i % 4 == 0) {
//				sum = 0F;
//			}
//			sum = TypeUtils.sum(sum, values[i]);
//			if (i % 4 == 3) {
//				if (sum == null) {
//					result.add(null);
//				} else {
//					result.add(sum / 4);
//				}
//			}
//		}
//		return result.stream().toArray(Float[]::new);
//	}

//	private static float calculateRevenueStandardizationFactor(Forecast forecast) {
//	return Math.max(
//			// Max revenue value
//			Stream.of(forecast.periods) //
//					.mapToInt(p -> p.production * p.sellToGridRevenue) //
//					.sum(),
//			// Max cost value
//			Stream.of(forecast.periods) //
//					.mapToInt(p -> p.consumption * p.buyFromGridCost) //
//					.sum())
//			/ STANDARDIZATION_INTERVAL;
//}
//
///**
// * Calculate target energy from Facts.FUTURE_PERIODS
// * 
// * @param forecast
// * @param devices
// * @return
// */
//private static float calculateTargetStorageEnergyInFinalPeriod(Forecast forecast, List<Device> devices) {
////	var ess = (Ess) devices.stream().filter(e -> e instanceof Ess).findFirst().get(); // Possible NPE: no ESS
//
//	// Total excess energy in FUTURE_PERIODs
//	var excessEnergy = Stream.of(forecast.periods) //
//			.skip(Facts.PLANNING_PERIODS) //
//			// Calculate excess energy per period
//			.mapToInt(p -> p.production - p.consumption) //
//			.sum();
//	final int targetStorageEnergy;
//	if (excessEnergy > 0) {
//		// Keep space in storage to charge excess energy
////		targetStorageEnergy = ess.capacity - excessEnergy; TODO
//		targetStorageEnergy = 0;
//	} else {
//		// Keep energy in storage to allow discharge of missing energy
//		targetStorageEnergy = excessEnergy * -1;
//	}
//
//	// TODO standardize
//	return
//	// Max value: ESS Capacity
////	Math.min(ess.capacity,
////			// Min value: 0
////			Math.max(0, targetStorageEnergy));
//	0; // TODO
//}
}
