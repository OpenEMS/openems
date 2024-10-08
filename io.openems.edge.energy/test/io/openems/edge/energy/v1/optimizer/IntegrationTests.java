package io.openems.edge.energy.v1.optimizer;

import static io.openems.common.utils.DateUtils.parseZonedDateTimeOrError;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForUnitTest;
import static io.openems.edge.energy.v1.optimizer.Params.PARAMS_PATTERN;
import static io.openems.edge.energy.v1.optimizer.Simulator.calculateCost;
import static io.openems.edge.energy.v1.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.energy.v1.optimizer.SimulatorTest.logSchedule;
import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.v1.optimizer.ScheduleDatas.ScheduleData;

public class IntegrationTests {

	@Before
	public void before() {
		initializeRandomRegistryForUnitTest();
	}

	/**
	 * Two price peaks; should CHARGE_GRID during first peak.
	 * 
	 * @throws Exception on error
	 */
	@Ignore
	@Test
	// TODO to be updated
	public void test1() throws Exception {
		var log = """
				""";
		var p = IntegrationTests.parseParams(log);
		var schedule = getBestSchedule(p, 30);
		logSchedule(p, schedule);

		assertEquals(282, p.optimizePeriods().get(0).essChargeInChargeGrid());
		assertEquals(1.400715212E7, calculateCost(p, schedule), 0.001);
	}

	public static final Pattern PERIOD_PATTERN = Pattern.compile("^.*(?<log>\\d{2}:\\d{2}\s+.*$)");

	protected static Params parseParams(String log) throws IllegalArgumentException, OpenemsException {
		var paramsMatcher = log.lines() //
				.findFirst() //
				.map(PARAMS_PATTERN::matcher) //
				.get();
		paramsMatcher.find();

		final var time = parseZonedDateTimeOrError(paramsMatcher.group("time"));
		final var essTotalEnergy = parseInt(paramsMatcher.group("essTotalEnergy"));
		final var essMinSocEnergy = parseInt(paramsMatcher.group("essMinSocEnergy"));
		final var essMaxSocEnergy = parseInt(paramsMatcher.group("essMaxSocEnergy"));
		final var essInitialEnergy = parseInt(paramsMatcher.group("essInitialEnergy"));
		final var states = Stream.of(paramsMatcher.group("states").split(", ")) //
				.map(StateMachine::valueOf) //
				.toArray(StateMachine[]::new);

		var sds = ScheduleDatas.fromLogString(essTotalEnergy, log);
		if (sds.isEmpty()) {
			throw new IllegalArgumentException("No Periods");
		}
		var sd = sds.stream().findFirst().get();

		return Params.create() //
				.setTime(time) //
				.setEssTotalEnergy(essTotalEnergy) //
				.setEssMinSocEnergy(essMinSocEnergy) //
				.setEssMaxSocEnergy(essMaxSocEnergy) //
				.setEssInitialEnergy(essInitialEnergy) //
				.setEssMaxChargeEnergy(sd.essMaxChargeEnergy()) //
				.setEssMaxDischargeEnergy(sd.essMaxDischargeEnergy()) //
				.seMaxBuyFromGrid(sd.maxBuyFromGrid()) //
				.setProductions(sds.stream().mapToInt(ScheduleData::production).toArray()) //
				.setConsumptions(sds.stream().mapToInt(ScheduleData::consumption).toArray()) //
				.setPrices(sds.stream().mapToDouble(ScheduleData::price).toArray()) //
				.setStates(states) //
				.setExistingSchedule(ImmutableSortedMap.of()) //
				.build();
	}
}
