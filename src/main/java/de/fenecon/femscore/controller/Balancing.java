package de.fenecon.femscore.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.counter.Socomec;
import de.fenecon.femscore.modbus.device.ess.Cess;
import de.fenecon.femscore.modbus.device.ess.Ess;
import de.fenecon.femscore.modbus.device.ess.EssProtocol;
import de.fenecon.femscore.modbus.protocol.BitsElement;
import de.fenecon.femscore.modbus.protocol.BooleanBitElement;
import de.fenecon.femscore.modbus.protocol.SignedIntegerDoublewordElement;
import de.fenecon.femscore.modbus.protocol.SignedIntegerWordElement;
import de.fenecon.femscore.modbus.protocol.UnsignedIntegerDoublewordElement;
import de.fenecon.femscore.modbus.protocol.UnsignedShortWordElement;

public abstract class Balancing extends Controller {
	private final static Logger log = LoggerFactory.getLogger(Balancing.class);

	private final boolean allowChargeFromAC;

	private int minSoc = 10;

	public Balancing(String name, Map<String, Ess> essDevices, Map<String, Counter> counterDevices,
			boolean allowChargeFromAc) {
		super(name, essDevices, counterDevices);
		this.allowChargeFromAC = allowChargeFromAc;
	}

	public void setMinSoc(int minSoc) {
		this.minSoc = minSoc;
	}

	public int getMinSoc() {
		return minSoc;
	}

	@Override
	public void init() {
		Cess cess = (Cess) (esss.get("cess0"));
		BitsElement bitsElement = (BitsElement) cess.getElement(EssProtocol.SystemState.name());
		BooleanBitElement cessRunning = (BooleanBitElement) bitsElement.getBit(EssProtocol.SystemStates.Running.name());
		Boolean isCessRunning = cessRunning.getValue();
		if (isCessRunning == null) {
			log.info("No connection to CESS");
		} else if (isCessRunning) {
			log.info("CESS is running");
		} else {
			log.warn("CESS is NOT running!");
			// TODO: activate it
		}
	}

	private int lastSetCessActivePower = 0;
	private int lastCounterActivePower = 0;
	private int lastCessActivePower = 0;
	private int lastDeviationDelta = 0;

	@Override
	public void run() {
		Cess cess = (Cess) (esss.get("cess0"));
		UnsignedShortWordElement cessSoc = cess.getSoc();
		SignedIntegerWordElement cessActivePower = cess.getActivePower();
		SignedIntegerWordElement cessReactivePower = cess.getReactivePower();
		UnsignedShortWordElement cessApparentPower = cess.getApparentPower();
		SignedIntegerWordElement cessAllowedCharge = cess.getAllowedCharge();
		UnsignedShortWordElement cessAllowedDischarge = cess.getAllowedDischarge();
		UnsignedShortWordElement cessAllowedApparent = cess.getAllowedApparent();
		SignedIntegerWordElement cessSetActivePower = cess.getSetActivePower();

		Socomec counter = (Socomec) (counters.get("grid"));
		SignedIntegerDoublewordElement counterActivePower = counter.getActivePower();
		SignedIntegerDoublewordElement counterReactivePower = counter.getReactivePower();
		UnsignedIntegerDoublewordElement counterApparentPower = counter.getApparentPower();
		UnsignedIntegerDoublewordElement counterActivePostiveEnergy = counter.getActivePositiveEnergy();
		UnsignedIntegerDoublewordElement counterActiveNegativeEnergy = counter.getActiveNegativeEnergy();

		// ess set active power deviation:
		// lastCalculatedCessActivePower = lastCalculatedCessActivePower
		// + (lastCalculatedCessActivePower - cessActivePower.getValue()) / 2;

		int calculatedCessActivePower;

		// } else {
		// normal operation
		// calculatedCessActivePower = (lastCalculatedCessActivePower +
		// counterActivePower.getValue()) / 100 * 100;

		// change in ActivePower => deviationDelta
		int diffCounterActivePower = lastCounterActivePower - counterActivePower.getValue();
		int diffCessActivePower = lastCessActivePower - cessActivePower.getValue();
		int deviationDelta = 0;
		if (counterActivePower.getValue() < 0) {
			deviationDelta = lastDeviationDelta - 100;
		} else if (counterActivePower.getValue() < 100) {
			deviationDelta = lastDeviationDelta;
		} else if (Math.abs(diffCounterActivePower - diffCessActivePower) <= 200) {
			deviationDelta = lastDeviationDelta + 100;
		}

		// actual calculation
		calculatedCessActivePower = (cessActivePower.getValue() + counterActivePower.getValue() + deviationDelta);

		// round to 100: cess can only be controlled with precision 100 W
		calculatedCessActivePower = calculatedCessActivePower / 100 * 100;

		if (calculatedCessActivePower > 0) { // discharge
			if (cessSoc.getValue() < this.getMinSoc()) { // low SOC
				// TODO: Hysterese einbauen, sonst springt der SOC die ganze
				// Zeit
				// zwischen 9 und 10 %:
				// [ 10 %] PWR: [ -100 W 500 VA 0 Var] COUNTER: [ 8350 W 6950 VA
				// 10860 Var + 913 kWh - 284 kWh] SET: [8300]
				// [ 10 %] PWR: [ 3200 W 400 VA 3500 Var] COUNTER: [ 3320 W 8450
				// VA
				// 9080 Var + 913 kWh - 284 kWh] SET: [6500]
				// [ 9 %] PWR: [ 6300 W 200 VA 6500 Var] COUNTER: [ 1780 W 8210
				// VA
				// 8220 Var + 913 kWh - 284 kWh] SET: [0]
				// [ 9 %] PWR: [ 4600 W 200 VA 4700 Var] COUNTER: [ 7570 W 7540
				// VA
				// 10680 Var + 913 kWh - 284 kWh] SET: [0]
				// [ 9 %] PWR: [ -100 W 400 VA 0 Var] COUNTER: [ 8300 W 6970 VA
				// 10840 Var + 913 kWh - 284 kWh] SET: [0]
				calculatedCessActivePower = 0;
			} else if (calculatedCessActivePower > cessAllowedDischarge.getValue()) {
				// not allowed to discharge with such high power
				calculatedCessActivePower = cessAllowedDischarge.getValue();
			} else {
				// discharge with calculated value
			}
		} else { // charge
			if (allowChargeFromAC) { // charging is allowed
				// TODO if (calculatedCessActivePower <
				// cessAllowedCharge.getValue()) {
				if (calculatedCessActivePower < -1000) {
					// not allowed to charge with such high power
					calculatedCessActivePower = cessAllowedCharge.getValue();
				} else {
					// charge with calculated value
				}
			} else { // charging is not allowed
				calculatedCessActivePower = 0;
			}
		}
		lastDeviationDelta = deviationDelta;

		cess.addToWriteQueue(cessSetActivePower, cessSetActivePower.toRegister(calculatedCessActivePower));

		lastSetCessActivePower = calculatedCessActivePower;
		lastCessActivePower = cessActivePower.getValue();
		lastCounterActivePower = counterActivePower.getValue();

		/*
		 * log.info("[" + cessSoc.readable() + "] PWR: [" +
		 * cessActivePower.readable() + " " + cessReactivePower.readable() + " "
		 * + cessApparentPower.readable() + "] ALLOWED: [" +
		 * cessAllowedCharge.readable() + " " + cessAllowedDischarge.readable()
		 * + " " + cessAllowedApparent.readable() + "] COUNTER: [" +
		 * counterActivePower.readable() + " " + counterReactivePower.readable()
		 * + " " + counterApparentPower.readable() + "] SET: [" +
		 * calculatedCessActivePower + "]");
		 */
		log.info("[" + cessSoc.readable() + "] PWR: [" + cessActivePower.readable() + " " + cessReactivePower.readable()
				+ " " + cessApparentPower.readable() + "] COUNTER: [" + counterActivePower.readable() + " "
				+ counterReactivePower.readable() + " " + counterApparentPower.readable() + " +"
				+ counterActivePostiveEnergy.readable() + " -" + counterActiveNegativeEnergy.readable() + "] SET: ["
				+ calculatedCessActivePower + "]");
	}

	private int roundTo100(int value) {
		return ((value + 99) / 100) * 100;
	}
}
