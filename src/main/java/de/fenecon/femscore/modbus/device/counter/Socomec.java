package de.fenecon.femscore.modbus.device.counter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.fenecon.femscore.modbus.protocol.ElementBuilder;
import de.fenecon.femscore.modbus.protocol.ElementLength;
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
	protected ModbusProtocol getProtocol() {
		ModbusProtocol protocol = new ModbusProtocol();
		protocol.addElementRange(new ElementRange(0xc568,
				new ElementBuilder(0xc568).name(CounterProtocol.ActivePower).multiplier(10).signed(true)
						.length(ElementLength.DOUBLEWORD).unit("W").build(),
				new ElementBuilder(0xc56a).name(CounterProtocol.ReactivePower).multiplier(10).signed(true)
						.length(ElementLength.DOUBLEWORD).unit("VA").build(),
				new ElementBuilder(0xc56c).name(CounterProtocol.ApparentPower).multiplier(10)
						.length(ElementLength.DOUBLEWORD).unit("Var").build()));
		return protocol;
	}

	@Override
	public Set<String> getInitElements() {
		return null;
	}

	@Override
	public Set<String> getMainElements() {
		return new HashSet<String>(Arrays.asList( //
				CounterProtocol.ActivePower.name(), //
				CounterProtocol.ReactivePower.name(), //
				CounterProtocol.ApparentPower.name()));
	}
}
