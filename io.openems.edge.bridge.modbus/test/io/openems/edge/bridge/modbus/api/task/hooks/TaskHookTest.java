package io.openems.edge.bridge.modbus.api.task.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

import com.ghgande.j2mod.modbus.io.ModbusRTUTransport;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.BridgeModbusSerialImpl;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.MyConfigSerial;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.hooks.mocks.DummySerialListener;
import io.openems.edge.bridge.modbus.api.task.hooks.mocks.DummyTaskHook;
import io.openems.edge.bridge.modbus.test.ModbusSlaveMock;
import io.openems.edge.bridge.modbus.test.SerialConnectionMock;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

class TaskHookTest {
	private static final String PORTNAME = "DUMMY";
	private static final int BAUDRATE = 19200;
	private static final int SLAVE_1_UNIT_ID = 10;
	private static final int SLAVE_2_UNIT_ID = 20;
	private static final Duration WAIT_BETWEEN_UNIT_ID_CHANGE = Duration.ofMillis(15L);

	private static ModbusSlave slave1;
	private static ModbusSlave slave2;
	private static AbstractModbusBridge bridge;

	@Test
	void textHookExecution() throws Exception {
		var hookOnTask = new DummyTaskHook();
		var hookOnComponent = new DummyTaskHook();

		var component = new DummyModbusComponent("comp1", bridge, SLAVE_1_UNIT_ID, new ChannelId[0]);
		component.addModbusTaskHook(hookOnComponent);

		this.sendTestRequest(component, task -> task.addHook(hookOnTask));

		assertEquals(DummyTaskHook.HookState.POST_EXECUTE, hookOnTask.getState());
		assertEquals(DummyTaskHook.HookState.POST_EXECUTE, hookOnComponent.getState());
		assertEquals(SLAVE_1_UNIT_ID, bridge.getLastTransferInfo().unitId());
	}

	@Test
	void testWaitBetweenUnitIdHook() throws Exception {
		var component2 = new DummyModbusComponent("comp1", bridge, SLAVE_2_UNIT_ID, new ChannelId[0]);
		component2.addModbusTaskHook(new WaitBetweenUnitIdHook(WAIT_BETWEEN_UNIT_ID_CHANGE));

		this.sendTestRequest(new DummyModbusComponent("comp2", bridge, SLAVE_1_UNIT_ID, new ChannelId[0]));

		var stopwatch = Stopwatch.createStarted();
		this.sendTestRequest(component2);
		stopwatch.stop();

		assertTrue(stopwatch.elapsed().toMillis() >= WAIT_BETWEEN_UNIT_ID_CHANGE.toMillis());
	}

	private void sendTestRequest(DummyModbusComponent component) {
		this.sendTestRequest(component, FunctionUtils::doNothing);
	}

	private void sendTestRequest(DummyModbusComponent component, Consumer<AbstractTask<?, ?>> extraSteps) {
		var task = new FC3ReadRegistersTask(500, Priority.HIGH, new SignedWordElement(500), new SignedWordElement(501));

		task.setParent(component);
		extraSteps.accept(task);
		task.execute(bridge);
	}

	@BeforeAll
	static void setup() throws Exception {
		var serialParams = new SerialParameters();
		serialParams.setPortName(PORTNAME);
		serialParams.setBaudRate(BAUDRATE);

		var ports = SerialConnectionMock.create(serialParams);

		var transport = new ModbusRTUTransport();
		transport.setCommPort(ports.getServer());
		ports.getServer().setModbusTransport(transport);

		var connForSlave1 = ports.createClientConnection();
		slave1 = ModbusSlaveMock.register(connForSlave1, serialParams, SLAVE_1_UNIT_ID,
				() -> new DummySerialListener(connForSlave1), new ModbusRTUTransport());

		var connForSlave2 = ports.createClientConnection();
		slave2 = ModbusSlaveMock.register(connForSlave2, serialParams, SLAVE_2_UNIT_ID,
				() -> new DummySerialListener(connForSlave2), new ModbusRTUTransport());

		var bridgeConfig = MyConfigSerial.create().setId("modbus0").setPortName(PORTNAME).setBaudRate(BAUDRATE)
				.setStopbits(Stopbit.ONE).setDatabits(8).setParity(Parity.NONE).setLogVerbosity(LogVerbosity.NONE)
				.build();

		bridge = new BridgeModbusSerialImpl() {
			@Override
			protected synchronized AbstractSerialConnection getModbusConnection() throws OpenemsException {
				return ports.getServer();
			}

			@Activate
			public void activate(ComponentContext context, MyConfigSerial config) {
				super.activate(context, config);
			}
		};

		new ComponentTest(bridge) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(bridgeConfig);
	}

	@AfterAll
	static void tearDown() {
		// Normally we need to stop, but j2mod is buggy currently.
		// ModbusSlave::closeListener is throwing UnsupportedOperationException because
		// it's using listenerThread.stop() - stopping threads is not supported anymore
		if (slave1 != null) {
			// slave1.close();
		}
		if (slave2 != null) {
			// slave2.close();
		}
	}
}
