package io.openems.edge.solaredge.ess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.solaredge.enums.ControlMode;
import io.openems.edge.solaredge.charger.SolarEdgeChargerImpl;
import io.openems.edge.solaredge.enums.AcChargePolicy;
import io.openems.edge.solaredge.enums.CommandMode;
import io.openems.edge.solaredge.enums.MeterCommunicateStatus;
import io.openems.edge.solaredge.enums.SeControlMode;
import io.openems.edge.common.channel.BooleanReadChannel;

public class ApplyPowerHandlerTest {

	private static final int CYCLE_TIME = 1000;

	@Test
	public void testApply() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(io.openems.edge.solaredge.charger.MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());

		var power = new DummyPower(/* maxApparentPower */ 10000, new PidFilter());

		var ess = new SolarEdgeEssImpl();
		ess.addCharger(charger);
		final var componentTest = new ComponentTest(ess) //
				.addReference("power", power) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(charger)
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.REMOTE) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build()) //
				.next(new TestCase());

		final var applyPowerHandler = new ApplyPowerHandler();

		final BooleanReadChannel smartModeNotWorkingWithPidFilter = ess.channel(SolarEdgeEss.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER);
		final BooleanReadChannel noSmartMeterDetected = ess.channel(SolarEdgeEss.ChannelId.NO_SMART_METER_DETECTED);
		final IntegerWriteChannel commandTimeout = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_TIMEOUT);
		final IntegerWriteChannel chargeLimit = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_CHARGE_LIMIT);
		final IntegerWriteChannel dischargeLimit = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT);
		final EnumWriteChannel commandMode = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE);

		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 100); // pvProduction
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_CHARGE_CONTINUES_POWER, 3000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_DISCHARGE_CONTINUES_POWER, 3300);

		// Test Block discharge when SOC channel is unavailable
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ true //
		);

		assertTrue(power.isFilterEnabled());
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertFalse(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().orElse(0));
		assertEquals(0, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(0, (int) dischargeLimit.getNextWriteValue().get());
		assertEquals(CommandMode.DISCHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().get());

		// Test Block charge when SOC channel is unavailable
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ -1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ true //
		);

		assertTrue(power.isFilterEnabled());
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertFalse(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().orElse(0));
		assertEquals(0, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(0, (int) dischargeLimit.getNextWriteValue().get());
		assertEquals(CommandMode.CHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().get());

		// Make SOC channel available
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 50);

		// Test apply with RemoteMode
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ true //
		);

		assertTrue(power.isFilterEnabled());
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertFalse(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().orElse(0));
		assertEquals(0, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(950, (int) dischargeLimit.getNextWriteValue().get()); // 1000 setActivePower - 300 pvProduction + 50 dischargeEfficiencyCompensation
		assertEquals(CommandMode.DISCHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().get());

		// Test fallback to AutoMode if gridActivePower or essActivePower are not available
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, null), //
				/* essActivePower */ new Value<Integer>(null, null), //
				/* isFilterEnabled */ true //
		);

		assertTrue(power.isFilterEnabled());
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertFalse(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().orElse(0));
		assertEquals(3000, (int) chargeLimit.getNextWriteValue().orElse(0)); // BATTERY1_MAX_CHARGE_CONTINUES_POWER
		assertEquals(3300, (int) dischargeLimit.getNextWriteValue().get()); // BATTERY1_MAX_DISCHARGE_CONTINUES_POWER
		assertEquals(CommandMode.AUTO.getValue(), (int) commandMode.getNextWriteValue().get());

		// Test Block discharge when SOC is at 10%
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 10);

		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ true //
		);

		assertTrue(power.isFilterEnabled());
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertFalse(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().orElse(0));
		assertEquals(0, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(0, (int) dischargeLimit.getNextWriteValue().get());
		assertEquals(CommandMode.DISCHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().get());

		// Test Block charge when SOC channel is at 100%
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 100);

		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ -1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ true //
		);

		assertTrue(power.isFilterEnabled());
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertFalse(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().orElse(0));
		assertEquals(0, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(0, (int) dischargeLimit.getNextWriteValue().get());
		assertEquals(CommandMode.CHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().get());

		componentTest.deactivate();
	}

	@Test
	public void testCalculate() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(io.openems.edge.solaredge.charger.MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());

		var ess = new SolarEdgeEssImpl();
		ess.addCharger(charger);

		final var applyPowerHandler = new ApplyPowerHandler();

		final IntegerWriteChannel chargeLimit = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_CHARGE_LIMIT);
		final IntegerWriteChannel dischargeLimit = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT);
		final EnumWriteChannel commandMode = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE);

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_CHARGE_CONTINUES_POWER, 3000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_DISCHARGE_CONTINUES_POWER, 3300);
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 50);
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 300); // pvProduction

		// INTERNAL
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertEquals(CommandMode.AUTO.getValue(), (int) commandMode.getNextWriteValue().orElse(0));
		assertEquals(3000, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(3300, (int) dischargeLimit.getNextWriteValue().get());

		// REMOTE positive (1000 activePowerSetPoint - 300 pvProduction + 50 dischargeEfficiencyCompensation)
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertEquals(CommandMode.DISCHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().orElse(0));
		assertEquals(0, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(750, (int) dischargeLimit.getNextWriteValue().get());

		// REMOTE negative (1000 activePowerSetPoint + 300 pvProduction)
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ -1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertEquals(CommandMode.CHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().orElse(0));
		assertEquals(1300, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(0, (int) dischargeLimit.getNextWriteValue().get());

		// SMART - DISCHARGE_BAT (1000 activePowerSetPoint - 300 pvProduction + 50 dischargeEfficiencyCompensation)
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertEquals(CommandMode.DISCHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().orElse(0));
		assertEquals(0, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(750, (int) dischargeLimit.getNextWriteValue().get());

		// SMART - CHARGE_BAT
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 0, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertEquals(CommandMode.CHARGE_BAT.getValue(), (int) commandMode.getNextWriteValue().orElse(0));
		assertEquals(300, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(0, (int) dischargeLimit.getNextWriteValue().get());

		// SMART - AUTO (Balancing)
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 670, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertEquals(CommandMode.AUTO.getValue(), (int) commandMode.getNextWriteValue().orElse(0));
		assertEquals(3000, (int) chargeLimit.getNextWriteValue().orElse(0));
		assertEquals(3300, (int) dischargeLimit.getNextWriteValue().get());
	}

	@Test
	public void testCheckControlModeWithActivePid() throws OpenemsNamedException {
		var ess = new SolarEdgeEssImpl();
		var applyPowerHandler = new ApplyPowerHandler();

		final BooleanReadChannel smartModeNotWorkingWithPidFilter = ess.channel(SolarEdgeEss.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER);

		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());

		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ true //
		);
		assertFalse(smartModeNotWorkingWithPidFilter.getNextValue().get());

		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ true //
		);
		assertTrue(smartModeNotWorkingWithPidFilter.getNextValue().get());
	}

	@Test
	public void testCheckControlModeRequiresAcCharge() throws Exception {
		var ess = new SolarEdgeEssImpl();
		var applyPowerHandler = new ApplyPowerHandler();

		final BooleanReadChannel acChargeNotEnabledWarning = ess.channel(SolarEdgeEss.ChannelId.AC_CHARGE_NOT_ENABLED);

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.UNDEFINED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(acChargeNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.ALWAYS_ALLOWED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(acChargeNotEnabledWarning.getNextValue().get());
		
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.DISABLED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(acChargeNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.DISABLED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertEquals(true, acChargeNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.DISABLED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(acChargeNotEnabledWarning.getNextValue().get());
		
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.FIXED_ENERGY_LIMIT);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(acChargeNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.FIXED_ENERGY_LIMIT);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(acChargeNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.FIXED_ENERGY_LIMIT);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(acChargeNotEnabledWarning.getNextValue().get());
		
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.PERCENT_OF_PRODUCTION);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(acChargeNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.PERCENT_OF_PRODUCTION);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(acChargeNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, AcChargePolicy.PERCENT_OF_PRODUCTION);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(acChargeNotEnabledWarning.getNextValue().get());
	}

	@Test
	public void testCheckControlModeRequiresRemoteControl() throws Exception {
		var ess = new SolarEdgeEssImpl();
		var applyPowerHandler = new ApplyPowerHandler();

		final BooleanReadChannel remoteControlNotEnabledWarning = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_NOT_ENABLED);

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.UNDEFINED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.REMOTE_CONTROL);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.DISABLED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.DISABLED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.DISABLED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.MAX_SELF_CONSUMPTION);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.MAX_SELF_CONSUMPTION);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.MAX_SELF_CONSUMPTION);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.TIME_OF_USE);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.TIME_OF_USE);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.TIME_OF_USE);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.BACKUP_ONLY);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.BACKUP_ONLY);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, SeControlMode.BACKUP_ONLY);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(remoteControlNotEnabledWarning.getNextValue().get());
	}

	@Test
	public void testCheckControlModeRequiresSmartMeter() throws Exception {
		var ess = new SolarEdgeEssImpl();
		var applyPowerHandler = new ApplyPowerHandler();

		final BooleanReadChannel noSmartMeterDetected = ess.channel(SolarEdgeEss.ChannelId.NO_SMART_METER_DETECTED);

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.UNDEFINED);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(noSmartMeterDetected.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(noSmartMeterDetected.getNextValue().get());
		
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.NO_METER);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.REMOTE, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertFalse(noSmartMeterDetected.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.NO_METER);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.INTERNAL, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(noSmartMeterDetected.getNextValue().get());

		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.NO_METER);
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 1000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<Integer>(null, 420), //
				/* isFilterEnabled */ false //
		);
		assertTrue(noSmartMeterDetected.getNextValue().get());
	}

	@Test
	public void testApplyWithPowerSetPoints() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(io.openems.edge.solaredge.charger.MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());

		var power = new DummyPower(/* maxApparentPower */ 10000, new PidFilter());

		var ess = new SolarEdgeEssImpl();
		ess.addCharger(charger);
		final var componentTest = new ComponentTest(ess) //
				.addReference("power", power) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(charger)
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.REMOTE) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build()) //
				.next(new TestCase());

		final var applyPowerHandler = new ApplyPowerHandler();

		final BooleanReadChannel smartModeNotWorkingWithPidFilter = ess.channel(SolarEdgeEss.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER);
		final BooleanReadChannel noSmartMeterDetected = ess.channel(SolarEdgeEss.ChannelId.NO_SMART_METER_DETECTED);
		final IntegerWriteChannel commandTimeout = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_TIMEOUT);
		final IntegerWriteChannel chargeLimit = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_CHARGE_LIMIT);
		final IntegerWriteChannel dischargeLimit = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT);
		final EnumWriteChannel commandMode = ess.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE);

		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 300); // pvProduction
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.NO_METER);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_CHARGE_CONTINUES_POWER, 3000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_DISCHARGE_CONTINUES_POWER, 3300);
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 50);

		// --- AC set-point of 5kW (export) ---
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 5000, // Will be limited to 3300 due to MaxDischargeContinuesPower
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<>(null, 420), //
				/* isFilterEnabled */ true
		);

		assertTrue(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertTrue(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().get());
		assertEquals(0, (int) chargeLimit.getNextWriteValue().get());
		assertEquals(3300, (int) dischargeLimit.getNextWriteValue().get());
		assertEquals(CommandMode.DISCHARGE_BAT.getValue(), commandMode.getNextWriteValue().get().intValue());

		// --- AC set-point of 3kW (export) ---
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ 3000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<>(null, 420), //
				/* isFilterEnabled */ true
		);

		assertTrue(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertTrue(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().get());
		assertEquals(0, (int) chargeLimit.getNextWriteValue().get());
		assertEquals(2850, (int) dischargeLimit.getNextWriteValue().get()); // 3000 setActivePower - 300 pvProduction + 150 dischargeEfficiencyCompensation
		assertEquals(CommandMode.DISCHARGE_BAT.getValue(), commandMode.getNextWriteValue().get().intValue());

		// --- AC set-point of -3kW (import) ---
		applyPowerHandler.apply(//
				ess,
				/* setActivePower */ -3000, //
				ControlMode.SMART, //
				/* gridActivePower */ new Value<Integer>(null, 250), //
				/* essActivePower */ new Value<>(null, 420), //
				/* isFilterEnabled */ true
		);

		assertTrue(smartModeNotWorkingWithPidFilter.getNextValue().get());
		assertTrue(noSmartMeterDetected.getNextValue().get());
		assertEquals(60, (int) commandTimeout.getNextWriteValue().get());
		assertEquals(3000, (int) chargeLimit.getNextWriteValue().get());
		assertEquals(0, (int) dischargeLimit.getNextWriteValue().get());
		assertEquals(CommandMode.CHARGE_BAT.getValue(), commandMode.getNextWriteValue().get().intValue());

		componentTest.deactivate();
	}
}