package de.fenecon.femscore.modbus.protocol.interfaces;

import com.ghgande.j2mod.modbus.procimg.Register;

public interface WordElement {
	public void update(Register register);
}
