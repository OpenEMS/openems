package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.calculateCost;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.logSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.initializeRandomRegistryForUnitTest;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public class IntegrationTests {

	@Before
	public void before() {
		initializeRandomRegistryForUnitTest();
	}

	/**
	 * Two price peaks; should CHARGE_GRID during first peak.
	 */
	@Ignore
	@Test
	public void test1() {
		var log = """
				Params [numberOfPeriods=113, time=2024-02-27T19:45+01:00[Europe/Berlin], essTotalEnergy=22000, essMinSocEnergy=0, essMaxSocEnergy=19800, essInitialEnergy=0, essMaxEnergyPerPeriod=2565, essChargeInChargeGrid=2475, maxBuyFromGrid=5750, states=[BALANCING, DELAY_DISCHARGE, CHARGE_GRID]]
				Time  Production Consumption EssInitial State             EssChargeDischarge  Grid Price  Cost
				19:45          0        1164          0 BALANCING                          0  1164 294.00 0.3422
				20:15          0        2623          0 BALANCING                          0  2623 269.30 0.7064
				20:30          0        1292          0 BALANCING                          0  1292 269.30 0.3479
				20:45          0        1001          0 BALANCING                          0  1001 269.30 0.2696
				21:00          0         383          0 BALANCING                          0   383 255.00 0.0977
				21:15          0         375          0 BALANCING                          0   375 255.00 0.0956
				21:30          0        1129          0 BALANCING                          0  1129 255.00 0.2879
				21:45          0         512          0 BALANCING                          0   512 255.00 0.1306
				22:00          0         384          0 BALANCING                          0   384 252.50 0.0970
				22:15          0        1265          0 BALANCING                          0  1265 252.50 0.3194
				22:30          0         316          0 BALANCING                          0   316 252.50 0.0798
				22:45          0         565          0 BALANCING                          0   565 252.50 0.1427
				23:00          0         505          0 BALANCING                          0   505 246.20 0.1243
				23:15          0         348          0 BALANCING                          0   348 246.20 0.0857
				23:30          0         739          0 BALANCING                          0   739 246.20 0.1819
				23:45          0         513          0 BALANCING                          0   513 246.20 0.1263
				00:00          0         158          0 BALANCING                          0   158 240.70 0.0380
				00:15          0         181          0 BALANCING                          0   181 240.70 0.0436
				00:30          0         356          0 BALANCING                          0   356 240.70 0.0857
				00:45          0         455          0 BALANCING                          0   455 240.70 0.1095
				01:00          0         256          0 BALANCING                          0   256 239.60 0.0613
				01:15          0         360          0 BALANCING                          0   360 239.60 0.0863
				01:30          0         139          0 BALANCING                          0   139 239.60 0.0333
				01:45          0         308          0 BALANCING                          0   308 239.60 0.0738
				02:00          0         320          0 BALANCING                          0   320 241.90 0.0774
				02:15          0         259          0 BALANCING                          0   259 241.90 0.0627
				02:30          0         223          0 BALANCING                          0   223 241.90 0.0539
				02:45          0         288          0 BALANCING                          0   288 241.90 0.0697
				03:00          0         269          0 BALANCING                          0   269 244.40 0.0657
				03:15          0         187          0 BALANCING                          0   187 244.40 0.0457
				03:30          0         230          0 BALANCING                          0   230 244.40 0.0562
				03:45          0         556          0 BALANCING                          0   556 244.40 0.1359
				04:00          0         571          0 BALANCING                          0   571 244.70 0.1397
				04:15          0         326          0 BALANCING                          0   326 244.70 0.0798
				04:30          0         219          0 BALANCING                          0   219 244.70 0.0536
				04:45          0         243          0 BALANCING                          0   243 244.70 0.0595
				05:00          0         108          0 BALANCING                          0   108 249.10 0.0269
				05:15          0         367          0 BALANCING                          0   367 249.10 0.0914
				05:30          0         271          0 BALANCING                          0   271 249.10 0.0675
				05:45          0        1496          0 BALANCING                          0  1496 249.10 0.3727
				06:00          0        1237          0 BALANCING                          0  1237 257.30 0.3183
				06:15          0        1439          0 BALANCING                          0  1439 257.30 0.3703
				06:30          3        1445          0 BALANCING                          0  1442 257.30 0.3710
				06:45          4        1308          0 BALANCING                          0  1304 257.30 0.3355
				07:00         24        1590          0 BALANCING                          0  1566 286.60 0.4488
				07:15         82        1184          0 BALANCING                          0  1102 286.60 0.3158
				07:30        111         924          0 BALANCING                          0   813 286.60 0.2330
				07:45        140         652          0 BALANCING                          0   512 286.60 0.1467
				08:00        158         337          0 BALANCING                          0   179 293.20 0.0525
				08:15        198         670          0 BALANCING                          0   472 293.20 0.1384
				08:30        293         734          0 BALANCING                          0   441 293.20 0.1293
				08:45        471         577          0 BALANCING                          0   106 293.20 0.0311
				09:00        499         771          0 BALANCING                          0   272 270.60 0.0736
				09:15        656         512          0 BALANCING                       -144     0 270.60 0.0000
				09:30       1172         198        144 BALANCING                       -974     0 270.60 0.0000
				09:45       1486        1095       1118 BALANCING                       -391     0 270.60 0.0000
				10:00       1150         355       1509 BALANCING                       -795     0 256.90 0.0000
				10:15        942         275       2304 BALANCING                       -667     0 256.90 0.0000
				10:30       1103         816       2971 BALANCING                       -287     0 256.90 0.0000
				10:45       1125         753       3258 BALANCING                       -372     0 256.90 0.0000
				11:00       1046         196       3630 BALANCING                       -850     0 249.30 0.0000
				11:15        956        1146       4480 DELAY_DISCHARGE                    0   190 249.30 0.0474
				11:30        722         392       4480 BALANCING                       -330     0 249.30 0.0000
				11:45        737         262       4810 BALANCING                       -475     0 249.30 0.0000
				12:00        687         803       5285 DELAY_DISCHARGE                    0   116 244.90 0.0284
				12:15        653         210       5285 BALANCING                       -443     0 244.90 0.0000
				12:30        659         885       5728 DELAY_DISCHARGE                    0   226 244.90 0.0553
				12:45        641         369       5728 BALANCING                       -272     0 244.90 0.0000
				13:00        656         400       6000 BALANCING                       -256     0 241.10 0.0000
				13:15        589         553       6256 BALANCING                        -36     0 241.10 0.0000
				13:30        492        1014       6292 DELAY_DISCHARGE                    0   522 241.10 0.1259
				13:45        487         403       6292 BALANCING                        -84     0 241.10 0.0000
				14:00        506         621       6376 DELAY_DISCHARGE                    0   115 242.10 0.0278
				14:15        579        1030       6376 DELAY_DISCHARGE                    0   451 242.10 0.1092
				14:30        599        1081       6376 DELAY_DISCHARGE                    0   482 242.10 0.1167
				14:45        645         987       6376 DELAY_DISCHARGE                    0   342 242.10 0.0828
				15:00        641        1137       6376 DELAY_DISCHARGE                    0   496 252.70 0.1253
				15:15        478         984       6376 DELAY_DISCHARGE                    0   506 252.70 0.1279
				15:30        390        1185       6376 DELAY_DISCHARGE                    0   795 252.70 0.2009
				15:45        355         997       6376 DELAY_DISCHARGE                    0   642 252.70 0.1622
				16:00        360        1135       6376 DELAY_DISCHARGE                    0   775 258.10 0.2000
				16:15        368         982       6376 DELAY_DISCHARGE                    0   614 258.10 0.1585
				16:30        316        1104       6376 DELAY_DISCHARGE                    0   788 258.10 0.2034
				16:45        218         988       6376 DELAY_DISCHARGE                    0   770 258.10 0.1987
				17:00        143        1124       6376 DELAY_DISCHARGE                    0   981 279.60 0.2743
				17:15         62         991       6376 BALANCING                        929     0 279.60 0.0000
				17:30          7        1106       5447 BALANCING                       1099     0 279.60 0.0000
				17:45          1        1374       4348 DELAY_DISCHARGE                    0  1373 279.60 0.3839
				18:00          1        1257       4348 BALANCING                       1256     0 279.80 0.0000
				18:15          0        1079       3092 BALANCING                       1079     0 279.80 0.0000
				18:30          0        1088       2013 BALANCING                       1088     0 279.80 0.0000
				18:45          0        1112        925 BALANCING                        925   187 279.80 0.0523
				19:00          0        1059          0 BALANCING                          0  1059 262.00 0.2775
				19:15          0        1303          0 BALANCING                          0  1303 262.00 0.3414
				19:30          0        1303          0 BALANCING                          0  1303 262.00 0.3414
				19:45          0         336          0 BALANCING                          0   336 262.00 0.0880
				20:00          0        1812          0 BALANCING                          0  1812 246.70 0.4470
				20:15          0         827          0 BALANCING                          0   827 246.70 0.2040
				20:30          0         749          0 BALANCING                          0   749 246.70 0.1848
				20:45          0         528          0 BALANCING                          0   528 246.70 0.1303
				21:00          0         196          0 BALANCING                          0   196 243.40 0.0477
				21:15          0         185          0 BALANCING                          0   185 243.40 0.0450
				21:30          0         535          0 BALANCING                          0   535 243.40 0.1302
				21:45          0         232          0 BALANCING                          0   232 243.40 0.0565
				22:00          0         165          0 BALANCING                          0   165 245.10 0.0404
				22:15          0         515          0 BALANCING                          0   515 245.10 0.1262
				22:30          0         120          0 BALANCING                          0   120 245.10 0.0294
				22:45          0         199          0 BALANCING                          0   199 245.10 0.0488
				23:00          0         162          0 BALANCING                          0   162 234.50 0.0380
				23:15          0         348          0 BALANCING                          0   348 234.50 0.0816
				23:30          0         739          0 BALANCING                          0   739 234.50 0.1733
				23:45          0         513          0 BALANCING                          0   513 234.50 0.1203
				""";
		var p = IntegrationTests.parseParams(log);
		var schedule = getBestSchedule(p, 30);
		logSchedule(p, schedule);

		assertEquals(282, p.essChargeInChargeGrid());
		assertEquals(1.400715212E7, calculateCost(p, schedule), 0.001);
	}

	/**
	 * Delta between low and high price is approx. factor 1.16.
	 */
	@Ignore
	@Test
	public void test2() {
		var log = """
				Params [numberOfPeriods=51, time=2024-02-28T11:15+01:00[Europe/Berlin], essTotalEnergy=22000, essMinSocEnergy=0, essMaxSocEnergy=19800, essInitialEnergy=0, essMaxEnergyPerPeriod=2565, essChargeInChargeGrid=361, maxBuyFromGrid=5750, states=[BALANCING, DELAY_DISCHARGE, CHARGE_GRID]]
				Time  Production Consumption EssInitial State             EssChargeDischarge  Grid Price  Cost
				11:15        518         865          0 BALANCING                          0   347 249.30 0.0865
				11:45        234         306          0 BALANCING                          0    72 249.30 0.0179
				12:00        257         951          0 BALANCING                          0   694 244.90 0.1700
				12:15        281         611          0 BALANCING                          0   330 244.90 0.0808
				12:30        321         409          0 BALANCING                          0    88 244.90 0.0216
				12:45        349        1399          0 BALANCING                          0  1050 244.90 0.2571
				13:00        395         239          0 CHARGE_GRID                     -517   361 241.10 0.1010
				13:15        388         418        517 CHARGE_GRID                     -361   391 241.10 0.1082
				13:30        352        1308        878 CHARGE_GRID                     -361  1317 241.10 0.3315
				13:45        376         534       1239 CHARGE_GRID                     -361   519 241.10 0.1391
				14:00        419         600       1600 DELAY_DISCHARGE                    0   181 242.10 0.0438
				14:15        513         818       1600 DELAY_DISCHARGE                    0   305 242.10 0.0738
				14:30        565        1097       1600 DELAY_DISCHARGE                    0   532 242.10 0.1288
				14:45        645        1050       1600 DELAY_DISCHARGE                    0   405 242.10 0.0981
				15:00        641        1074       1600 DELAY_DISCHARGE                    0   433 252.70 0.1094
				15:15        478        1048       1600 DELAY_DISCHARGE                    0   570 252.70 0.1440
				15:30        390        1055       1600 DELAY_DISCHARGE                    0   665 252.70 0.1680
				15:45        355        1117       1600 DELAY_DISCHARGE                    0   762 252.70 0.1926
				16:00        360        1016       1600 DELAY_DISCHARGE                    0   656 258.10 0.1693
				16:15        368        1116       1600 DELAY_DISCHARGE                    0   748 258.10 0.1931
				16:30        316         988       1600 DELAY_DISCHARGE                    0   672 258.10 0.1734
				16:45        218        1096       1600 DELAY_DISCHARGE                    0   878 258.10 0.2266
				17:00        143        1000       1600 DELAY_DISCHARGE                    0   857 279.60 0.2396
				17:15         62        1118       1600 DELAY_DISCHARGE                    0  1056 279.60 0.2953
				17:30          7         991       1600 DELAY_DISCHARGE                    0   984 279.60 0.2751
				17:45          1        1151       1600 DELAY_DISCHARGE                    0  1150 279.60 0.3215
				18:00          1        1414       1600 BALANCING                       1413     0 279.80 0.0000
				18:15          0        1227        187 DELAY_DISCHARGE                    0  1227 279.80 0.3433
				18:30          0        1026        187 DELAY_DISCHARGE                    0  1026 279.80 0.2871
				18:45          0        1099        187 BALANCING                        187   912 279.80 0.2552
				19:00          0        1120          0 BALANCING                          0  1120 262.00 0.2934
				19:15          0        1120          0 BALANCING                          0  1120 262.00 0.2934
				19:30          0        1327          0 BALANCING                          0  1327 262.00 0.3477
				19:45          0        1286          0 BALANCING                          0  1286 262.00 0.3369
				20:00          0        1302          0 BALANCING                          0  1302 246.70 0.3212
				20:15          0        1297          0 BALANCING                          0  1297 246.70 0.3200
				20:30          0        1298          0 BALANCING                          0  1298 246.70 0.3202
				20:45          0        1295          0 BALANCING                          0  1295 246.70 0.3195
				21:00          0        1297          0 BALANCING                          0  1297 243.40 0.3157
				21:15          0        1298          0 BALANCING                          0  1298 243.40 0.3159
				21:30          0        1290          0 BALANCING                          0  1290 243.40 0.3140
				21:45          0        1378          0 BALANCING                          0  1378 243.40 0.3354
				22:00          0        1494          0 BALANCING                          0  1494 245.10 0.3662
				22:15          0        1298          0 BALANCING                          0  1298 245.10 0.3181
				22:30          0        1137          0 BALANCING                          0  1137 245.10 0.2787
				22:45          0         965          0 BALANCING                          0   965 245.10 0.2365
				23:00          0         958          0 BALANCING                          0   958 234.50 0.2247
				23:15          0         998          0 BALANCING                          0   998 234.50 0.2340
				23:30          0        1384          0 BALANCING                          0  1384 234.50 0.3245
				23:45          0         956          0 BALANCING                          0   956 234.50 0.2242
				""";
		var p = IntegrationTests.parseParams(log);
		var schedule = getBestSchedule(p, 30);
		logSchedule(p, schedule);

		assertTrue(stream(schedule).noneMatch(s -> s == StateMachine.CHARGE_GRID));
		assertEquals(323, p.essChargeInChargeGrid());
		assertEquals(1.1092183E7, calculateCost(p, schedule), 0.001);
	}

	public static final Pattern PARAMS_PATTERN = Pattern.compile("^" //
			+ ".*essTotalEnergy=(?<essTotalEnergy>\\d+)" //
			+ ".*essMinSocEnergy=(?<essMinSocEnergy>\\d+)" //
			+ ".*essMaxSocEnergy=(?<essMaxSocEnergy>\\d+)" //
			+ ".*essInitialEnergy=(?<essInitialEnergy>\\d+)" //
			+ ".*essMaxEnergyPerPeriod=(?<essMaxEnergyPerPeriod>\\d+)" //
			+ ".*maxBuyFromGrid=(?<maxBuyFromGrid>\\d+)" //
			+ ".*states=\\[(?<states>[A-Z_, ]+)\\]" //
			+ ".*$");
	public static final Pattern PERIOD_PATTERN = Pattern.compile("^.*(?<log>\\d{2}:\\d{2}\s+.*$)");

	protected static Params parseParams(String log) throws IllegalArgumentException {
		var periods = IntegrationTests.parsePeriods(log);

		var paramsMatcher = log.lines() //
				.findFirst() //
				.map(PARAMS_PATTERN::matcher) //
				.get();
		paramsMatcher.find();

		final var essTotalEnergy = parseInt(paramsMatcher.group("essTotalEnergy"));
		final var essMinSocEnergy = parseInt(paramsMatcher.group("essMinSocEnergy"));
		final var essMaxSocEnergy = parseInt(paramsMatcher.group("essMaxSocEnergy"));
		final var essInitialEnergy = parseInt(paramsMatcher.group("essInitialEnergy"));
		final var essMaxEnergyPerPeriod = parseInt(paramsMatcher.group("essMaxEnergyPerPeriod"));
		final var maxBuyFromGrid = parseInt(paramsMatcher.group("maxBuyFromGrid"));
		final var states = Stream.of(paramsMatcher.group("states").split(", ")) //
				.map(StateMachine::valueOf) //
				.toArray(StateMachine[]::new);

		return Params.create() //
				.time(periods.get(0).time()) //
				.essTotalEnergy(essTotalEnergy) //
				.essMinSocEnergy(essMinSocEnergy) //
				.essMaxSocEnergy(essMaxSocEnergy) //
				.essInitialEnergy(essInitialEnergy) //
				.essMaxEnergyPerPeriod(essMaxEnergyPerPeriod) //
				.maxBuyFromGrid(maxBuyFromGrid) //
				.productions(periods.stream().mapToInt(Period::production).toArray()) //
				.consumptions(periods.stream().mapToInt(Period::consumption).toArray()) //
				.prices(periods.stream().mapToDouble(Period::price).toArray()) //
				.states(states) //
				.existingSchedule() //
				.build();
	}

	private static List<Period> parsePeriods(String log) throws IllegalArgumentException {
		var result = log.lines() //
				.skip(1) //
				.map(l -> {
					if (l.contains(" Time ")) { // remove header
						return null;
					}
					var matcher = PERIOD_PATTERN.matcher(l);
					if (!matcher.find()) {
						return null;
					}
					return matcher.group("log"); //
				}) //
				.filter(Objects::nonNull) //
				.map(Period::fromLog) //
				.toList();
		if (result.isEmpty()) {
			throw new IllegalArgumentException("No Periods");
		}
		return result;
	}
}
