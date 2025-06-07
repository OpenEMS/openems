package io.openems.edge.bridge.sml;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.FlowControl;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding.DecodingException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.sml.api.BridgeSml;
import io.openems.edge.bridge.sml.api.DummySerialPort;
import io.openems.edge.bridge.sml.api.SmlTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = BridgeSmlSerialConfig.class, factory = true)
@Component(name = "Bridge.SmlSerial", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeSmlSerialImpl extends AbstractOpenemsComponent implements BridgeSml, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeSmlSerialImpl.class);

	public BridgeSmlSerialImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeSml.ChannelId.values() //
		);
	}

	private final Map<String, SmlTask> tasks = new HashMap<>();
	private final SmlWorker worker = new SmlWorker();

	private String portName;
	private SerialPortBuilder serialPortBuilder;
	
	@Reference
	private SerialPort smlSerialConnection;

	@Activate
	protected void activate(ComponentContext context, BridgeSmlSerialConfig config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.portName = config.portName();
		this.worker.activate(config.id());
		this.serialPortBuilder = SerialPortBuilder.newBuilder(config.portName()).setBaudRate(config.baudRate())
				.setDataBits(config.databits()).setFlowControl(config.flowControl()).setParity(config.parity())
				.setStopBits(config.stopbits());
		
		// Todo: Remove this once real Hardware can be used for verification later
		BridgeSmlSerialImpl.this.smlSerialConnection = new DummySerialPort("/etc/ttyUSB0", 9600, 30000, DataBits.DATABITS_8, Parity.EVEN,
				StopBits.STOPBITS_1, FlowControl.NONE,
				"1B1B1B1B0101010176050F1A20606200620072630101760101050508B5760B090149534B000403DF63010163C4BA0076050F1A2061620062007263070177010B090149534B000403DF63070100620AFFFF7262016507AFF5DC7A77078181C78203FF010101010449534B0177070100000009FF010101010B090149534B000403DF630177070100010800FF650000018201621E52FF59000000000D637E080177070100010801FF0101621E52FF59000000000D637E080177070100010802FF0101621E52FF5900000000000000000177070100100700FF0101621B520055000000A80177070100240700FF0101621B520055000000750177070100380700FF0101621B5200550000001601770701004C0700FF0101621B5200550000001D0177078181C78205FF0101010183020C2DE05C56024E1CD45280F4A0769A95E629CAE205C55C9F1683CA5419778E1D9BCFA1C577A6B36A92709EBF05EA21BD01010163291C0076050F1A206262006200726302017101630054001B1B1B1B1A00AE84");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		try {
			this.smlSerialConnection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.worker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.worker.triggerNextRun();
			break;
		}
	}

	private class SmlWorker extends AbstractCycleWorker {

		@Override
		protected void forever() throws OpenemsException, DecodingException {
			// Check if time passed by, if not, do nothing
			try {
				// Todo: Reactivate if real hardware can be used for verification
				//BridgeSmlSerialImpl.this.smlSerialConnection = BridgeSmlSerialImpl.this.serialPortBuilder.build();

				for (SmlTask task : BridgeSmlSerialImpl.this.tasks.values()) {
					try {
						var data = task.getRequest();
						data.getMessages();
						// "Before accessing elements of a variable data structure it has to be decoded
						// using the decode method." ??
						task.setResponse(data);
					} catch (IOException e) {
						//e.printStackTrace();
					}
				}

				BridgeSmlSerialImpl.this.smlSerialConnection.close();
			} catch (IOException e) {
				BridgeSmlSerialImpl.this.logError(BridgeSmlSerialImpl.this.log,
						"Connection via [" + BridgeSmlSerialImpl.this.portName + "] failed: " + e.getMessage());
			}
		}
	}

	@Override
	public void addTask(String sourceId, SmlTask task) {
		this.tasks.put(sourceId, task);
	}

	@Override
	public void removeTask(String sourceId) {
		this.tasks.remove(sourceId);
	}

	@Override
	public SerialPort getSmlConnection() {
		return this.smlSerialConnection;
	}
}
