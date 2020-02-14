package io.openems.edge.ess.mr.gridcon.ongrid.state;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;

public class Run extends BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(Run.class);

	public Run(EssGridcon gridconPCS, SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3) {
		super(gridconPCS, b1, b2, b3);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.State.RUN;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be Stopped, ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED;
		}
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
		}
		
		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.State.RUN;
	}

	

	@Override
	public void act() {
		log.info("Set all parameters to gridcon!");
		
		// sometimes link voltage can be too low unrecognized by gridcon, i.e. no error message
		// in case of that, restart the system, but this should be detected by isError() function
		gridconPCS.runSystem();
		
		setStringWeighting();
		
		setStringControlMode();

	}
	
	private void setStringControlMode() {
			int weightingMode = 0; // Depends on number of battery strings!!!

			// --- // TODO if battery is not ready for work, remove it from the weighting
			// mode!!

			boolean useBatteryStringA = (battery1 != null && battery1.isRunning());
			if (useBatteryStringA) {
				weightingMode += 1; // battA = 1 (2^0)
			}
			boolean useBatteryStringB = (battery2 != null && battery2.isRunning());
			if (useBatteryStringB) {
				weightingMode += 8; // battB = 8 (2^3)
			}
			boolean useBatteryStringC = (battery3 != null && battery3.isRunning());
			if (useBatteryStringC) {
				weightingMode += 64; // battC = 64 (2^6)
			}

			gridconPCS.setStringControlMode(weightingMode);
		}
		
	private void setStringWeighting() {
		int activePower = gridconPCS.getActivePower().value().orElse(0);
		
		//Calculate string weightings
		//TODO set the weighting in another pos
				// set the weights for battery strings A, B and C.
				Map<GridConChannelId, Float> weightings = null;
				if (activePower > 0) {
					weightings =  getWeightingMap().get(KEY_POWER_DISCHARGING);// weightingMap.get(KEY_POWER_DISCHARGING);
				} else if (activePower < 0) {
					weightings = getWeightingMap().get(KEY_POWER_CHARGING);
				} else {
					weightings = getWeightingMap().get(KEY_POWER_IS_ZERO);
				}

				Float weightA = weightings.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A);
				Float weightB = weightings.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B);
				Float weightC = weightings.get(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C);

				System.out.println("Weight a: " + weightA + "Weight b: " + weightB + "Weight c: " + weightC);
				
				gridconPCS.setWeightStringA(weightA);
				gridconPCS.setWeightStringB(weightB);
				gridconPCS.setWeightStringC(weightC);
	}

	public static final String KEY_POWER_IS_ZERO = "zero";
	public static final String KEY_POWER_CHARGING = "charging";
	public static final String KEY_POWER_DISCHARGING = "discharging";
	
	protected Map<String, Map<GridConChannelId, Float>> getWeightingMap() {

		Map<String, Map<GridConChannelId, Float>> ret = new HashMap<>();

		Map<GridConChannelId, Float> chargeMap = new HashMap<>();
		Map<GridConChannelId, Float> dischargeMap = new HashMap<>();
		Map<GridConChannelId, Float> zeroMap = new HashMap<>();

		ret.put(KEY_POWER_CHARGING, chargeMap);
		ret.put(KEY_POWER_DISCHARGING, dischargeMap);
		ret.put(KEY_POWER_IS_ZERO, zeroMap);

		// Discharge
		if (battery1 != null && isBatteryReady(battery1)) {
			float currentA = battery1.getMaxDischargeCurrentX();
			float voltageA = battery1.getVoltageX();
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, currentA * voltageA);
		} else {
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
		}

		if (battery2 != null && isBatteryReady(battery2)) {
			float currentB = battery2.getMaxDischargeCurrentX();
			float voltageB = battery2.getVoltageX();
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, currentB * voltageB);
		} else {
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
		}

		if (battery3 != null && isBatteryReady(battery3)) {
			float currentC = battery3.getMaxDischargeCurrentX();
			float voltageC = battery3.getVoltageX();
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, currentC * voltageC);
		} else {
			dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);
		}

		// Charge
		if (battery1 != null && isBatteryReady(battery1)) {
			float currentA = battery1.getMaxChargeCurrentX();
			float voltageA = battery1.getVoltageX();
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, currentA * voltageA);
		} else {
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
		}

		if (battery2 != null && isBatteryReady(battery2)) {
			float currentB = battery2.getMaxChargeCurrentX();
			float voltageB = battery2.getVoltageX();
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, currentB * voltageB);
		} else {
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
		}

		if (battery3 != null && isBatteryReady(battery3)) {
			float currentC = battery3.getMaxChargeCurrentX();
			float voltageC = battery3.getVoltageX();
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, currentC * voltageC);
		} else {
			chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);
		}

		// active power is zero

		float factor = 100;
		float weightA = 0;
		float weightB = 0;
		float weightC = 0;
		if (battery1 != null  && battery2 != null && battery3 != null) { // ABC
			
			// Racks die abgeschalten sind dürfen nicht berücksichtigt werden
			// --> Gewichtung auf 0

				float averageVoltageA =  battery1.getVoltageX();
				float averageVoltageB =  battery2.getVoltageX();
				float averageVoltageC =  battery3.getVoltageX();

				float min = Math.min(averageVoltageA, Math.min(averageVoltageB, averageVoltageC));
				if (isBatteryReady(battery1)) {
					weightA = (averageVoltageA - min) * factor;
				}
				if (isBatteryReady(battery2)) {
					weightB = (int) ((averageVoltageB - min) * factor);
				}
				if (isBatteryReady(battery3)) {
					weightC = (int) ((averageVoltageC - min) * factor);
				}
		} else if (battery1 != null && battery2 != null && battery3 == null) { // AB
		
				float averageVoltageA = battery1.getVoltageX();
				float averageVoltageB = battery2.getVoltageX();
				float min = Math.min(averageVoltageA, averageVoltageB);
				if (isBatteryReady(battery1)) {
					weightA = (averageVoltageA - min) * factor;
				}
				if (isBatteryReady(battery2)) {
					weightB = (averageVoltageB - min) * factor;
				}
		} else if (battery1 != null && battery2 == null && battery3 != null) { // AC
				float averageVoltageA =  battery1.getVoltageX();
				float averageVoltageC =  battery3.getVoltageX();
				float min = Math.min(averageVoltageA, averageVoltageC);
				if (isBatteryReady(battery1)) {
					weightA = (averageVoltageA - min) * factor;
				}
				if (isBatteryReady(battery3)) {
					weightC = (averageVoltageC - min) * factor;
				}
		} else if (battery1 == null && battery2 != null && battery3 != null) { // BC
				float averageVoltageB =  battery2.getVoltageX();
				float averageVoltageC =  battery3.getVoltageX();
				float min = Math.min(averageVoltageB, averageVoltageC);
				if (isBatteryReady(battery2)) {
					weightB = (averageVoltageB - min) * factor;
				}
				if (isBatteryReady(battery3)) {
					weightC = (averageVoltageC - min) * factor;
				}
		}

		zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, weightA);
		zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, weightB);
		zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, weightC);

		return ret;
	}
	
	protected boolean isBatteryReady(SoltaroBattery battery) {
		if (battery == null) {
			return false;
		}
		return battery.isRunning();
	}
}
