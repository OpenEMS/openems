package io.openems.edge.ess.mr.gridcon;

import io.openems.edge.battery.soltaro.SoltaroBattery;

public class WeightingHelper {

	public static Float[] getWeighting(float activePower, SoltaroBattery b1, SoltaroBattery b2,
			SoltaroBattery b3) {

		Float[] ret = {0f, 0f, 0f};
		
		// Discharge
		if (activePower > 0) {
			ret[0] = getWeightingForDischarge(b1);
			ret[1] = getWeightingForDischarge(b2);
			ret[2] = getWeightingForDischarge(b3);
		// Charge
		} else if (activePower < 0) {
			ret[0] = getWeightingForCharge(b1);
			ret[1] = getWeightingForCharge(b2);
			ret[2] = getWeightingForCharge(b3);
		// active power is zero
		} else {
			ret = getWeightingForNoPower(b1, b2, b3);	
		}

		return ret;
	}
	
	protected static Float[] getWeightingForNoPower(SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3) {
		float factor = 100;
		
		float voltageA = Float.MAX_VALUE;
		float voltageB = Float.MAX_VALUE;
		float voltageC = Float.MAX_VALUE;
		
		float weightA = 0;
		if(isBatteryReady(b1)) {
			weightA = 1;
			voltageA = b1.getVoltage().value().get();
		} 
		float weightB = 0;
		if(isBatteryReady(b2)) {
			weightB = 1;
			voltageB = b2.getVoltage().value().get();
		}
		
		float weightC = 0;
		if(isBatteryReady(b3)) {
			weightC = 1;
			voltageC = b3.getVoltage().value().get();
		}		
		
		float minVoltage = Math.min(voltageA, Math.min(voltageB, voltageC));
		
		if (minVoltage < Float.MAX_VALUE) {
			weightA = Math.max((voltageA - minVoltage), 1) * weightA * factor;
			weightB = Math.max((voltageB - minVoltage), 1) * weightB * factor;
			weightC = Math.max((voltageC - minVoltage), 1) * weightC * factor;
		}
		
		return new Float[] {weightA, weightB, weightC};
	}

	static float getWeightingForCharge(SoltaroBattery b) {
		float weight = 0;
		if (b != null && isBatteryReady(b)) {
			float current = b.getChargeMaxCurrent().value().get();
			float voltage = b.getVoltage().value().get();
			weight = current * voltage;
		}
		return weight;
	}
	
	static float getWeightingForDischarge(SoltaroBattery b) {
		float weight = 0;
		if (b != null && isBatteryReady(b)) {
			float current = b.getDischargeMaxCurrent().value().get();
			float voltage = b.getVoltage().value().get();
			weight = current * voltage;
		}
		return weight;
	}

	protected static boolean isBatteryReady(SoltaroBattery battery) {
		if (battery == null) {
			return false;
		}
		return !battery.isUndefined() && battery.isRunning();
	}

	public static int getStringControlMode(SoltaroBattery battery1, SoltaroBattery battery2, SoltaroBattery battery3) {
		int weightingMode = 0; 

		boolean useBatteryStringA = (battery1 != null && battery1.isRunning());
		if (useBatteryStringA) {
			weightingMode = weightingMode + 1; // battA = 1 (2^0)
		}
		boolean useBatteryStringB = (battery2 != null && battery2.isRunning());
		if (useBatteryStringB) {
			weightingMode = weightingMode + 8; // battB = 8 (2^3)
		}
		boolean useBatteryStringC = (battery3 != null && battery3.isRunning());
		if (useBatteryStringC) {
			weightingMode = weightingMode + 64; // battC = 64 (2^6)
		}
		
		return weightingMode;
	}
}
