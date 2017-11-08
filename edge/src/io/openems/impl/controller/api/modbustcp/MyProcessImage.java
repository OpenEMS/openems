package io.openems.impl.controller.api.modbustcp;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.DigitalIn;
import com.ghgande.j2mod.modbus.procimg.DigitalOut;
import com.ghgande.j2mod.modbus.procimg.FIFO;
import com.ghgande.j2mod.modbus.procimg.File;
import com.ghgande.j2mod.modbus.procimg.IllegalAddressException;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.ProcessImage;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.api.doc.ChannelDoc;
import io.openems.api.exception.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.core.ApiWorker;

/**
 * Holds the mapping between Modbus addresses ("ref") and OpenEMS Channel addresses and answers Modbus-TCP Slave
 * requests.
 *
 * @author stefan.feilmeier
 *
 */
public class MyProcessImage implements ProcessImage {
	/**
	 * Holds the link between Modbus address ("ref") and ChannelRegisterMap
	 */
	private final NavigableMap<Integer, ChannelRegisterMap> registerMaps = new TreeMap<>();

	private final Logger log = LoggerFactory.getLogger(this.getClass());;
	private final int unitId;
	// private final ApiWorker apiWorker;

	protected MyProcessImage(int unitId, ApiWorker apiWorker) {
		this.unitId = unitId;
		// this.apiWorker = apiWorker;
	}

	protected synchronized void clearMapping() {
		this.registerMaps.clear();
	}

	protected synchronized void addMapping(int ref, ChannelAddress channelAddress, ChannelDoc channelDoc)
			throws OpenemsException {
		if (ref < 0) {
			throw new OpenemsException(
					"Modbus address [" + ref + "] for Channel [" + channelAddress + "] must be a positive number.");
		}
		ChannelRegisterMap channelRegistermap = new ChannelRegisterMap(channelAddress, channelDoc);
		if (!this.registerMaps.subMap(ref, ref + channelRegistermap.registerCount() - 1).isEmpty()) {
			throw new OpenemsException(
					"Modbus address [" + ref + "] for [" + channelAddress + "] is already occupied.");
		}
		this.registerMaps.put(ref, channelRegistermap);
	}


	/*
	 * Implementations of ProcessImage
	 */

	@Override
	public int getUnitID() {
		return this.unitId;
	}

	@Override
	public synchronized DigitalOut[] getDigitalOutRange(int offset, int count) throws IllegalAddressException {
		log.warn("getDigitalOutRange is not implemented");
		return new DigitalOut[] {};
	}

	@Override
	public synchronized DigitalOut getDigitalOut(int ref) throws IllegalAddressException {
		log.warn("getDigitalOut is not implemented");
		return null;
	}

	@Override
	public synchronized int getDigitalOutCount() {
		log.warn("getDigitalOutCount is not implemented");
		return 0;
	}

	@Override
	public synchronized DigitalIn[] getDigitalInRange(int offset, int count) throws IllegalAddressException {
		log.warn("getDigitalInRange is not implemented");
		return new DigitalIn[] {};
	}

	@Override
	public synchronized DigitalIn getDigitalIn(int ref) throws IllegalAddressException {
		log.warn("getDigitalIn is not implemented");
		return null;
	}

	@Override
	public synchronized int getDigitalInCount() {
		log.warn("getDigitalInCount is not implemented");
		return 0;
	}

	@Override
	public synchronized InputRegister[] getInputRegisterRange(int offset, int count) throws IllegalAddressException {
		SortedMap<Integer, ChannelRegisterMap> registerMaps = this.registerMaps.subMap(offset, offset + count);

		InputRegister[] result = new InputRegister[count];
		for (int i = 0; i < count;) {
			int ref = i + offset;
			// get channel value as InputRegister[]
			if (!registerMaps.containsKey(ref)) {
				this.throwIllegalAddressException("No mapping defined for Modbus address [" + ref + "].");
			}
			try {
				InputRegister[] registers = registerMaps.get(ref).getReadRegisters();
				for (int j = 0; j < registers.length; j++) {
					if (i + j > result.length - 1) {
						this.throwIllegalAddressException("Mobus result is not fitting in RegisterRange. Offset ["
								+ offset + "] Count [" + count + "]");
					}
					result[i + j] = registers[j];
				}
				// increase i by register count
				i += registers.length;
			} catch (OpenemsException e) {
				this.throwIllegalAddressException(e.getMessage());
			}
		}
		return result;
	}

	@Override
	public synchronized InputRegister getInputRegister(int ref) throws IllegalAddressException {
		log.warn("getInputRegister is not implemented");
		return null;
	}

	@Override
	public synchronized int getInputRegisterCount() {
		log.warn("getInputRegisterCount is not implemented");
		return 0;
	}

	@Override
	public synchronized Register[] getRegisterRange(int offset, int count) throws IllegalAddressException {
		log.warn("getRegisterRange is not implemented");
		return new Register[] {};
	}

	@Override
	public synchronized Register getRegister(int ref) throws IllegalAddressException {
		log.warn("getRegister is not implemented");
		return null;
	}

	@Override
	public synchronized int getRegisterCount() {
		log.warn("getRegisterCount is not implemented");
		return 0;
	}

	@Override
	public synchronized File getFile(int ref) throws IllegalAddressException {
		log.warn("getFile is not implemented");
		return null;
	}

	@Override
	public synchronized File getFileByNumber(int ref) throws IllegalAddressException {
		log.warn("getFileByNumber is not implemented");
		return null;
	}

	@Override
	public synchronized int getFileCount() {
		log.warn("getFileCount is not implemented");
		return 0;
	}

	@Override
	public synchronized FIFO getFIFO(int ref) throws IllegalAddressException {
		log.warn("getFIFO is not implemented");
		return null;
	}

	@Override
	public synchronized FIFO getFIFOByAddress(int ref) throws IllegalAddressException {
		log.warn("getFIFOByAddress is not implemented");
		return null;
	}

	@Override
	public synchronized int getFIFOCount() {
		log.warn("getDigitalOutRange is not implemented");
		return 0;
	}

	private void throwIllegalAddressException(String message) throws IllegalAddressException {
		IllegalAddressException error = new IllegalAddressException(message);
		log.error(error.getMessage());
		throw error;
	}
}
