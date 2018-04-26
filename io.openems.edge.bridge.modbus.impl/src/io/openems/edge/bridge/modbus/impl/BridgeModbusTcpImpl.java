package io.openems.edge.bridge.modbus.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

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

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.controllerexecutor.EdgeEventConstants;
import io.openems.edge.common.worker.AbstractWorker;

/**
 * Provides a service for connecting to, querying and writing to a Modbus/TCP
 * device
 * 
 * @author stefan.feilmeier
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Modbus.Tcp", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeModbusTcpImpl extends AbstractOpenemsComponent
		implements BridgeModbusTcp, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(BridgeModbusTcpImpl.class);
	private final ModbusWorker worker = new ModbusWorker();

	/**
	 * Set ForceWrite to interrupt the ReadTasks and execute the WriteTasks
	 * immediately.
	 */
	private final AtomicBoolean forceWrite = new AtomicBoolean(false);

	public BridgeModbusTcpImpl() {
		Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	/**
	 * The configured IP address
	 */
	private String ipAddress = "";

	/**
	 * Remember defective devices (Unit IDs)?
	 */
	private final Set<Integer> defectiveUnitIds = new ConcurrentSkipListSet<Integer>();

	/**
	 * Holds the added protocols per source Component-ID
	 * 
	 * @param config
	 */
	private final Multimap<String, ModbusProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.ipAddress = config.ip();
		if (this.isEnabled()) {
			this.worker.activate(config.id());
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	/**
	 * Adds the protocol
	 * 
	 * @param sourceId
	 * @param protocol
	 */
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.protocols.put(sourceId, protocol);
	}

	/**
	 * Removes the protocol
	 */
	public void removeProtocol(String sourceId) {
		this.protocols.removeAll(sourceId);
	}

	private class ModbusWorker extends AbstractWorker {
		private ModbusTCPMaster master = null;

		@Override
		public void activate(String name) {
			super.activate(name);
		}

		@Override
		public void deactivate() {
			super.deactivate();
			// disconnect from Modbus
			ModbusTCPMaster master = this.master;
			if (master != null) {
				master.disconnect();
			}
		}

		@Override
		protected void forever() {
			// get ModbusMaster or abort
			ModbusTCPMaster master = getModbusMaster();
			if (master == null) {
				return;
			}

			// get the read tasks for this run
			List<ReadTask> nextReadTasks = this.getNextReadTasks();

			/*
			 * execute next read tasks
			 */
			nextReadTasks.forEach(readTask -> {
				/*
				 * was FORCE WRITE set? -> exeute WriteTasks now
				 */
				if (forceWrite.getAndSet(false)) {
					List<WriteTask> writeTasks = this.getNextWriteTasks();
					writeTasks.forEach(writeTask -> {
						try {
							writeTask.executeWrite(master);
						} catch (ModbusException e) {
							log.error(id() + ". Unable to execute modbus write: " + e.getMessage());
						}
					});
				}
				/*
				 * Execute next read task
				 */
				try {
					readTask.executeQuery(master);
				} catch (ModbusException e) {
					log.error(id() + ". Unable to execute modbus query: " + e.getMessage());
				}
			});
		}

		private ModbusTCPMaster getModbusMaster() {
			if (this.master == null) {
				ModbusTCPMaster master = new ModbusTCPMaster(ipAddress, 502, 10000, true);
				try {
					master.connect();
					this.master = master;
				} catch (Exception e) {
					log.error("Unable to connect to [" + ipAddress + "]: " + e.getMessage());
					// TODO set State to Fault
				}
			}
			return this.master;
		}

		/**
		 * Returns the 'nextReadTasks' list.
		 * 
		 * This checks if a device is listed as defective and - if it is - adds only one
		 * task with this unitId to the queue
		 */
		private List<ReadTask> getNextReadTasks() {
			List<ReadTask> result = new ArrayList<>();
			protocols.values().forEach(protocol -> {
				// get the next read tasks from the protocol
				List<ReadTask> nextReadTasks = protocol.getNextReadTasks();
				// check if the unitId is defective
				int unitId = protocol.getUnitId();
				if (nextReadTasks.size() > 0 && defectiveUnitIds.contains(unitId)) {
					// it is defective. Add only one read task.
					// This avoids filling the queue with requests that cannot be fulfilled anyway
					// because the unitId is not reachable
					result.add(nextReadTasks.get(0));
				} else {
					// add all tasks to the next tasks
					result.addAll(nextReadTasks);
				}
			});
			return result;
		}

		private List<WriteTask> getNextWriteTasks() {
			List<WriteTask> result = new ArrayList<>();
			protocols.values().forEach(protocol -> {
				result.addAll(protocol.getNextWriteTasks());
			});
			return result;
		}

		@Override
		protected int getCycleTime() {
			// TODO calculate cycle time to optimize handling of read and write tasks
			return 1000;
		}
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.forceWrite.set(true);
			break;
		}
	}
}
