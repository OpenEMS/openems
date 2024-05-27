package io.openems.edge.controller.api.modbus.rtu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.IllegalAddressException;

public class MyIllegalAddressException extends IllegalAddressException {

	private static final long serialVersionUID = 1L;

	private final Logger log = LoggerFactory.getLogger(MyIllegalAddressException.class);

	public MyIllegalAddressException(MyProcessImage parent, String message) {
		super(message);
		parent.parent.logWarn(this.log, message);
	}

}
