package io.openems.edge.bridge.modbus.sunspec.ess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

public class AbstractSunSpecEssTest {

	private class DummySunSpecEssImpl extends AbstractSunSpecEss
			implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

		private static final int READ_FROM_MODBUS_BLOCK = 1;

		private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
				.put(DefaultSunSpecModel.S_1, Priority.LOW) //
				.put(DefaultSunSpecModel.S_101, Priority.LOW) //
				.build();

		protected void setModbus(BridgeModbus modbus) {
			super.setModbus(modbus);
		}

		public DummySunSpecEssImpl() throws OpenemsNamedException {
			super(//
					ACTIVE_MODELS, //
					OpenemsComponent.ChannelId.values(), //
					SymmetricEss.ChannelId.values(), //
					ManagedSymmetricEss.ChannelId.values() //
			);
		}

		@Activate
		private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
			super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(),
					READ_FROM_MODBUS_BLOCK);
		}

		@Override
		@Deactivate
		protected void deactivate() {
			super.deactivate();
		}

		@Override
		public Power getPower() {
			return null;
		}

		@Override
		public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
			return;
		}

		@Override
		public int getPowerPrecision() {
			return 1;
		}
	}

	@Test
	public void testDebugLog() throws Exception {
		var sut = new DummySunSpecEssImpl();
		assertNotNull(sut.debugLog());
	}

	@Test
	public void testOnSunSpecInitializationCompleted() throws Exception {
		var sut = new DummySunSpecEssImpl();
		sut.onSunSpecInitializationCompleted();
	}

	@Test
	public void testGetSunSpecChannel() throws Exception {
		var sut = new DummySunSpecEssImpl();
		new ComponentTest(sut) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
						.withRegisters(40002, 1, 66) // Block 1
						.withRegisters(40070, 101, 50) // Block 101
						.withRegisters(40122, 0xFFFF, 0)) // END_OF_MAP
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build())
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.also(t -> {
							// read DefaultSunSpecModel.S_1
							assertFalse(sut.isSunSpecInitializationCompleted());
						})) //
				.next(new TestCase() //
						.also(t -> {
							// read DefaultSunSpecModel.S_101
							assertTrue(sut.isSunSpecInitializationCompleted());
						})) //
				.next(new TestCase() //
						.also(t -> {
							// Test getSunSpecChannel() returns Channel for known point
							assertTrue(sut.<Channel<?>>getSunSpecChannel(DefaultSunSpecModel.S101.DCW).isPresent());
						})) //
				.next(new TestCase() //
						.also(t -> {
							// Test getSunSpecChannel() returns Optional.empty for unknown point
							assertTrue(sut.<Channel<?>>getSunSpecChannel(DefaultSunSpecModel.S103.DCW).isEmpty());
						})) //
				.deactivate();
	}

	@Test
	public void testGetSunSpecChannelOrError() throws Exception {
		var sut = new DummySunSpecEssImpl();
		new ComponentTest(sut) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
						.withRegisters(40002, 1, 66) // Block 1
						.withRegisters(40070, 101, 50) // Block 101
						.withRegisters(40122, 0xFFFF, 0)) // END_OF_MAP
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build())
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.also(t -> {
							// read DefaultSunSpecModel.S_1
							assertFalse(sut.isSunSpecInitializationCompleted());
						})) //
				.next(new TestCase() //
						.also(t -> {
							// read DefaultSunSpecModel.S_101
							assertTrue(sut.isSunSpecInitializationCompleted());
						})) //
				.next(new TestCase() //
						.also(t -> {
							// Test getSunSpecChannelOrError() throws an error if Channel is not available.
							assertThrows(OpenemsException.class,
									() -> sut.<Channel<?>>getSunSpecChannelOrError(DefaultSunSpecModel.S103.DCW));
						})) //
				.deactivate();
	}
}
