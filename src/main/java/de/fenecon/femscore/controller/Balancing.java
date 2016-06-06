package de.fenecon.femscore.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.counter.Socomec;
import de.fenecon.femscore.modbus.device.ess.Cess;
import de.fenecon.femscore.modbus.device.ess.Ess;
import de.fenecon.femscore.modbus.device.ess.EssProtocol;
import de.fenecon.femscore.modbus.protocol.BitElement;
import de.fenecon.femscore.modbus.protocol.BitsElement;
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
		BitElement cessRunning = bitsElement.getBit(EssProtocol.SystemStates.Running.name());
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
	private int lowSocCounter = 0;

	@Override
	public void run() {
		Cess cess = (Cess) (esss.get("cess0"));
		UnsignedShortWordElement cessSoc = cess.getSoc();
		SignedIntegerWordElement cessActivePower = cess.getActivePower();
		SignedIntegerWordElement cessReactivePower = cess.getReactivePower();
		UnsignedShortWordElement cessApparentPower = cess.getApparentPower();
		SignedIntegerWordElement cessAllowedCharge = cess.getAllowedCharge();
		UnsignedShortWordElement cessAllowedDischarge = cess.getAllowedDischarge();
		// UnsignedShortWordElement cessAllowedApparent =
		// cess.getAllowedApparent();
		SignedIntegerWordElement cessSetActivePower = cess.getSetActivePower();
		SignedIntegerWordElement cessPv1OutputPower = cess.getPv1OutputPower();
		SignedIntegerWordElement cessPv2OutputPower = cess.getPv2OutputPower();

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

		// low SOC hysteresis
		if (cessSoc.getValue() < this.getMinSoc()) {
			if (lowSocCounter < 0) {
				lowSocCounter = 1;
			} else if (lowSocCounter > 3) {
				lowSocCounter = 4;
			} else {
				lowSocCounter++;
			}
		} else {
			if (lowSocCounter > 0) {
				lowSocCounter = -1;
			} else if (lowSocCounter < -3) {
				lowSocCounter = -4;
			} else {
				lowSocCounter--;
			}
		}

		// actual power calculation
		calculatedCessActivePower = (cessActivePower.getValue() + counterActivePower.getValue() + deviationDelta);

		if (calculatedCessActivePower > 0) {
			// discharge
			if (lowSocCounter > 3) {
				// low soc
				calculatedCessActivePower = 0;
			} else if (lowSocCounter < 3) {
				// normal operation
				if (calculatedCessActivePower > cessAllowedDischarge.getValue()) {
					// not allowed to discharge with such high power
					calculatedCessActivePower = cessAllowedDischarge.getValue();
				} else {
					// discharge with calculated value
				}
			} else {
				System.out.println("vorher: " + calculatedCessActivePower + "; lowSocCounter: " + lowSocCounter);
				calculatedCessActivePower = (int) (calculatedCessActivePower * Math.abs(lowSocCounter) / 3.);
				System.out.println("nachher: " + calculatedCessActivePower);
			}

		} else {
			// charge
			if (allowChargeFromAC) { // charging is allowed
				if (calculatedCessActivePower < cessAllowedCharge.getValue()) {
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

		// TODO: safety - remove
		if (calculatedCessActivePower > 1000) {
			calculatedCessActivePower = 1000;
		} else if (calculatedCessActivePower < -1000) {
			calculatedCessActivePower = -1000;
		}

		// round to 100: cess can only be controlled with precision 100 W
		calculatedCessActivePower = calculatedCessActivePower / 100 * 100;

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
				+ " " + cessApparentPower.readable() + "] DCPV: [" + cessPv1OutputPower.readable()
				+ cessPv2OutputPower.readable() + "] COUNTER: [" + counterActivePower.readable() + " "
				+ counterReactivePower.readable() + " " + counterApparentPower.readable() + " +"
				+ counterActivePostiveEnergy.readable() + " -" + counterActiveNegativeEnergy.readable() + "] SET: ["
				+ calculatedCessActivePower + "]");
	}
}
