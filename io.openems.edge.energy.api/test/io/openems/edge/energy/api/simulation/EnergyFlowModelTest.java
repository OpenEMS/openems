package io.openems.edge.energy.api.simulation;

import static io.openems.edge.energy.api.simulation.EnergyFlow.Curtailment.ALLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnergyFlowModelTest {

	@Nested
	class ConstructorTest {

		@Test
		void shouldClampConsumptionToMaximumSupply_whenPredictionExceedsAvailableSupply() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1200, // unmanagedConsumption
					10_000, // essMaxCharge
					100, // essMaxDischarge
					100, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(400, model.getConsumption());
			assertEquals(0, model.getManagedConsumption());
			assertEquals(0, model.addManagedConsumption("cons1", 100));

			final var flow = model.solve();
			assertEquals(400, flow.getUnmanagedConsumption());
			assertEquals(400, flow.getConsumption());
			assertEquals(200, flow.getProduction());
			assertEquals(0, flow.getExcessProduction());
			assertEquals(100, flow.getEss());
			assertEquals(100, flow.getGrid());
		}

		@ParameterizedTest
		@CsvSource({ "400, 100, 100", "300, 100, 0" })
		void shouldPreserveConsumption_whenPredictionDoesNotExceedAvailableSupply(final int consumption,
				final int expectedEss, final int expectedGrid) {
			final var model = new EnergyFlow.Model(//
					200, // production
					consumption, // unmanagedConsumption
					10_000, // essMaxCharge
					100, // essMaxDischarge
					100, // gridMaxBuy
					10_000 // gridMaxSell
			);

			final var flow = model.solve();
			assertEquals(consumption, model.getConsumption());
			assertEquals(consumption, flow.getUnmanagedConsumption());
			assertEquals(0, model.getManagedConsumption());
			assertEquals(200, flow.getProduction());
			assertEquals(expectedEss, flow.getEss());
			assertEquals(expectedGrid, flow.getGrid());
		}

		@Test
		void shouldPreserveConsumption_whenMaximumSupplyExceedsIntegerRange() {
			final var model = new EnergyFlow.Model(//
					Integer.MAX_VALUE, // production
					Integer.MAX_VALUE, // unmanagedConsumption
					10_000, // essMaxCharge
					Integer.MAX_VALUE, // essMaxDischarge
					Integer.MAX_VALUE, // gridMaxBuy
					10_000 // gridMaxSell
			);

			final var flow = model.solve();
			assertEquals(Integer.MAX_VALUE, model.getConsumption());
			assertEquals(Integer.MAX_VALUE, flow.getUnmanagedConsumption());
			assertEquals(Integer.MAX_VALUE, flow.getProduction());
			assertEquals(0, flow.getEss());
			assertEquals(0, flow.getGrid());
		}
	}

	@Nested
	class SetEssGreaterOrEqualsTest {

		@Test
		void shouldPreservePvUse_whenRaisingLowerBound() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					500 // gridMaxSell
			);
			assertEquals(-1_000, model.setEssGreaterOrEquals(-1_000));
			assertEquals(-1_000, model.setEssGreaterOrEquals(-3_000));
			assertEquals(-300, model.setEssGreaterOrEquals(-50));
		}

		@Test
		void shouldDisableCharging_whenExportCapacityIsSufficient() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(0, model.setEssGreaterOrEquals(0));
			assertEquals(0, model.setEssGreaterOrEquals(-200));
		}

		@Test
		void shouldNotIncreaseCurtailment_whenCurtailmentIsUnavoidable() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					100, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					100 // gridMaxSell
			);
			assertEquals(-100, model.setEssGreaterOrEquals(-50));
			assertEquals(-100, model.setEssGreaterOrEquals(0));
		}

		@Test
		void shouldDisableChargingAndIncreaseCurtailment_whenCurtailmentIsAllowed() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					100, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					100 // gridMaxSell
			);
			assertEquals(0, model.setEssGreaterOrEquals(0, ALLOW));

			final var flow = model.solve();
			assertEquals(0, flow.getEss());
			assertEquals(-100, flow.getGrid());
			assertEquals(300, flow.getProduction());
			assertEquals(700, flow.getExcessProduction());
		}

		@Test
		void shouldPreservePvUse_whenCurtailmentIsAllowedButExportCapacityIsSufficient() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(0, model.setEssGreaterOrEquals(0, ALLOW));

			final var flow = model.solve();
			assertEquals(0, flow.getEss());
			assertEquals(-800, flow.getGrid());
			assertEquals(1_000, flow.getProduction());
			assertEquals(0, flow.getExcessProduction());
		}

		@Test
		void shouldPreserveEssCommitment_whenChangingLowerBound() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					300, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitEss(-50);
			assertEquals(-50, model.setEssGreaterOrEquals(-600));
			assertEquals(-50, model.setEssGreaterOrEquals(-200));
			assertEquals(-50, model.setEssGreaterOrEquals(0));
			assertEquals(-50, model.setEssGreaterOrEquals(0, ALLOW));
		}

		@Test
		void shouldKeepEssIntervalFlexible_whenGridIsCommitted() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					800, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitGrid(-200);
			assertEquals(-800, model.setEssGreaterOrEquals(-1_000));
			assertEquals(-600, model.setEssGreaterOrEquals(-600));
			assertEquals(-600, model.setEssGreaterOrEquals(0));
		}
	}

	@Nested
	class SetEssLessOrEqualsTest {

		@Test
		void shouldSupplyConsumption_whenLoweringUpperBound() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					500, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(1000, model.setEssLessOrEquals(1000));
			assertEquals(1000, model.setEssLessOrEquals(3000));
			assertEquals(300, model.setEssLessOrEquals(50));
		}

		@Test
		void shouldDisableDischarging_whenImportCapacityIsSufficient() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(0, model.setEssLessOrEquals(0));
			assertEquals(0, model.setEssLessOrEquals(200));
		}

		@Test
		void shouldPreserveEssCommitment_whenChangingUpperBound() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					300, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitEss(50);
			assertEquals(50, model.setEssLessOrEquals(600));
			assertEquals(50, model.setEssLessOrEquals(200));
			assertEquals(50, model.setEssLessOrEquals(0));
		}

		@Test
		void shouldKeepEssIntervalFlexible_whenGridIsCommitted() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					800, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitGrid(200);
			assertEquals(800, model.setEssLessOrEquals(1_000));
			assertEquals(600, model.setEssLessOrEquals(600));
			assertEquals(600, model.setEssLessOrEquals(0));
		}
	}

	@Nested
	class SetGridGreaterOrEqualsTest {

		@Test
		void shouldPreservePvUse_whenRaisingLowerBound() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					500, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(-1000, model.setGridGreaterOrEquals(-1000));
			assertEquals(-1000, model.setGridGreaterOrEquals(-3000));
			assertEquals(-300, model.setGridGreaterOrEquals(-50));
		}

		@Test
		void shouldDisableExport_whenChargingCapacityIsSufficient() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(0, model.setGridGreaterOrEquals(0));
			assertEquals(0, model.setGridGreaterOrEquals(-200));
		}

		@Test
		void shouldNotIncreaseCurtailment_whenCurtailmentIsUnavoidable() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					100, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					100 // gridMaxSell
			);
			assertEquals(-100, model.setGridGreaterOrEquals(-50));
			assertEquals(-100, model.setGridGreaterOrEquals(0));
		}

		@Test
		void shouldDisableExportAndIncreaseCurtailment_whenCurtailmentIsAllowed() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					100, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					100 // gridMaxSell
			);
			assertEquals(0, model.setGridGreaterOrEquals(0, ALLOW));

			final var flow = model.solve();
			assertEquals(-100, flow.getEss());
			assertEquals(0, flow.getGrid());
			assertEquals(300, flow.getProduction());
			assertEquals(700, flow.getExcessProduction());
		}

		@Test
		void shouldPreservePvUse_whenCurtailmentIsAllowedButChargingCapacityIsSufficient() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(0, model.setGridGreaterOrEquals(0, ALLOW));

			final var flow = model.solve();
			assertEquals(-800, flow.getEss());
			assertEquals(0, flow.getGrid());
			assertEquals(1_000, flow.getProduction());
			assertEquals(0, flow.getExcessProduction());
		}

		@Test
		void shouldPreserveGridCommitment_whenChangingLowerBound() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					1_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					300 // gridMaxSell
			);
			model.commitGrid(-50);
			assertEquals(-50, model.setGridGreaterOrEquals(-600));
			assertEquals(-50, model.setGridGreaterOrEquals(-200));
			assertEquals(-50, model.setGridGreaterOrEquals(0));
			assertEquals(-50, model.setGridGreaterOrEquals(0, ALLOW));
		}

		@Test
		void shouldKeepGridIntervalFlexible_whenEssIsCommitted() {
			final var model = new EnergyFlow.Model(//
					1_000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					800 // gridMaxSell
			);
			model.commitEss(-200);
			assertEquals(-800, model.setGridGreaterOrEquals(-1_000));
			assertEquals(-600, model.setGridGreaterOrEquals(-600));
			assertEquals(-600, model.setGridGreaterOrEquals(0));
		}
	}

	@Nested
	class SetGridLessOrEqualsTest {

		@Test
		void shouldSupplyConsumption_whenLoweringUpperBound() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					500, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(1000, model.setGridLessOrEquals(1000));
			assertEquals(1000, model.setGridLessOrEquals(3000));
			assertEquals(300, model.setGridLessOrEquals(50));
		}

		@Test
		void shouldDisableImport_whenDischargeCapacityIsSufficient() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(0, model.setGridLessOrEquals(0));
			assertEquals(0, model.setGridLessOrEquals(200));
		}

		@Test
		void shouldPreserveGridCommitment_whenChangingUpperBound() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					300, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitGrid(50);
			assertEquals(50, model.setGridLessOrEquals(600));
			assertEquals(50, model.setGridLessOrEquals(200));
			assertEquals(50, model.setGridLessOrEquals(0));
		}

		@Test
		void shouldKeepGridIntervalFlexible_whenEssIsCommitted() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1_000, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					800, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitEss(200);
			assertEquals(800, model.setGridLessOrEquals(1_000));
			assertEquals(600, model.setGridLessOrEquals(600));
			assertEquals(600, model.setGridLessOrEquals(0));
		}
	}

	@Nested
	class AddManagedConsumptionTest {

		@Test
		void shouldLimitConsumptionToAvailableSupply_whenEssAndGridAreUncommitted() {
			final var model = new EnergyFlow.Model(//
					1000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					2000, // essMaxDischarge
					1200, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(2000, model.addManagedConsumption("cons1", 2000));
			assertEquals(1800, model.addManagedConsumption("cons2", 1800));
			assertEquals(200, model.addManagedConsumption("cons3", 1000));
			assertEquals(4200, model.getConsumption());
			assertEquals(4000, model.getManagedConsumption());
			assertEquals(-3200, model.getSurplus());
		}

		@Test
		void shouldLimitConsumptionToAvailableSupply_whenEssIsCommitted() {
			final var model = new EnergyFlow.Model(//
					1000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					2200, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitEss(1000);
			assertEquals(2000, model.addManagedConsumption("cons1", 2000));
			assertEquals(1800, model.addManagedConsumption("cons2", 1800));
			assertEquals(200, model.addManagedConsumption("cons3", 1000));
			assertEquals(2200, model.getGrid());
			assertEquals(4200, model.getConsumption());
			assertEquals(4000, model.getManagedConsumption());
			assertEquals(-3200, model.getSurplus());
		}

		@Test
		void shouldUseOtherwiseCurtailedPv_whenEssIsCommitted() {
			final var model = new EnergyFlow.Model(//
					1000, // production
					200, // unmanagedConsumption
					100, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					100 // gridMaxSell
			);
			model.commitEss(100);
			assertEquals(200, model.addManagedConsumption("cons1", 200));
			assertEquals(-100, model.getGrid());
			assertEquals(400, model.getConsumption());
			assertEquals(200, model.getManagedConsumption());
			assertEquals(600, model.getSurplus());
		}

		@Test
		void shouldLimitConsumptionToAvailableSupply_whenGridIsCommitted() {
			final var model = new EnergyFlow.Model(//
					1000, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					2200, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			model.commitGrid(1000);
			assertEquals(2000, model.addManagedConsumption("cons1", 2000));
			assertEquals(1800, model.addManagedConsumption("cons2", 1800));
			assertEquals(200, model.addManagedConsumption("cons3", 1000));
			assertEquals(2200, model.getEss());
			assertEquals(4200, model.getConsumption());
			assertEquals(4000, model.getManagedConsumption());
			assertEquals(-3200, model.getSurplus());
		}

		@Test
		void shouldUseOtherwiseCurtailedPv_whenGridIsCommitted() {
			final var model = new EnergyFlow.Model(//
					1000, // production
					200, // unmanagedConsumption
					100, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					100 // gridMaxSell
			);
			model.commitGrid(100);
			assertEquals(200, model.addManagedConsumption("cons1", 200));
			assertEquals(-100, model.getEss());
			assertEquals(400, model.getConsumption());
			assertEquals(200, model.getManagedConsumption());
			assertEquals(600, model.getSurplus());
		}
	}

	@Nested
	class CommitEssTest {

		@Test
		void shouldLimitChargingAndPreserveCommitment_whenTargetExceedsChargeCapacity() {
			final var model = new EnergyFlow.Model(//
					1200, // production
					200, // unmanagedConsumption
					800, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(-800, model.commitEss(-1000));
			assertEquals(-800, model.commitEss(0));
			assertEquals(-200, model.getGrid());
		}

		@Test
		void shouldDischarge_whenChargingIsRequestedButOnlyDischargingIsFeasible() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					400, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(600, model.commitEss(-1000));
			assertEquals(400, model.getGrid());
		}

		@Test
		void shouldLimitDischargingAndPreserveCommitment_whenTargetExceedsDischargeCapacity() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1200, // unmanagedConsumption
					10_000, // essMaxCharge
					800, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(800, model.commitEss(1000));
			assertEquals(800, model.commitEss(0));
			assertEquals(800, model.commitEss(0, ALLOW));
			assertEquals(200, model.getGrid());
		}

		@Test
		void shouldCharge_whenDischargingIsRequestedButOnlyChargingPreservesPvUse() {
			final var model = new EnergyFlow.Model(//
					2200, // production
					200, // unmanagedConsumption
					600, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					400 // gridMaxSell
			);
			assertEquals(-600, model.commitEss(1000));
			assertEquals(-400, model.getGrid());
		}

		@Test
		void shouldLimitDischargingToConsumptionAndExportCapacity_whenCurtailmentIsAllowed() {
			final var model = new EnergyFlow.Model(//
					2200, // production
					200, // unmanagedConsumption
					600, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					400 // gridMaxSell
			);
			assertEquals(600, model.commitEss(1000, ALLOW));

			final var flow = model.solve();
			assertEquals(600, flow.getEss());
			assertEquals(-400, flow.getGrid());
			assertEquals(0, flow.getProduction());
			assertEquals(2200, flow.getExcessProduction());
		}
	}

	@Nested
	class CommitGridTest {

		@Test
		void shouldLimitExportAndPreserveCommitment_whenTargetExceedsExportCapacity() {
			final var model = new EnergyFlow.Model(//
					1200, // production
					200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					800 // gridMaxSell
			);
			assertEquals(-800, model.commitGrid(-1000));
			assertEquals(-800, model.commitGrid(0));
			assertEquals(-200, model.getEss());
		}

		@Test
		void shouldImport_whenExportIsRequestedButOnlyImportIsFeasible() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1200, // unmanagedConsumption
					10_000, // essMaxCharge
					400, // essMaxDischarge
					10_000, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(600, model.commitGrid(-1000));
			assertEquals(400, model.getEss());
		}

		@Test
		void shouldLimitImportAndPreserveCommitment_whenTargetExceedsImportCapacity() {
			final var model = new EnergyFlow.Model(//
					200, // production
					1200, // unmanagedConsumption
					10_000, // essMaxCharge
					10_000, // essMaxDischarge
					800, // gridMaxBuy
					10_000 // gridMaxSell
			);
			assertEquals(800, model.commitGrid(1000));
			assertEquals(800, model.commitGrid(0));
			assertEquals(800, model.commitGrid(0, ALLOW));
			assertEquals(200, model.getEss());
		}

		@Test
		void shouldExport_whenImportIsRequestedButOnlyExportPreservesPvUse() {
			final var model = new EnergyFlow.Model(//
					2200, // production
					200, // unmanagedConsumption
					400, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					600 // gridMaxSell
			);
			assertEquals(-600, model.commitGrid(1000));
			assertEquals(-400, model.getEss());
		}

		@Test
		void shouldLimitImportToConsumptionAndChargingCapacity_whenCurtailmentIsAllowed() {
			final var model = new EnergyFlow.Model(//
					2200, // production
					200, // unmanagedConsumption
					400, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					600 // gridMaxSell
			);
			assertEquals(600, model.commitGrid(1000, ALLOW));

			final var flow = model.solve();
			assertEquals(-400, flow.getEss());
			assertEquals(600, flow.getGrid());
			assertEquals(0, flow.getProduction());
			assertEquals(2200, flow.getExcessProduction());
		}
	}

	@Nested
	class SolveTest {

		@Test
		void shouldCurtailExcessProduction_whenChargingAndExportCapacityAreInsufficient() {
			final var model = new EnergyFlow.Model(//
					2200, // production
					200, // unmanagedConsumption
					400, // essMaxCharge
					10_000, // essMaxDischarge
					10_000, // gridMaxBuy
					600 // gridMaxSell
			);
			model.commitEss(1000);
			final var ef = model.solve();
			assertEquals(1200, ef.getProduction());
			assertEquals(1000, ef.getExcessProduction());
		}
	}
}
