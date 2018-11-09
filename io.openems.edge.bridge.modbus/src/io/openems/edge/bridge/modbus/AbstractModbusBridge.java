package io.openems.edge.bridge.modbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.worker.AbstractCycleWorker;

/**
 * Abstract service for connecting to, querying and writing to a Modbus device
 * 
 */
public abstract class AbstractModbusBridge extends AbstractOpenemsComponent implements EventHandler {

	/**
	 * Default Modbus timeout in [ms]
	 * 
	 * Modbus library default is 3000 ms
	 */
	protected final static int DEFAULT_TIMEOUT = 1000;
	/**
	 * Default Modbus retries
	 * 
	 * Modbus library default is 5
	 */
	protected final static int DEFAULT_RETRIES = 1;

	private final Logger log = LoggerFactory.getLogger(AbstractModbusBridge.class);
	private final ModbusWorker worker = new ModbusWorker();

	/**
	 * Set ForceWrite to interrupt the ReadTasks and execute the WriteTasks
	 * immediately.
	 */
	private final AtomicBoolean forceWrite = new AtomicBoolean(false);

	public AbstractModbusBridge() {
		Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	/**
	 * Remember defective devices (Unit IDs)?
	 */
	@SuppressWarnings("unused")
	private final Set<Integer> defectiveUnitIds = new ConcurrentSkipListSet<Integer>();

	/**
	 * Holds the added protocols per source Component-ID
	 * 
	 * @param config
	 */
	private final Multimap<String, ModbusProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	protected void activate(ComponentContext context, String service_pid, String id, boolean enabled) {
		super.activate(context, service_pid, id, enabled);
		if (this.isEnabled()) {
			this.worker.activate(id);
		}
	}

	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
		this.closeModbusConnection();
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

	private class ModbusWorker extends AbstractCycleWorker {

		@Override
		public void activate(String name) {
			super.activate(name);
		}

		@Override
		public void deactivate() {
			super.deactivate();
		}

		@Override
		protected void forever() {
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
							writeTask.executeWrite(AbstractModbusBridge.this);
						} catch (OpenemsException e) {
							logError(log, writeTask.toString() + " write failed: " + e.getMessage());
						}
					});
				}
				/*
				 * Execute next read abstractTask
				 */
				try {
					readTask.executeQuery(AbstractModbusBridge.this);
				} catch (OpenemsException e) {
					// TODO remember defective unitid
					logError(log, readTask.toString() + " read failed: " + e.getMessage());
					// invalidate elements of this task
					for (ModbusElement<?> element : readTask.getElements()) {
						log.debug("invalidate element: " + element.toString());
						element.invalidate();
					}
				}
			});
		}

		/**
		 * Returns the 'nextReadTasks' list.
		 * 
		 * This checks if a device is listed as defective and - if it is - adds only one
		 * abstractTask with this unitId to the queue
		 */
		private List<ReadTask> getNextReadTasks() {
			List<ReadTask> result = new ArrayList<>();
			protocols.values().forEach(protocol -> {
				// get the next read tasks from the protocol
				List<ReadTask> nextReadTasks = protocol.getNextReadTasks();
				// check if the unitId is defective
				// int unitId = protocol.getUnitId();
				// FIXME: if we do the following in here, we will eventually miss the
				// ONCE-priority tasks
				// if (nextReadTasks.size() > 0 && defectiveUnitIds.contains(unitId)) {
				// // it is defective. Add only one read abstractTask.
				// // This avoids filling the queue with requests that cannot be fulfilled
				// anyway
				// // because the unitId is not reachable
				// result.add(nextReadTasks.get(0));
				// } else {
				// add all tasks to the next tasks
				result.addAll(nextReadTasks);
				// }
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
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.forceWrite.set(true);
			this.worker.triggerNextCycle();
			break;
		}
	}

	/**
	 * Creates a new Modbus Transaction on an open Modbus connection
	 * 
	 * @return
	 * @throws OpenemsException
	 */
	public abstract ModbusTransaction getNewModbusTransaction() throws OpenemsException;

	/**
	 * Closes the Modbus connection
	 */
	public abstract void closeModbusConnection();
}
