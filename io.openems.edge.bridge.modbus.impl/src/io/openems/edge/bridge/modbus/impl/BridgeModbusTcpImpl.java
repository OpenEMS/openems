package io.openems.edge.bridge.modbus.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;

import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.impl.internal.ReadTask;
import io.openems.edge.bridge.modbus.impl.internal.WriteTask;
import io.openems.edge.bridge.modbus.protocol.ModbusProtocol;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.worker.AbstractWorker;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Modbus.Tcp", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BridgeModbusTcpImpl extends AbstractOpenemsComponent implements BridgeModbusTcp, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeModbusTcpImpl.class);
	private final ModbusWorker worker = new ModbusWorker();

	public BridgeModbusTcpImpl() {
		this.addChannels( //
				new IntegerReadChannel(this, OpenemsComponent.ChannelId.STATE));
	}

	/**
	 * The configured IP address
	 */
	private String ipAddress = "";

	/**
	 * Is the device with this UnitId ok?
	 */
	private final Set<Integer> defectiveUnitIds = new ConcurrentSkipListSet<Integer>();

	/**
	 * ReadTasks that are required to run once per Scheduler cycle
	 */
	private final List<ReadTask> requiredReadTasks = new ArrayList<>();

	/**
	 * ReadTasks that are required to run once in a while
	 */
	private final List<ReadTask> readTasks = new ArrayList<>();

	/**
	 * WriteTasks
	 */
	private final List<WriteTask> writeTasks = new ArrayList<>();

	@Activate
	void activate(Config config) {
		super.activate(config.id(), config.enabled());
		this.ipAddress = config.ip();
		this.worker.activate(config.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	public void addProtocol(String sourceId, Integer unitId, ModbusProtocol protocol) {
		if (unitId == null || protocol == null) {
			return;
		}
		synchronized (this.writeTasks) {
			// TODO
			// for (WriteRange range : protocol.getWriteRanges()) {
			// this.writeTasks.add(new WriteTask(sourceId, unitId, range));
			// }
		}
		synchronized (this.readTasks) {
			protocol.getRanges().forEach(range -> {
				this.readTasks.add(new ReadTask(sourceId, unitId, range));
			});
		}
	}

	public void removeProtocol(String sourceId) {
		synchronized (this.writeTasks) {
			// TODO
			// for (WriteRange range : protocol.getWriteRanges()) {
			// this.writeTasks.add(new WriteTask(sourceId, unitId, range));
			// }
		}
		synchronized (this.readTasks) {
			this.readTasks.removeIf(task -> task.getSourceId().equals(sourceId));
		}
	}

	private class ModbusWorker extends AbstractWorker {
		private ModbusTCPMaster master = null;

		/**
		 * Next planned readTasks
		 */
		private final Queue<ReadTask> nextReadTasks = new LinkedList<>();

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
			log.info("Forever...");
			// get ModbusMaster or abort
			ModbusTCPMaster master = getModbusMaster();
			if (master == null) {
				return;
			}

			// refill NextReadTasks if it is empty
			if (nextReadTasks.isEmpty()) {
				refillNextReadTasksQueue();
			}

			// execute next read task
			ReadTask task = nextReadTasks.poll();
			if (task == null) {
				// no task. Abort here.
				return;
			}
			try {
				task.execute(master);
			} catch (ModbusException e) {
				System.out.println(e.getMessage());
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private ModbusTCPMaster getModbusMaster() {
			if (this.master == null) {
				ModbusTCPMaster master = new ModbusTCPMaster(ipAddress, 502, 10000, true);
				try {
					master.connect();
					this.master = master;
				} catch (Exception e) {
					log.error("Unable to connect to [" + ipAddress + "]: " + e.getMessage());
				}
			}
			return this.master;
		}

		/**
		 * Refills 'nextReadTasks' queue.
		 * 
		 * This checks if a device is listed as defective and - if it is - adds only one
		 * task with this unitId to the queue
		 */
		private void refillNextReadTasksQueue() {
			Set<Integer> addedUnitIds = new HashSet<>();
			synchronized (readTasks) {
				readTasks.forEach(task -> {
					int unitId = task.getModbusUnitId();
					if (defectiveUnitIds.contains(unitId)) {
						if (!addedUnitIds.contains(unitId)) {
							// if device is listed as defective -> add only one task with this unitId
							nextReadTasks.add(task);
							addedUnitIds.add(unitId);
						}
					} else {
						// if device with unitId is ok -> add always
						nextReadTasks.add(task);
					}
				});
			}
		}

		@Override
		protected int getCycleTime() {
			// TODO calculate cycle time to optimize handling of read and write tasks
			return 1000;
		}
	}
}
