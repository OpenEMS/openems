package io.openems.edge.bridge.modbus.test;

import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

import org.osgi.service.event.Event;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractModbusListener;
import com.ghgande.j2mod.modbus.procimg.ProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.Config;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.worker.internal.DefectiveComponents;
import io.openems.edge.bridge.modbus.api.worker.internal.TasksSupplier;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyModbusBridge extends AbstractModbusBridge implements BridgeModbusTcp, BridgeModbus, OpenemsComponent {

	private final ModbusTransaction modbusTransaction = new ModbusTransaction() {
		@Override
		public void execute() throws ModbusException {
			this.response = DummyModbusBridge.this.executeModbusRequest(this.request);
		}
	};
	private final AbstractModbusListener modbusListener = new AbstractModbusListener() {
		@Override
		public ProcessImage getProcessImage(int unitId) {
			return DummyModbusBridge.this.processImage;
		}

		@Override
		public void run() {
		}

		@Override
		public void stop() {
		}

	};
	private final TasksSupplier tasksSupplier;
	private final DefectiveComponents defectiveComponents;

	private SimpleProcessImage processImage = null;
	private InetAddress ipAddress = null;

	public DummyModbusBridge(String id) {
		this(id, LogVerbosity.NONE);
	}

	public DummyModbusBridge(String id, LogVerbosity logVerbosity) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values() //
		);
		for (var channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, new Config(id, "", false, logVerbosity, 2));
		this.tasksSupplier = getValueViaReflection(this.worker, "tasksSupplier");
		this.defectiveComponents = getValueViaReflection(this.worker, "defectiveComponents");
	}

	private synchronized DummyModbusBridge withProcessImage(Consumer<SimpleProcessImage> callback) {
		if (this.processImage == null) {
			this.processImage = new SimpleProcessImage();
		}
		callback.accept(this.processImage);
		return this;
	}

	/**
	 * Sets the IP-Address.
	 * 
	 * @param ipAddress an IP-Address.
	 * @return myself
	 * @throws UnknownHostException on parse error
	 */
	public DummyModbusBridge withIpAddress(String ipAddress) throws UnknownHostException {
		this.ipAddress = InetAddress.getByName(ipAddress);
		return this;
	}

	/**
	 * Sets the value of a Register.
	 * 
	 * @param address the Register address
	 * @param b1      first byte
	 * @param b2      second byte
	 * @return myself
	 */
	public DummyModbusBridge withRegister(int address, byte b1, byte b2) {
		return this.withProcessImage(pi -> pi.addRegister(address, new SimpleRegister(b1, b2)));
	}

	/**
	 * Sets the value of a Register.
	 * 
	 * @param address the Register address
	 * @param value   the value
	 * @return myself
	 */
	public DummyModbusBridge withRegister(int address, int value) {
		return this.withProcessImage(pi -> pi.addRegister(address, new SimpleRegister(value)));
	}

	/**
	 * Sets the values of Registers.
	 * 
	 * @param startAddress the start Register address
	 * @param values       the values
	 * @return myself
	 */
	public DummyModbusBridge withRegisters(int startAddress, int... values) {
		for (var value : values) {
			this.withRegister(startAddress++, value);
		}
		return this;
	}

	/**
	 * Sets the values of Registers.
	 * 
	 * @param startAddress the start Register address
	 * @param values       the values
	 * @return myself
	 */
	public DummyModbusBridge withRegisters(int startAddress, int[]... values) {
		for (var a : values) {
			for (var b : a) {
				this.withRegister(startAddress++, b);
			}
		}
		return this;
	}

	/**
	 * NOTE: {@link DummyModbusBridge} does not call parent handleEvent().
	 */
	@Override
	public synchronized void handleEvent(Event event) {
		// NOTE: TOPIC_CYCLE_EXECUTE_WRITE is not implemented (yet)
		if (this.processImage == null) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.onBeforeProcessImage();
		}
	}

	private ModbusResponse executeModbusRequest(ModbusRequest request) {
		return request.createResponse(this.modbusListener);
	}

	private void onBeforeProcessImage() {
		var cycleTasks = this.tasksSupplier.getCycleTasks(this.defectiveComponents);
		for (var readTask : cycleTasks.reads()) {
			readTask.execute(this);
		}
	}

	@Override
	public InetAddress getIpAddress() {
		if (this.ipAddress != null) {
			return this.ipAddress;
		}
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public ModbusTransaction getNewModbusTransaction() throws OpenemsException {
		return this.modbusTransaction;
	}

	@Override
	public void closeModbusConnection() {
	}

}
