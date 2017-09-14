package io.openems.impl.controller.api.modbustcp;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

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
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;

import io.openems.api.doc.ChannelDoc;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.core.Databus;
import io.openems.core.utilities.BitUtils;

/**
 * Holds the mapping between Modbus addresses ("ref") and OpenEMS Channel addresses and answers Modbus-TCP Slave
 * requests.
 *
 * @author stefan.feilmeier
 *
 */
public class MyProcessImage implements ProcessImage {
	/**
	 * Holds the link between Modbus address ("ref") and ChannelAddress
	 */
	private final NavigableMap<Integer, ChannelAddress> ref2Channel = new TreeMap<>();
	/**
	 * Holds the already used Refs to avoid double declaration
	 */
	private final TreeSet<Integer> usedRefs = new TreeSet<>();
	/**
	 * Holds the link between ChannelAddress and ChannelDoc
	 * Each ChannelDoc is guaranteed to have a bitLength
	 */
	private final Map<ChannelAddress, ChannelDoc> channel2Doc = new HashMap<>();

	private final Logger log = LoggerFactory.getLogger(this.getClass());;
	private final int unitId;
	private final Databus databus;

	protected MyProcessImage(int unitId) {
		this.unitId = unitId;
		this.databus = Databus.getInstance();
	}

	protected synchronized void clearMapping() {
		this.ref2Channel.clear();
		this.usedRefs.clear();
		this.channel2Doc.clear();
	}

	protected synchronized void addMapping(int ref, ChannelAddress channelAddress, ChannelDoc channelDoc)
			throws OpenemsException {
		if (ref < 0) {
			throw new OpenemsException(
					"Modbus address [" + ref + "] for Channel [" + channelAddress + "] must be a positive number.");
		}
		Optional<Class<?>> typeOpt = channelDoc.getTypeOpt();
		if (!typeOpt.isPresent()) {
			throw new OpenemsException(
					"Type for Channel [" + channelAddress + "] is not defined. Annotation is missing.");
		}
		Optional<Integer> bitLengthOpt = channelDoc.getBitLengthOpt();
		if (!bitLengthOpt.isPresent()) {
			throw new OpenemsException("BitLength for Channel [" + channelAddress + "] is not defined.");
		}
		int registerLength = bitLengthOpt.get() / 16;
		for (int i = ref; i < ref + registerLength; i++) {
			if (this.usedRefs.contains(i)) {
				throw new OpenemsException(
						"Modbus address [" + ref + "] for [" + channelAddress + "] is already occupied.");
			}
			this.usedRefs.add(i);
		}
		this.ref2Channel.put(ref, channelAddress);
		this.channel2Doc.put(channelAddress, channelDoc);
	}

	/**
	 * Data type converters. Always returns an array with length 2.
	 *
	 * @param object
	 * @return
	 * @throws NotImplementedException
	 */
	private InputRegister[] toInputRegister(Optional<?> objectOpt) throws NotImplementedException {
		if (!objectOpt.isPresent()) {
			return new InputRegister[] { new SimpleInputRegister() };
		}
		Object object = objectOpt.get();
		byte[] b = BitUtils.toBytes(object);
		InputRegister[] result = new InputRegister[b.length / 2];
		for (int i = 0; i < b.length / 2; i++) {
			result[i] = new SimpleInputRegister(b[i * 2], b[i * 2 + 1]);
		}
		return result;
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
		SortedMap<Integer, ChannelAddress> refs = this.ref2Channel.subMap(offset, offset + count);
		InputRegister[] result = new InputRegister[count];
		for (int i = 0; i < count;) {
			int ref = i + offset;
			// get channel value as InputRegister[]
			if (!refs.containsKey(ref)) {
				this.throwIllegalAddressException("No mapping defined for Modbus address [" + ref + "].");
			}
			ChannelAddress channelAddress = refs.get(ref);
			Optional<?> valueOpt = databus.getValue(channelAddress);
			InputRegister[] value;
			try {
				value = toInputRegister(valueOpt);
				// add value to result
				for (int j = 0; j < value.length; j++) {
					if (i + j > result.length - 1) {
						this.throwIllegalAddressException("Mobus result is not fitting in RegisterRange. Offset ["
								+ offset + "] Count [" + count + "]");
					}
					result[i + j] = value[j];
				}
				// increase i by bitlength
				i += value.length;
			} catch (NotImplementedException e) {
				this.throwIllegalAddressException(
						"Mobus result is not fitting in RegisterRange. Offset [" + offset + "] Count [" + count + "]");
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
