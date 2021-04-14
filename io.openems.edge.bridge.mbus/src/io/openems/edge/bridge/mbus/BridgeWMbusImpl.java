package io.openems.edge.bridge.mbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.mbus.api.BridgeWMbus;
import io.openems.edge.bridge.mbus.api.WMbusProtocol;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.wireless.WMBusConnection;
import org.openmuc.jmbus.wireless.WMBusListener;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.openmuc.jmbus.wireless.WMBusMode;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

@Designate(ocd = ConfigWMBus.class, factory = true)
@Component(name = "Bridge.WirelessMbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeWMbusImpl extends AbstractOpenemsComponent implements BridgeWMbus, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeWMbusImpl.class);

	public BridgeWMbusImpl() {
		super(//
				ChannelId.values() //
		);
	}

	private final Map<String, WMbusProtocol> devices = new HashMap<>();
	private final WMbusWorker worker = new WMbusWorker();

	private WMBusConnection wMBusConnection;
	private WMBusConnection.WMBusSerialBuilder builder;
	private WMBusListener listener;
	private String portName;
	private WMBusConnection.WMBusManufacturer manufacturer;
	private WMBusMode mode;
	private boolean scan;
	private boolean debug;

	private final LinkedBlockingDeque<WMBusMessage> messageQueue = new LinkedBlockingDeque<>();

	@Activate
	protected void activate(ComponentContext context, ConfigWMBus configWMBus) {
		super.activate(context, configWMBus.id(), configWMBus.alias(), configWMBus.enabled());
		this.portName = configWMBus.portName();
		this.manufacturer = configWMBus.manufacturer();
		this.mode = configWMBus.mode();
		this.scan = configWMBus.scan();
		this.debug = configWMBus.debug();
		this.listener = new WMBusReceiver(this);

		this.worker.activate(configWMBus.id());

		this.builder = new WMBusConnection.WMBusSerialBuilder(manufacturer, listener, portName).setMode(mode);

		try {
			this.wMBusConnection = this.builder.build();
		} catch (IOException e) {
			this.logError(this.log,
					"Connection via [" + portName + "] failed: " + e.getMessage());
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		try {
			this.wMBusConnection.close();
		} catch (IOException e) {
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

	@Override
	public void addProtocol(String sourceId, WMbusProtocol protocol) {
		this.devices.put(sourceId, protocol);

	}

	@Override
	public void removeProtocol(String sourceId) {
		this.devices.remove(sourceId);
	}

	// All WMBus messages are collected by the WMBusReceiver and placed in the message queue. The forever
	// method takes the messages from the queue and processes them.
	public static class WMBusReceiver implements WMBusListener {
		private final BridgeWMbusImpl parent;

		public WMBusReceiver(BridgeWMbusImpl parent) {
			this.parent = parent;
		}

		@Override
		public void newMessage(WMBusMessage message) {
			parent.messageQueue.add(message);
		}

		@Override
		public void discardedBytes(byte[] bytes) {
		}

		@Override
		public void stoppedListening(IOException cause) {
			parent.restartConnection(cause);
		}

	}

	public void restartConnection(IOException cause) {
		this.logError(this.log,
				"Connection via [" + portName + "] failed: " + cause.getMessage());
	}

	private class WMbusWorker extends AbstractCycleWorker {

		@Override
		protected void forever() throws OpenemsException {

			// Structure of this code:
			// A message is taken from the message queue. Then the list of WMBus devices registered to the bridge is
			// traversed to see if the message is from any of those devices. A comparison is done first by radio address,
			// and if necessary also by meter number.
			// Currently, the comparison by meter number is only used to tell channel 1 and 2 apart for the Padpuls
			// Relay.
			// I'm not entirely sure if it is possible for two different devices to have the same radio address. If it
			// is, they could be told apart by their meter number. However, the "detection by meter number" of this
			// bridge can not be used by meter types other than the Padpuls Relay. This is a limitation of the jmbus
			// library. The reason is, the meter number is only readable after decoding the message, and the keys for
			// decoding are stored in a list with the radio address (more specifically, the dllSecondaryAddress) as the
			// identifier. This list is part of the jmbus library. So there can be only one key per radio address. The
			// Padpuls Relay circumvents this problem by using the same decryption key for both channels.
			// If you need to use two meters that happen to have the same radio address, "detection by meter number"
			// can be used to tell them apart if you set them to the same decryption key. "detection by meter number"
			// can be enabled by adding a few lines of code to the meter module.
			// If there are two devices using the same radio address but you want to read data from only one of them,
			// this is no problem. As long as they don't have the same decryption key, only messages from one devices
			// can be decoded. The messages from the other device can not be decoded and are not processed. You will get
			// constant decode-error messages though because of the other device.

			while (BridgeWMbusImpl.this.messageQueue.isEmpty() == false) {
				WMBusMessage message;
				try {
					message	= BridgeWMbusImpl.this.messageQueue.takeLast();
					// This secondary address is the data link layer (=dll) one. It contains the radio address.
					SecondaryAddress dllSecondaryAddress = message.getSecondaryAddress();
					if (BridgeWMbusImpl.this.scan) {
						BridgeWMbusImpl.this.logInfo(BridgeWMbusImpl.this.log,
								"Device found:\n Radio address: " + dllSecondaryAddress.getDeviceId()	// jmbus library calls the radio address deviceId.
										+ ", Manufacturer ID: " + dllSecondaryAddress.getManufacturerId()
										+ ", device version: " + dllSecondaryAddress.getVersion()
										+ ", device type: " + dllSecondaryAddress.getDeviceType());
					}

					for (WMbusProtocol device : BridgeWMbusImpl.this.devices.values()) {
						SecondaryAddress deviceDllAddress = device.getDllSecondaryAddress();
						if (debug) {
							BridgeWMbusImpl.this.logInfo(BridgeWMbusImpl.this.log,
									"Checking Device " + device.getComponentId() + " with radio address "
											+ (device.getRadioAddress()) + ".");
						}

						// This executes if no message from this device has been received yet. The reason why it is done
						// this way:
						// jmbus library stores the decryption key with the dllSecondaryAddress as identifier. The
						// dllSecondaryAddress contains the radio address, but is not identical to it. But the number
						// printed on a meter to identify it is the radio address, so this is what is entered in the
						// meter config.
						// The easiest way to get the dllSecondaryAddress is to simply take it from the first received
						// message of that device and store it in deviceDllAddress. If that field is still null (no message
						// yet from this device), extract the radio address from dllSecondaryAddress and identify by
						// radio address. Then get and store the dllSecondaryAddress for this device and use it to
						// register the decryption key. Further messages can then be identified directly by the
						// dllSecondaryAddress.
						if (deviceDllAddress == null) {
							String radioAddress = device.getRadioAddress();
							String detectedAddress = String.valueOf(dllSecondaryAddress.getDeviceId());
							if (debug) {
								BridgeWMbusImpl.this.logInfo(BridgeWMbusImpl.this.log,
										"Not yet detected. Comparing " + radioAddress + " with " + detectedAddress + ".");
							}
							if (detectedAddress.equals(radioAddress)) {
								deviceDllAddress = dllSecondaryAddress;	// Needed for if() branch in line 235.
								device.setDllSecondaryAddress(dllSecondaryAddress);	// This needs to happen before registerKey()
								device.registerKey(BridgeWMbusImpl.this.wMBusConnection);	// This won't work if dllSecondaryAddress is not set.
								BridgeWMbusImpl.this.logInfo(BridgeWMbusImpl.this.log,
										"Device " + device.getComponentId() + " with radio address " + radioAddress + " has been detected.");
							} else {
								// Detected device is not this device from the list. Abort here, otherwise null pointer
								// exception in next "if" because this device does not have the secondary address set yet.
								if (debug) {
									BridgeWMbusImpl.this.logInfo(BridgeWMbusImpl.this.log,
											"Nope, that's not this device. Moving on to next device in list (if there are more).");
								}
								continue;
							}
						}

						// This executes if the message is from this device.
						if (deviceDllAddress.hashCode() == dllSecondaryAddress.hashCode()) {
							VariableDataStructure data = message.getVariableDataResponse();	// data is the contents of the message.
							try {
								data.decode();	// WMBus messages are usually encrypted. Need to decode before it can be read.
								if (device.isIdentifyByMeterNumber()) {
									// This is needed to distinguishing between channel 1 and 2 for the Padpuls Relay.
									// Both channels have identical radio addresses, only the meter number is different.
									// Data has another secondary address, which is the "transport layer secondary address".
									// It contains the meter number.
									String detectedMeterNumber = String.valueOf(data.getSecondaryAddress().getDeviceId());	// jmbus calls the meter number deviceId. Very confusing, since radio address is also called deviceId in jmbus.
									if (detectedMeterNumber.equals(device.getMeterNumber()) == false) {
										// This is not the right device. Abort and try next device in list.
										continue;
									}
								}
								device.logSignalStrength(message.getRssi());	// This calls the logSignalStrength() method of the meter module.
								device.processData(data);	// This invokes the ChannelDataRecordMapper, filling the channels with data.
								if (BridgeWMbusImpl.this.scan) {
									BridgeWMbusImpl.this.logInfo(BridgeWMbusImpl.this.log, String.valueOf(message));
								}
							} catch (DecodingException e) {
								if (device.isUseErrorChannel()) {
									device.setError("Wrong decryption key");
								}
								BridgeWMbusImpl.this.logError(BridgeWMbusImpl.this.log,
										"Unable to fully decode received message for device "
												+ device.getComponentId() + " with ID " + device.getRadioAddress()
												+ ". Check the decryption key!");
							}
							// Message has been assigned to a device. No need to look through rest of the list.
							break;
						}
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

}
