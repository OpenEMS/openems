package io.openems.edge.batteryinverter.cluster;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.test.DummyManagedSymmetricBatteryInverter;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BatteryInverterClusterTest {

	private static final String CLUSTER_ID = "ess0";
	private static final String BATTERY_INVERTER1_ID = "batteryInverter0";
	private static final String BATTERY_INVERTER2_ID = "batteryInverter1";
	
	private static final ChannelAddress CLUSTER_GRID_MODE = new ChannelAddress(CLUSTER_ID, SymmetricBatteryInverter.ChannelId.GRID_MODE.id());
	private static final ChannelAddress INVERTER1_GRID_MODE = new ChannelAddress(BATTERY_INVERTER1_ID, SymmetricBatteryInverter.ChannelId.GRID_MODE.id());
	private static final ChannelAddress INVERTER2_GRID_MODE = new ChannelAddress(BATTERY_INVERTER2_ID, SymmetricBatteryInverter.ChannelId.GRID_MODE.id());
	
	private static final ChannelAddress CLUSTER_ACTIVE_POWER = new ChannelAddress(CLUSTER_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_POWER.id());
	private static final ChannelAddress INVERTER1_ACTIVE_POWER = new ChannelAddress(BATTERY_INVERTER1_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_POWER.id());
	private static final ChannelAddress INVERTER2_ACTIVE_POWER = new ChannelAddress(BATTERY_INVERTER2_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_POWER.id());
	
	private static final ChannelAddress CLUSTER_REACTIVE_POWER = new ChannelAddress(CLUSTER_ID, SymmetricBatteryInverter.ChannelId.REACTIVE_POWER.id());
	private static final ChannelAddress INVERTER1_REACTIVE_POWER = new ChannelAddress(BATTERY_INVERTER1_ID, SymmetricBatteryInverter.ChannelId.REACTIVE_POWER.id());
	private static final ChannelAddress INVERTER2_REACTIVE_POWER = new ChannelAddress(BATTERY_INVERTER2_ID, SymmetricBatteryInverter.ChannelId.REACTIVE_POWER.id());
	
	private static final ChannelAddress CLUSTER_MAX_APPARENT_POWER = new ChannelAddress(CLUSTER_ID, SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER.id());
	private static final ChannelAddress INVERTER1_MAX_APPARENT_POWER = new ChannelAddress(BATTERY_INVERTER1_ID, SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER.id());
	private static final ChannelAddress INVERTER2_MAX_APPARENT_POWER = new ChannelAddress(BATTERY_INVERTER2_ID, SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER.id());
	
	private static final ChannelAddress CLUSTER_ACTIVE_CHARGE_ENERGY = new ChannelAddress(CLUSTER_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY.id());
	private static final ChannelAddress INVERTER1_ACTIVE_CHARGE_ENERGY = new ChannelAddress(BATTERY_INVERTER1_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY.id());
	private static final ChannelAddress INVERTER2_ACTIVE_CHARGE_ENERGY = new ChannelAddress(BATTERY_INVERTER2_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY.id());
	
	private static final ChannelAddress CLUSTER_ACTIVE_DISCHARGE_ENERGY = new ChannelAddress(CLUSTER_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY.id());
	private static final ChannelAddress INVERTER1_ACTIVE_DISCHARGE_ENERGY = new ChannelAddress(BATTERY_INVERTER1_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY.id());
	private static final ChannelAddress INVERTER2_ACTIVE_DISCHARGE_ENERGY = new ChannelAddress(BATTERY_INVERTER2_ID, SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY.id());
	
	@Test
	public void testCluster() throws Exception {
		new ComponentTest(new BatteryInverterClusterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addBatteryInverter", new DummyManagedSymmetricBatteryInverter(BATTERY_INVERTER1_ID)) //
				.addReference("addBatteryInverter", new DummyManagedSymmetricBatteryInverter(BATTERY_INVERTER2_ID)) //
				.activate(MyConfig.create() //
						.setId(CLUSTER_ID) //
						.setBatteryInverterIds(BATTERY_INVERTER1_ID, BATTERY_INVERTER2_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input(INVERTER1_GRID_MODE, GridMode.ON_GRID)
						.input(INVERTER2_GRID_MODE, GridMode.ON_GRID)
						.output(CLUSTER_GRID_MODE, GridMode.ON_GRID)
						
						.input(INVERTER1_ACTIVE_POWER, 1500) //
						.input(INVERTER2_ACTIVE_POWER, 1500) //
						.output(CLUSTER_ACTIVE_POWER, 3000) //
						
						.input(INVERTER1_REACTIVE_POWER, 1111) //
						.input(INVERTER2_REACTIVE_POWER, 450) //
						.output(CLUSTER_REACTIVE_POWER, 1561) //
						
						.input(INVERTER1_MAX_APPARENT_POWER, 92000) //
						.input(INVERTER2_MAX_APPARENT_POWER, 92000) //
						.output(CLUSTER_MAX_APPARENT_POWER, 184000) //
						
						// This test was failing when I passed integer values. It works when casting to (long) 
						.input(INVERTER1_ACTIVE_CHARGE_ENERGY, (long) 1450.0) // 
						.input(INVERTER2_ACTIVE_CHARGE_ENERGY, (long) 1550.0) //
						.output(CLUSTER_ACTIVE_CHARGE_ENERGY, (long) 3000.0) //
						
						.input(INVERTER1_ACTIVE_DISCHARGE_ENERGY, (long) 46000.0) // 
						.input(INVERTER2_ACTIVE_DISCHARGE_ENERGY, (long) 54000.0) //
						.output(CLUSTER_ACTIVE_DISCHARGE_ENERGY, (long) 100000.0) //
						
						);
	}
	
	@Test
	public void testGridMode() throws Exception {
		new ComponentTest(new BatteryInverterClusterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addBatteryInverter", new DummyManagedSymmetricBatteryInverter(BATTERY_INVERTER1_ID)) //
				.addReference("addBatteryInverter", new DummyManagedSymmetricBatteryInverter(BATTERY_INVERTER2_ID)) //
				.activate(MyConfig.create() //
						.setId(CLUSTER_ID) //
						.setBatteryInverterIds(BATTERY_INVERTER1_ID, BATTERY_INVERTER2_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input(INVERTER1_GRID_MODE, GridMode.ON_GRID) //
						.input(INVERTER2_GRID_MODE, GridMode.ON_GRID) //
						.output(CLUSTER_GRID_MODE, GridMode.ON_GRID) //
				) //
				.next(new TestCase() //
						.input(INVERTER1_GRID_MODE, GridMode.OFF_GRID) //
						.input(INVERTER2_GRID_MODE, GridMode.OFF_GRID) //
						.output(CLUSTER_GRID_MODE, GridMode.OFF_GRID) //
				) //
				.next(new TestCase() //
						.input(INVERTER1_GRID_MODE, GridMode.OFF_GRID) //
						.input(INVERTER2_GRID_MODE, GridMode.UNDEFINED) //
						.output(CLUSTER_GRID_MODE, GridMode.UNDEFINED) //
				) //
				.next(new TestCase() //
						.input(INVERTER1_GRID_MODE, GridMode.OFF_GRID) //
						.input(INVERTER2_GRID_MODE, GridMode.ON_GRID) //
						.output(CLUSTER_GRID_MODE, GridMode.UNDEFINED) //
				) //
		;
	}

}
