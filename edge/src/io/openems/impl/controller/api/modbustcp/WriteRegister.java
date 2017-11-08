package io.openems.impl.controller.api.modbustcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;;

public class WriteRegister extends io.openems.impl.controller.api.modbustcp.Register implements Register {

	private final Logger log = LoggerFactory.getLogger(WriteRegister.class);

	@Override
	public int getValue() {
		log.warn("getValue is not implemented");
		return 0;
	}

	@Override
	public int toUnsignedShort() {
		log.warn("toUnsignedShort is not implemented");
		return 0;
	}

	@Override
	public short toShort() {
		log.warn("toShort is not implemented");
		return 0;
	}

	@Override
	public byte[] toBytes() {
		log.warn("toBytes is not implemented");
		return null;
	}

	@Override
	public void setValue(int v) {
		log.warn("setValue(int " + v + ") is not implemented");

	}

	@Override
	public void setValue(short s) {
		log.warn("setValue(short " + s + ") is not implemented");

	}

	@Override
	public void setValue(byte[] bytes) {
		log.warn("setValue(" + bytes + ") is not implemented");
	}

}
