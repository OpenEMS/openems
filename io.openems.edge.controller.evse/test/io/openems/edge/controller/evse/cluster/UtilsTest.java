package io.openems.edge.controller.evse.cluster;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.controller.evse.cluster.Utils.calculate;
import static io.openems.edge.controller.evse.cluster.Utils.distributePower;
import static io.openems.edge.controller.evse.single.Types.Hysteresis.INACTIVE;
import static io.openems.edge.evse.api.SingleThreePhase.SINGLE_PHASE;
import static io.openems.edge.evse.api.SingleThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.FORCE;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.MINIMUM;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.SURPLUS;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.ZERO;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.evse.cluster.Utils.Input;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.controller.evse.test.DummyControllerEvseSingle;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;

public class UtilsTest {

	private static final Input CTRL0 = InputBuilder.create() //
			.setId("evse0") //
			.setReadyForCharging(true) //
			.setActualMode(SURPLUS) //
			.setActivePower(0) //
			.setLimit(THREE_PHASE, 6000, 16000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(false) //
			.setProfiles(ImmutableList.of()) //
			.build();
	private static final Input CTRL1 = InputBuilder.create() //
			.setId("evse1") //
			.setReadyForCharging(true) //
			.setActualMode(MINIMUM) //
			.setActivePower(0) //
			.setLimit(THREE_PHASE, 6000, 16000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(false) //
			.setProfiles(ImmutableList.of()) //
			.build();
	private static final Input CTRL2 = InputBuilder.create() //
			.setId("evse2") //
			.setReadyForCharging(true) //
			.setActualMode(ZERO) //
			.setActivePower(0) //
			.setLimit(THREE_PHASE, 6000, 16000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(false) //
			.setProfiles(ImmutableList.of()) //
			.build();
	private static final Input CTRL3 = InputBuilder.create() //
			.setId("evse3") //
			.setReadyForCharging(true) //
			.setActualMode(SURPLUS) //
			.setActivePower(0) //
			.setLimit(SINGLE_PHASE, 6000, 32000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(false) //
			.setProfiles(ImmutableList.of()) //
			.build();
	private static final Input CTRL4 = InputBuilder.create() //
			.setId("evse4") //
			.setReadyForCharging(true) //
			.setActualMode(SURPLUS) //
			.setActivePower(0) //
			.setLimit(THREE_PHASE, 6000, 16000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(false) //
			.setProfiles(ImmutableList.of()) //
			.build();
	private static final Input CTRL5 = InputBuilder.create() //
			.setId("evse5") //
			.setReadyForCharging(true) //
			.setActualMode(FORCE) //
			.setActivePower(0) //
			.setLimit(THREE_PHASE, 6000, 16000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(false) //
			.setProfiles(ImmutableList.of()) //
			.build();
	private static final Input CTRL6 = InputBuilder.create() //
			.setId("evse6") //
			.setReadyForCharging(true) //
			.setActualMode(SURPLUS) //
			.setActivePower(0) //
			.setLimit(THREE_PHASE, 6000, 16000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(true) //
			.setProfiles(ImmutableList.of()) //
			.build();
	private static final Input CTRL7 = InputBuilder.create() //
			.setId("evse7") //
			.setReadyForCharging(true) //
			.setActualMode(FORCE) //
			.setActivePower(0) //
			.setLimit(THREE_PHASE, 6000, 16000) //
			.setHysteresis(INACTIVE) //
			.setAppearsToBeFullyCharged(true) //
			.setProfiles(ImmutableList.of()) //
			.build();

	private static final class InputBuilder {
		private String id;
		private boolean isReadyForCharging;
		private Mode.Actual actualMode;
		private Integer activePower;
		private Limit limit;
		private Hysteresis hysteresis;
		private boolean appearsToBeFullyCharged;
		private ImmutableList<Profile> profiles;

		public InputBuilder setId(String id) {
			this.id = id;
			return this;
		}

		public InputBuilder setReadyForCharging(boolean isReadyForCharging) {
			this.isReadyForCharging = isReadyForCharging;
			return this;
		}

		public InputBuilder setActualMode(Mode.Actual actualMode) {
			this.actualMode = actualMode;
			return this;
		}

		public InputBuilder setActivePower(Integer activePower) {
			this.activePower = activePower;
			return this;
		}

		public InputBuilder setLimit(SingleThreePhase phase, int minCurrent, int maxCurrent) {
			this.limit = new Limit(phase, minCurrent, maxCurrent);
			return this;
		}

		public InputBuilder setHysteresis(Hysteresis hysteresis) {
			this.hysteresis = hysteresis;
			return this;
		}

		public InputBuilder setAppearsToBeFullyCharged(boolean appearsToBeFullyCharged) {
			this.appearsToBeFullyCharged = appearsToBeFullyCharged;
			return this;
		}

		public InputBuilder setProfiles(ImmutableList<Profile> profiles) {
			this.profiles = profiles;
			return this;
		}

		public Input build() {
			var params = new Params(this.isReadyForCharging, this.actualMode, this.activePower, this.limit,
					this.hysteresis, this.appearsToBeFullyCharged, this.profiles);
			var ctrl = new DummyControllerEvseSingle(this.id) //
					.withParams(params);
			return new Input(ctrl, params);
		}

		public static InputBuilder create() {
			return new InputBuilder();
		}
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
						CTRL6.ctrl(), //
						CTRL7.ctrl(), //
						CTRL1.ctrl(), //
						CTRL2.ctrl(), //
						CTRL3.ctrl(), //
						CTRL4.ctrl(), //
						CTRL5.ctrl()), //
				log -> doNothing());

		assertEquals(16000, outputs.get(0).current());
		assertEquals(6000, outputs.get(1).current());
		assertEquals(6000, outputs.get(2).current()); // SURPLUS: appears to be fully charged
		assertEquals(6000, outputs.get(3).current()); // FORCE: appears to be fully charged
		assertEquals(0, outputs.get(4).current());
		assertEquals(7130, outputs.get(5).current());
		assertEquals(6000, outputs.get(6).current());
		assertEquals(16000, outputs.get(7).current());
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
}
