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

public class BalancingWithoutAcGenerator extends Controller {
	private final static Logger log = LoggerFactory.getLogger(BalancingWithoutAcGenerator.class);

	public BalancingWithoutAcGenerator(Map<String, Ess> essDevices, Map<String, Counter> counterDevices) {
		super(essDevices, counterDevices);
	}

	@Override
	public void init() {
		Cess cess = (Cess) (essDevices.get("cess0"));
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
		Cess cess = (Cess) (essDevices.get("cess0"));
		UnsignedShortWordElement cessSoc = cess.getSoc();
		SignedIntegerWordElement cessActivePower = cess.getActivePower();
		SignedIntegerWordElement cessReactivePower = cess.getReactivePower();
		UnsignedShortWordElement cessApparentPower = cess.getApparentPower();
		SignedIntegerWordElement cessAllowedCharge = cess.getAllowedCharge();
		UnsignedShortWordElement cessAllowedDischarge = cess.getAllowedDischarge();
		UnsignedShortWordElement cessAllowedApparent = cess.getAllowedApparent();
		SignedIntegerWordElement cessSetActivePower = cess.getSetActivePower();

		Socomec counter = (Socomec) (counterDevices.get("grid"));
		SignedIntegerDoublewordElement counterActivePower = counter.getActivePower();
		SignedIntegerDoublewordElement counterReactivePower = counter.getReactivePower();
		UnsignedIntegerDoublewordElement counterApparentPower = counter.getApparentPower();

		// ess set active power deviation:
		// lastCalculatedCessActivePower = lastCalculatedCessActivePower
		// + (lastCalculatedCessActivePower - cessActivePower.getValue()) / 2;

		int calculatedCessActivePower;
		if (cessSoc.getValue() < 10) {
			// low SOC
			calculatedCessActivePower = 0;
		} else {
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
			log.info("DeviationDelta " + deviationDelta);

			// actual calculation
			calculatedCessActivePower = (cessActivePower.getValue() + counterActivePower.getValue() + deviationDelta);

			// round to 100: cess can only be controlled with precision 100 W
			calculatedCessActivePower = calculatedCessActivePower / 100 * 100;

			if (calculatedCessActivePower > 0) {
				// discharge
				if (calculatedCessActivePower > cessAllowedDischarge.getValue()) {
					// not allowed to discharge with such high power
					calculatedCessActivePower = cessAllowedDischarge.getValue();
				}
			} else {
				// charge
				calculatedCessActivePower = 0;
				// never charge from AC, because we don't have an external AC
				// generator
			}

			lastDeviationDelta = deviationDelta;
		}

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
				+ counterReactivePower.readable() + " " + counterApparentPower.readable() + "] SET: ["
				+ calculatedCessActivePower + "]");
	}

	private int roundTo100(int value) {
		return ((value + 99) / 100) * 100;
	}
}
