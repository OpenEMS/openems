package de.fenecon.femscore.modbus.device.counter;

import de.fenecon.femscore.modbus.protocol.ElementBuilder;
import de.fenecon.femscore.modbus.protocol.ElementRange;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;

public class Socomec extends Counter {

	public Socomec(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	@Override
	public String toString() {
		return "Socomec [name=" + name + ", unitid=" + unitid + "]";
	}

	@Override
	protected ModbusProtocol getMainProtocol() {
		ModbusProtocol protocol = new ModbusProtocol();
		protocol.addElementRange(new ElementRange(0xc568,
				new ElementBuilder(0xc568).name(CounterProtocol.ActivePower).multiplier(10).signed(true).length(2)
						.unit("W").build(),
				new ElementBuilder(0xc56a).name(CounterProtocol.ReactivePower).multiplier(10).signed(true).length(2)
						.unit("VA").build(),
				new ElementBuilder(0xc56c).name(CounterProtocol.ApparentPower).multiplier(10).length(2).unit("Var")
						.build()));
		return protocol;
	}
}
