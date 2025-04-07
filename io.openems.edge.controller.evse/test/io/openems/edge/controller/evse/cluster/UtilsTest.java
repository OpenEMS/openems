package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.Maps.newTreeMap;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.controller.evse.cluster.Utils.calculate;
import static io.openems.edge.controller.evse.cluster.Utils.distributePower;
import static io.openems.edge.evse.api.SingleThreePhase.SINGLE_PHASE;
import static io.openems.edge.evse.api.SingleThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.FORCE;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.MINIMUM;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.SURPLUS;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.ZERO;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.test.TestUtils;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.evse.cluster.Utils.Input;
import io.openems.edge.controller.evse.cluster.Utils.Input.Hysteresis;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.test.DummyControllerEvseSingle;
import io.openems.edge.evse.api.Limit;

public class UtilsTest {

	private static final Input CTRL0 = createInput("evse0",
			new Params(true, SURPLUS, 0, new Limit(THREE_PHASE, 6000, 16000), ImmutableList.of()));
	private static final Input CTRL1 = createInput("evse1",
			new Params(true, MINIMUM, 0, new Limit(THREE_PHASE, 6000, 16000), ImmutableList.of()));
	private static final Input CTRL2 = createInput("evse2",
			new Params(true, ZERO, 0, new Limit(THREE_PHASE, 6000, 16000), ImmutableList.of()));
	private static final Input CTRL3 = createInput("evse3",
			new Params(true, SURPLUS, 0, new Limit(SINGLE_PHASE, 6000, 32000), ImmutableList.of()));
	private static final Input CTRL4 = createInput("evse4",
			new Params(true, SURPLUS, 0, new Limit(THREE_PHASE, 6000, 16000), ImmutableList.of()));
	private static final Input CTRL5 = createInput("evse5",
			new Params(true, FORCE, 0, new Limit(THREE_PHASE, 6000, 16000), ImmutableList.of()));

	private static Input createInput(String id, Params params) {
		var ctrl = new DummyControllerEvseSingle(id) //
				.withParams(params);
		return new Input(ctrl, params, newTreeMap());
	}

	@Test
	public void testCalculateSurplusSufficient() {
		var outputs = calculate(//
				DistributionStrategy.BY_PRIORITY, //
				new DummySum() //
						.withGridActivePower(-32000) //
						.withEssDischargePower(0), //
				List.of(//
						CTRL0.ctrl(), //
						CTRL1.ctrl(), //
						CTRL2.ctrl(), //
						CTRL3.ctrl(), //
						CTRL4.ctrl(), //
						CTRL5.ctrl()), //
				Map.of(), //
				log -> doNothing());

		assertEquals(16000, outputs.get(0).current());
		assertEquals(6000, outputs.get(1).current());
		assertEquals(0, outputs.get(2).current());
		assertEquals(7130, outputs.get(3).current());
		assertEquals(6000, outputs.get(4).current());
		assertEquals(16000, outputs.get(5).current());
	}

	@Test
	public void testCalculateSurplusNotSufficient() {
		var outputs = calculate(//
				DistributionStrategy.EQUAL_POWER, //
				new DummySum() //
						.withGridActivePower(-12000) //
						.withEssDischargePower(0), //
				List.of(//
						CTRL0.ctrl(), //
						CTRL1.ctrl(), //
						CTRL2.ctrl(), //
						CTRL3.ctrl(), //
						CTRL4.ctrl()), //
				Map.of(), //
				log -> doNothing());

		assertEquals(7696, outputs.get(0).current());
		assertEquals(6000, outputs.get(1).current());
		assertEquals(0, outputs.get(2).current());
		assertEquals(11087, outputs.get(3).current());
		assertEquals(0, outputs.get(4).current());
	}

	@Test
	public void testDistributePowerOne() {
		// Zero
		assertArrayEquals(//
				new int[] { 0 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0 }, //
						0));

		// Low Three-Phase
		assertArrayEquals(//
				new int[] { 0 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0 }, //
						4139));

		// Low Single-Phase
		assertArrayEquals(//
				new int[] { 0 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0 }, //
						1379));

		// MinPower Three-Phase
		assertArrayEquals(//
				new int[] { 4140 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0 }, //
						4140));

		// MinPower Single-Phase
		assertArrayEquals(//
				new int[] { 1380 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL3 }, //
						1380));

		// MinPower+Remaining Three-Phase
		assertArrayEquals(//
				new int[] { 4141 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0 }, //
						4141));

		// MinPower+Remaining Single-Phase
		assertArrayEquals(//
				new int[] { 1381 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL3 }, //
						1381));

		// High Three-Phase
		assertArrayEquals(//
				new int[] { 11040 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0 }, //
						11041));

		// High Single-Phase
		assertArrayEquals(//
				new int[] { 7360 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL3 }, //
						7361));
	}

	@Test
	public void testDistributePowerTwo() {
		// Zero
		assertArrayEquals(//
				new int[] { 0, 0 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0, CTRL4 }, //
						0));

		// Sufficient for one
		assertArrayEquals(//
				new int[] { 5000, 0 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0, CTRL4 }, //
						5000));

		// Sufficient for two (Three-Phase)
		assertArrayEquals(//
				new int[] { 5000, 5000 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0, CTRL4 }, //
						10000));

		// Sufficient for two (Mixed)
		assertArrayEquals(//
				new int[] { 6380, 3620 }, //
				distributePower(DistributionStrategy.EQUAL_POWER, //
						new Input[] { CTRL0, CTRL3 }, //
						10000));
	}

	@Test
	public void testHysteresis() {
		// TODO full tests
		var now = Instant.now(TestUtils.createDummyClock());
		var h = new TreeMap<Instant, Integer>();
		h.put(now.minusSeconds(310), 7000);
		h.put(now.minusSeconds(300), 8000);
		h.put(now.minusSeconds(290), 9000);
		assertEquals(Hysteresis.INACTIVE, Hysteresis.from(now, h));
	}
}
