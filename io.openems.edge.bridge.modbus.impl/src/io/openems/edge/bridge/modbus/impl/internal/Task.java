package io.openems.edge.bridge.modbus.impl.internal;

import java.util.Collections;
import java.util.Queue;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.google.common.collect.EvictingQueue;

public abstract class Task {

	private final Queue<Long> requiredTimes;
	private final String sourceId;
	
	protected final int modbusUnitId;

	public Task(String sourceId, int modbusUnitId) {
		this.sourceId = sourceId;
		this.modbusUnitId = modbusUnitId;
		this.requiredTimes = EvictingQueue.create(5);
		for (int i = 0; i < 5; i++) {
			this.requiredTimes.add(0L);
		}
	}

	public String getSourceId() {
		return sourceId;
	}

	public int getModbusUnitId() {
		return modbusUnitId;
	}

	public long getRequiredTime() {
		synchronized (this.requiredTimes) {
			return Collections.max(this.requiredTimes);
		}
	}

	public void execute(ModbusTCPMaster master) throws ModbusException {
		long timeBefore = System.currentTimeMillis();
		this._execute(master);
		synchronized (this.requiredTimes) {
			this.requiredTimes.add(System.currentTimeMillis() - timeBefore);
		}
	}

	protected abstract void _execute(ModbusTCPMaster master) throws ModbusException;

}
