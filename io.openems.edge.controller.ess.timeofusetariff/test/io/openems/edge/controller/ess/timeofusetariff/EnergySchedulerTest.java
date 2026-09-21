package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyDelayCharge;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyDischargeConsumption;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.applyLimitCharge;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.buildEnergyScheduleHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

class EnergySchedulerTest {

	@Test
	void testNull() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod(0 /* BALANCING */);
		assertEquals(106 /* fallback to balancing */, t0.ef().solve().getEss());
	}

	@Test
	void testChargeConsumption() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(ControlMode.CHARGE_CONSUMPTION.modes, null, null));
		var t = EnergyScheduleTester.from(esh);

		// Initial Population: DELAY_DISCHARGE and CHARGE_GRID
		var ip = t.perEsh.get(0).initialPopulation();
		assertEquals(4, ip.size());
		assertEquals(//
				"[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(0).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(1).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(2).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(3).modeIndexes()));

		assertEquals(106, t.simulatePeriod(0 /* BALANCING */).ef().solve().getEss());
		assertEquals(0, t.simulatePeriod(1 /* DELAY_DISCHARGE */).ef().solve().getEss());
		assertEquals(-1327, t.simulatePeriod(2 /* CHARGE_GRID */).ef().solve().getEss());
	}

	@Test
	void testDelayDischarge() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(ControlMode.DELAY_DISCHARGE.modes, null, null));
		var t = EnergyScheduleTester.from(esh);

		// Initial Population: DELAY_DISCHARGE only
		var ip = t.perEsh.get(0).initialPopulation();
		assertEquals(2, ip.size());
		assertEquals(//
				"[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(0).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(1).modeIndexes()));

		assertEquals(106, t.simulatePeriod(0 /* BALANCING */).ef().solve().getEss());
		assertEquals(0, t.simulatePeriod(1 /* DELAY_DISCHARGE */).ef().solve().getEss());
	}

	@Test
	void testDischargeToGrid() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(ControlMode.DISCHARGE_TO_GRID.modes, null, null));
		var t = EnergyScheduleTester.from(esh);

		// Initial Population: DELAY_DISCHARGE, CHARGE_GRID and DISCHARGE_GRID
		var ip = t.perEsh.get(0).initialPopulation();
		assertEquals(5, ip.size());
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	@DisplayName("applyDelayCharge()")
	class ApplyDelayChargeTest {

		@Mock
		private EnergyFlow.Model model;

		@Test
		void shouldDelayCharge_whenSurplus() {
			when(this.model.getSurplus()).thenReturn(1000);
			applyDelayCharge(this.model);
			verify(this.model).setEss(0);
		}

		@Test
		void shouldDischarge_whenNoSurplus() {
			when(this.model.getSurplus()).thenReturn(-1000);
			applyDelayCharge(this.model);
			verify(this.model).setEss(1000);
		}

		@Test
		void shouldDelayCharge_whenExactlyZeroSurplus() {
			when(this.model.getSurplus()).thenReturn(0);
			applyDelayCharge(this.model);
			verify(this.model).setEss(0);
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	@DisplayName("applyLimitCharge()")
	class ApplyLimitChargeTest {

		@Mock
		private EnergyFlow.Model model;

		@Test
		void shouldLimitCharge_whenLimitLessThanSurplus() {
			when(this.model.getSurplus()).thenReturn(1000);
			applyLimitCharge(this.model, 500);
			verify(this.model).setEss(-500);
		}

		@Test
		void shouldChargeSurplus_whenLimitGreaterThanSurplus() {
			when(this.model.getSurplus()).thenReturn(600);
			applyLimitCharge(this.model, 1000);
			verify(this.model).setEss(-600);
		}

		@Test
		void shouldChargeSurplus_whenNoLimit() {
			when(this.model.getSurplus()).thenReturn(1000);
			applyLimitCharge(this.model, null);
			verify(this.model).setEss(-1000);
		}

		@Test
		void shouldDischarge_whenNoSurplus() {
			when(this.model.getSurplus()).thenReturn(-1000);
			applyLimitCharge(this.model, 500);
			verify(this.model).setEss(1000);
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	@DisplayName("applyDischargeConsumption()")
	class ApplyDischargeConsumptionTest {

		@Mock
		private EnergyFlow.Model model;

		@Test
		void shouldDischargeConsumption() {
			when(this.model.getConsumption()).thenReturn(1000);
			applyDischargeConsumption(this.model);
			verify(this.model).setEss(1000);
		}
	}
}