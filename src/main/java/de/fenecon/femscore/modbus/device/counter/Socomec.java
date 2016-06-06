package de.fenecon.femscore.modbus.device.counter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.fenecon.femscore.modbus.protocol.ElementBuilder;
import de.fenecon.femscore.modbus.protocol.ElementLength;
import de.fenecon.femscore.modbus.protocol.ElementRange;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;
import de.fenecon.femscore.modbus.protocol.SignedIntegerDoublewordElement;
import de.fenecon.femscore.modbus.protocol.UnsignedIntegerDoublewordElement;

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
		/*
		 * protocol.addElementRange(new ElementRange(0xc568, new
		 * ElementBuilder(0xc568).name(CounterProtocol.ActivePower).multiplier(
		 * 10).signed(true)
		 * .length(ElementLength.DOUBLEWORD).unit("W").inverted(inverted).build(
		 * ), new
		 * ElementBuilder(0xc56a).name(CounterProtocol.ReactivePower).multiplier
		 * (10).signed(true)
		 * .length(ElementLength.DOUBLEWORD).unit("VA").inverted(inverted).build
		 * (), new
		 * ElementBuilder(0xc56c).name(CounterProtocol.ApparentPower).multiplier
		 * (10)
		 * .length(ElementLength.DOUBLEWORD).unit("Var").inverted(inverted).
		 * build()));
		 */
		protocol.addElementRange(new ElementRange(0xc568,
				new ElementBuilder(0xc568).name(CounterProtocol.ActivePower).multiplier(10).signed(true)
						.length(ElementLength.DOUBLEWORD).unit("W").build(),
				new ElementBuilder(0xc56a).name(CounterProtocol.ReactivePower).multiplier(10).signed(true)
						.length(ElementLength.DOUBLEWORD).unit("VA").build(),
				new ElementBuilder(0xc56c).name(CounterProtocol.ApparentPower).multiplier(10)
						.length(ElementLength.DOUBLEWORD).unit("Var").build()));
		// if (!inverted) {
		protocol.addElementRange(new ElementRange(0xc652,
				new ElementBuilder(0xc652).name(CounterProtocol.ActivePositiveEnergy).length(ElementLength.DOUBLEWORD)
						.unit("kWh").build(),
				new ElementBuilder(0xc654).name(CounterProtocol.ReactivePositiveEnergy).length(ElementLength.DOUBLEWORD)
						.unit("kvarh").build(),
				new ElementBuilder(0xc656).name(CounterProtocol.ApparentEnergy).length(ElementLength.DOUBLEWORD)
						.unit("kVAh").build(),
				new ElementBuilder(0xc658).name(CounterProtocol.ActiveNegativeEnergy).length(ElementLength.DOUBLEWORD)
						.unit("kWh").build(),
				new ElementBuilder(0xc65a).name(CounterProtocol.ReactiveNegativeEnergy).length(ElementLength.DOUBLEWORD)
						.unit("kvarh").build()));
		/*
		 * } else { protocol.addElementRange(new ElementRange(0xc652, new
		 * ElementBuilder(0xc652).name(CounterProtocol.ActiveNegativeEnergy)
		 * .length(ElementLength.DOUBLEWORD).unit("kWh").build(), new
		 * ElementBuilder(0xc654).name(CounterProtocol.ReactiveNegativeEnergy)
		 * .length(ElementLength.DOUBLEWORD).unit("kvarh").build(), new
		 * ElementBuilder(0xc656).name(CounterProtocol.ApparentEnergy).length(
		 * ElementLength.DOUBLEWORD) .unit("kVAh").inverted(inverted).build(),
		 * new ElementBuilder(0xc658).name(CounterProtocol.ActivePositiveEnergy)
		 * .length(ElementLength.DOUBLEWORD).unit("kWh").build(), new
		 * ElementBuilder(0xc65a).name(CounterProtocol.ReactivePositiveEnergy)
		 * .length(ElementLength.DOUBLEWORD).unit("kvarh").build())); }
		 */

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

	public SignedIntegerDoublewordElement getActivePower() {
		return (SignedIntegerDoublewordElement) getElement(CounterProtocol.ActivePower.name());
	}

	public SignedIntegerDoublewordElement getReactivePower() {
		return (SignedIntegerDoublewordElement) getElement(CounterProtocol.ReactivePower.name());
	}

	public UnsignedIntegerDoublewordElement getApparentPower() {
		return (UnsignedIntegerDoublewordElement) getElement(CounterProtocol.ApparentPower.name());
	}

	public UnsignedIntegerDoublewordElement getActivePositiveEnergy() {
		return (UnsignedIntegerDoublewordElement) getElement(CounterProtocol.ActivePositiveEnergy.name());
	}

	public UnsignedIntegerDoublewordElement getActiveNegativeEnergy() {
		return (UnsignedIntegerDoublewordElement) getElement(CounterProtocol.ActiveNegativeEnergy.name());
	}
}
