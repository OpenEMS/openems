package de.fenecon.femscore.modbus.protocol.interfaces;

import com.ghgande.j2mod.modbus.procimg.Register;

public interface DoublewordElement {
	public void update(Register reg1, Register reg2);
}
