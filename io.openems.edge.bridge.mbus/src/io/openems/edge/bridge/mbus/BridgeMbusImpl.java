package io.openems.edge.bridge.mbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openems.edge.bridge.mbus.api.ChannelRecord;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder;
import org.openmuc.jmbus.VariableDataStructure;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.MbusTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = ConfigMBus.class, factory = true)
@Component(name = "Bridge.Mbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeMbusImpl extends AbstractOpenemsComponent implements BridgeMbus, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeMbusImpl.class);

	public BridgeMbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
	}

	private final Map<String, MbusTask> tasks = new HashMap<>();
	private final MbusWorker worker = new MbusWorker();

	private MBusConnection mBusConnection;
	private MBusSerialBuilder builder;
	private String portName;
	private boolean debug;

	@Activate
	protected void activate(ComponentContext context, ConfigMBus config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.portName = config.portName();
		this.worker.activate(config.id());
		this.builder = MBusConnection.newSerialBuilder(this.portName).setBaudrate(config.baudrate());
		this.debug = config.debug();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.mBusConnection.close();
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

	public MBusConnection getmBusConnection() {
		return this.mBusConnection;
	}

	private class MbusWorker extends AbstractCycleWorker {

		@Override
		protected void forever() throws OpenemsException, DecodingException {
			try {
				BridgeMbusImpl.this.mBusConnection = BridgeMbusImpl.this.builder.build();

				for (MbusTask task : BridgeMbusImpl.this.tasks.values()) {

					// MBus devices have an optional polling interval. If enabled, the device is not polled every cycle
					// but only once every polling interval. This is used to to conserve the energy of battery powered
					// meters.
					// permissionToPoll() will always return true if no polling interval has been set.
					if (task.permissionToPoll()) {
						try {
							VariableDataStructure data = task.getRequest();
							// From jmbus library:
							// "Before accessing elements of a variable data structure it has to be decoded using the
							// decode method."
							// This decode() here is probably redundant, but it also doesn't hurt. MBus messages are
							// usually not encrypted. There is also currently no code to allow a decryption key to be
							// set for MBus devices.
							data.decode();
							task.processData(data);
							if (debug) {
								BridgeMbusImpl.this.logInfo(BridgeMbusImpl.this.log,
										"Polling M-Bus device [" + task.getMeterId() + "]:");
								BridgeMbusImpl.this.logInfo(BridgeMbusImpl.this.log, data.toString());
								BridgeMbusImpl.this.logInfo(BridgeMbusImpl.this.log, "Channels updated:");
								List<ChannelRecord> channelDataRecordsList = task.getOpenemsMbusComponent().getChannelDataRecordsList();
								for (ChannelRecord record : channelDataRecordsList) {
									BridgeMbusImpl.this.logInfo(BridgeMbusImpl.this.log,
											record.getChannel().channelId() + " - " + record.getChannel().getNextValue().get());
								}
								BridgeMbusImpl.this.logInfo(BridgeMbusImpl.this.log, "");
							}
						} catch (IOException e) {
							BridgeMbusImpl.this.logError(BridgeMbusImpl.this.log,
									"Connection to M-Bus device [" + task.getMeterId() + "] failed. "
											+ "Check the cable and/or the M-Bus PrimaryAddress of the device.");
						}
					}
				}

				BridgeMbusImpl.this.mBusConnection.close();
			} catch (IOException e) {
				BridgeMbusImpl.this.logError(BridgeMbusImpl.this.log,
						"Connection via [" + portName + "] failed: " + e.getMessage());
			}
		}
	}

	@Override
	public void addTask(String sourceId, MbusTask task) {
		this.tasks.put(sourceId, task);
	}

	@Override
	public void removeTask(String sourceId) {
		this.tasks.remove(sourceId);
	}

}
