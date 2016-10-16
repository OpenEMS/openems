package io.openems.impl.protocol.modbus.internal;

import com.ghgande.j2mod.modbus.procimg.Register;

public interface WordElement {
	public void setValue(Register register);
}
