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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.core.utilities.api.ApiWorker;

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
	private final ApiWorker apiWorker;

	protected MyProcessImage(ApiWorker apiWorker) {
		this.apiWorker = apiWorker;
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
		ChannelRegisterMap channelRegistermap = new ChannelRegisterMap(channelAddress, channelDoc, this.apiWorker);
		if (!this.registerMaps.subMap(ref, ref + channelRegistermap.getRegisters().length - 1).isEmpty()) {
			throw new OpenemsException(
					"Modbus address [" + ref + "] for [" + channelAddress + "] is already occupied.");
		}
		this.registerMaps.put(ref, channelRegistermap);
	}

	/*
	 * Implementations of ProcessImage
	 */
	@Override
	public synchronized DigitalOut[] getDigitalOutRange(int offset, int count) throws IllegalAddressException {
		// TODO implement getDigitalOutRange
		this.throwIllegalAddressException("getDigitalOutRange is not implemented");
		return new DigitalOut[] {};
	}

	@Override
	public synchronized DigitalOut getDigitalOut(int ref) throws IllegalAddressException {
		// TODO implement getDigitalOut
		log.warn("getDigitalOut is not implemented");
		return null;
	}

	@Override
	public synchronized int getDigitalOutCount() {
		// TODO implement getDigitalOutCount
		log.warn("getDigitalOutCount is not implemented");
		return 0;
	}

	@Override
	public synchronized DigitalIn[] getDigitalInRange(int offset, int count) throws IllegalAddressException {
		// TODO implement getDigitalInRange
		this.throwIllegalAddressException("getDigitalInRange is not implemented");
		return new DigitalIn[] {};
	}

	@Override
	public synchronized DigitalIn getDigitalIn(int ref) throws IllegalAddressException {
		// TODO implement getDigitalIn
		this.throwIllegalAddressException("getDigitalIn is not implemented");
		return null;
	}

	@Override
	public synchronized int getDigitalInCount() {
		// TODO implement getDigitalInCount
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
			InputRegister[] registers = registerMaps.get(ref).getRegisters();
			for (int j = 0; j < registers.length; j++) {
				if (i + j > result.length - 1) {
					this.throwIllegalAddressException("Mobus result is not fitting in RegisterRange. Offset [" + offset
							+ "] Count [" + count + "]");
				}
				result[i + j] = registers[j];
			}
			// increase i by register count
			i += registers.length;
		}
		return result;
	}

	@Override
	public synchronized InputRegister getInputRegister(int ref) throws IllegalAddressException {
		// TODO implement getInputRegister
		this.throwIllegalAddressException("getInputRegister is not implemented");
		return null;
	}

	@Override
	public synchronized int getInputRegisterCount() {
		// TODO implement getInputRegisterCount
		log.warn("getInputRegisterCount is not implemented");
		return 0;
	}

	@Override
	public synchronized Register[] getRegisterRange(int offset, int count) throws IllegalAddressException {
		SortedMap<Integer, ChannelRegisterMap> registerMaps = this.registerMaps.subMap(offset, offset + count);
		if (registerMaps.firstKey() != offset) {
			this.throwIllegalAddressException("No valid mapping for Modbus address [" + offset + "].");
		}
		if (registerMaps.lastKey() + registerMaps.get(registerMaps.lastKey()).getRegisters().length != offset + count) {
			this.throwIllegalAddressException("Modbus register range has no valid length.");
		}
		Register[] registers = new Register[count];
		int i = 0;
		for (ChannelRegisterMap map : registerMaps.values()) {
			for (Register register : map.getRegisters()) {
				registers[i++] = register;
			}
		}
		return registers;
	}

	@Override
	public synchronized Register getRegister(int ref) throws IllegalAddressException {
		SortedMap<Integer, ChannelRegisterMap> registerMaps = this.registerMaps.subMap(0, ref + 1);
		int lastKey = registerMaps.lastKey();
		ChannelRegisterMap registerMap = registerMaps.get(lastKey);
		if (registerMap.getRegisters().length > 1) {
			this.throwIllegalAddressException("Channel [" + registerMap.getChannelAddress()
			+ "] consists of more than one register. Modbus address [" + ref + "] is invalid.");
		}
		int offset = ref - lastKey;
		if (lastKey + registerMap.getRegisters().length < ref) {
			this.throwIllegalAddressException("No mapping defined for Modbus address [" + ref + "].");
		}
		Register[] registers = registerMap.getRegisters();
		Register register = registers[offset];
		return register;
	}

	@Override
	public synchronized int getRegisterCount() {
		// TODO implement getRegisterCount
		log.warn("getRegisterCount is not implemented");
		return 0;
	}

	@Override
	public synchronized File getFile(int ref) throws IllegalAddressException {
		// TODO implement getFile
		log.warn("getFile is not implemented");
		return null;
	}

	@Override
	public synchronized File getFileByNumber(int ref) throws IllegalAddressException {
		// TODO implement getFileByNumber
		log.warn("getFileByNumber is not implemented");
		return null;
	}

	@Override
	public synchronized int getFileCount() {
		// TODO implement getFileCount
		log.warn("getFileCount is not implemented");
		return 0;
	}

	@Override
	public synchronized FIFO getFIFO(int ref) throws IllegalAddressException {
		// TODO implement getFIFO
		log.warn("getFIFO is not implemented");
		return null;
	}

	@Override
	public synchronized FIFO getFIFOByAddress(int ref) throws IllegalAddressException {
		// TODO implement getFIFOByAddress
		log.warn("getFIFOByAddress is not implemented");
		return null;
	}

	@Override
	public synchronized int getFIFOCount() {
		// TODO implement getDigitalOutRange
		log.warn("getDigitalOutRange is not implemented");
		return 0;
	}

	private void throwIllegalAddressException(String message) throws IllegalAddressException {
		IllegalAddressException error = new IllegalAddressException(message);
		log.error(error.getMessage());
		throw error;
	}

	@Override
	public int getUnitID() {
		// TODO Auto-generated method stub
		return 0;
	}
}
