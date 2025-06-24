package io.openems.edge.deye.ess;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.deye.battery.DeyeSunBattery;
import io.openems.edge.deye.dccharger.DeyeDcCharger;

import io.openems.edge.deye.enums.WorkState;
import io.openems.edge.deye.enums.EnableDisable;
import io.openems.edge.deye.enums.EnergyManagementModel;
import io.openems.edge.deye.enums.LimitControlFunction;

import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class ApplyPowerHandler {

	private final DeyeSunHybridImpl ess;
	private final DeyeSunBattery battery;
	private final DeyeDcCharger dcCharger;
	private final Logger log;

	
	private int sellPSet = 1000; //value in Reg 154 (W)
	
	private AverageCalculator batteryTargetPowerAvg = new AverageCalculator(5);		
	
	private int maxTargetW = 0; //save max Target power for error analysis
	private int maxBatteryPower = 0; //save max current battery  power for error analysis 
	private int maxGridPower =0;
	private int maxPvPower =0;
	private int batteryVoltageRaw =0;
	private int maxActivePower	 =0;
	
	private int minTargetW = 0; //save min Target power for error analysis
	private int minBatteryPower = 0; //save min current battery  power for error analysis 
	private int minGridPower =0;
	private int minPvPower =0;
	private int minActivePower	 =0;	
	private boolean valueSkipped = false;
	

	public ApplyPowerHandler(DeyeSunHybridImpl ess, DeyeSunBattery battery, DeyeDcCharger dcCharger) {
		this.ess = ess;
		this.battery = battery;
		this.dcCharger = dcCharger;
		this.log = this.ess.getLogger();
				
	}


	public void apply(int activePowerTarget, int reactivePower, int configuredMaxApparentPower)
			throws OpenemsNamedException {
		if (!ess.isManaged()) {
			log.debug("[ApplyPower] ESS not managed – skipping.");
			return;
		}

		if (ess.getWorkState() != WorkState.NORMAL) {
			log.error("ESS not in normal state. Skipping ApplyPower");
			return;
		}
		
		
		Integer maxApparentPower = ess.getMaxApparentPower().get();
		Integer batteryPower = battery.getDcPower().get(); // assume that this DcDischargePower
		Integer powerAcGrid = ess.getGridOutPower().get(); // power to or from grid, negative while power to grid
		Integer activePower = ess.getActivePower().get(); // Total inverter AC power out of battery, PV. Includes
		Integer dcDischargePower = ess.getDcDischargePower().get(); //
		Integer soc = ess.getSoc().get(); 
		
		Integer batteryVoltageRaw = battery.getBatteryVoltage().get(); // 0.01 V/LSB
		Integer pvPower = dcCharger.getActualPower().get(); // Stringsummen

		Integer maxAllowedChargePower = ess.getAllowedChargePower().get();
		Integer maxAllowedDischargePower = ess.getAllowedDischargePower().get();		
		int iSet =0;

		if (Stream.of(batteryVoltageRaw, batteryPower, powerAcGrid, pvPower, maxAllowedChargePower, maxAllowedDischargePower).anyMatch(Objects::isNull))
			return;

		int batteryPowerTarget = activePowerTarget - (activePower - dcDischargePower);
		int batteryPowerTarget1 = activePowerTarget - pvPower;
		//batteryPowerTarget = 1000;
		int pseudoPvPower = activePower - dcDischargePower;
		
		// ideally self-consumption of the inverter ~100W
		int balancingPower = activePowerTarget -powerAcGrid - activePower;

		if (maxAllowedChargePower == null || maxAllowedDischargePower == null) {
			ess.logDebug(log, "Max charge/discharge values not available. Skipping ApplyPower");
			return;
		}

		if (maxApparentPower == null) {
			ess.logDebug(log, "Max Apparent power not available. Skipping ApplyPower");
			return;
		}

		if (batteryPower == null) {
			ess.logDebug(log, "battery power not available. Skipping ApplyPower");
			return;
		}

		if (powerAcGrid == null) {
			ess.logDebug(log, "Grid out power not available. Skipping ApplyPower");
			return;
		}
		
		if (Math.abs(balancingPower) > 200) {
			
			
			
			maxAllowedDischargePower -= pvPower;
	
			double batteryVoltage = batteryVoltageRaw / 1000.0; // V
			
			// Error correction
			double pErr   = 0;
			int   targetPower = 0;
			pErr = batteryPowerTarget - batteryPower;        // W
			targetPower = (int) (batteryPowerTarget + pErr);		
			
			if (batteryPowerTarget > 0) { // discharging
	
				targetPower = Math.max(targetPower, batteryPowerTarget);
				targetPower = Math.min(targetPower, maxAllowedDischargePower);
				
			} else { // charging
	
				targetPower = Math.min(targetPower, batteryPowerTarget);
				
				if (soc < 100) { // battery full, maxAllowedChargePower must NOT be applied. Otherwise pv will be limited
					targetPower = Math.max(targetPower, maxAllowedChargePower);
				} else {
					targetPower = 100; // minimum
				}
				
			}
			
			// simple plausibility check
			// store highest values for most necessary parameters
			if (Math.abs(targetPower - this.batteryTargetPowerAvg.getAverage()) < 2000 || valueSkipped == true) {
				this.batteryTargetPowerAvg.addValue(targetPower); //
				valueSkipped = false;			
			} else {
				if (targetPower > maxTargetW) {
					maxTargetW = targetPower; // store highest value	
				}
				valueSkipped = true;
				
			}
			// Max
			if (batteryPower > maxBatteryPower) {
				maxBatteryPower = batteryPower;
			}
	
			if (powerAcGrid > maxGridPower) {
				maxGridPower = powerAcGrid;
			}		
	
			if (pvPower > maxPvPower) {
				maxPvPower = pvPower;
			}		
			
			if (batteryVoltageRaw > batteryVoltageRaw) {
				batteryVoltageRaw = batteryVoltageRaw;
			}				
	
			if (activePower > maxActivePower) {
				maxActivePower = activePower;
			}				
			
			// Min
			if (batteryPower < minBatteryPower) {
				minBatteryPower = batteryPower;
			}
	
			if (powerAcGrid < minGridPower) {
				minGridPower = powerAcGrid;
			}		
	
			if (pvPower < minPvPower) {
				minPvPower = pvPower;
			}		
			
			if (batteryVoltageRaw < batteryVoltageRaw) {
				batteryVoltageRaw = batteryVoltageRaw;
			}				
	
			if (activePower < minActivePower) {
				minActivePower = activePower;
			}			
			
			targetPower = this.batteryTargetPowerAvg.getAverage();
	
			// Neuer Sollwert für Register 154:
			sellPSet = (int)Math.round(
			              Math.max(-8000,
			                  Math.min(8000, targetPower)
			              )
			           );
	
			// 10-W-Rundung (der WR quantisiert sowieso):
			sellPSet = (sellPSet / 10) * 10;		
			
			
			double iTarget   = targetPower / batteryVoltage;                     // A
			
			// 
			if ( this.ess.getSolarSellMode() != EnableDisable.ENABLED) {
				this.ess.setSolarSellMode(EnableDisable.ENABLED);
			}
			
			// register 142
			if (this.ess.getLimitControlFunction() != LimitControlFunction.SELLING_ACTIVE) {
			 	this.ess.setLimitControlFunction(LimitControlFunction.SELLING_ACTIVE);
			}			
			
			this.ess.setTargetActivePower(targetPower);
			this.ess.setTargetCurrent(iSet);
			
	
				
			if (iTarget >= 0) { // Entladen
				iSet = (int) Math.ceil(iTarget);						
				
				if ( this.ess.getSellModeTimePoint1Capacity().get() != 5) {
					this.ess.setSellModeTimePoint1Capacity(5);
				}
				
				this.ess.setSellModeTimePoint1Power(sellPSet);
	
				//this.ess.setChargeDischargeMode(false, Math.abs(batteryPowerTarget));
				battery.setBatteryMaxDischargeCurrent(iSet *1000); // Reg 109  // Channel is in mA
				
				// If value is 0 inverter lowers PV production!!! wtf!
				if (battery.getBatteryMaxChargeCurrent().get() == null || battery.getBatteryMaxChargeCurrent().get() < 5000) {
					battery.setBatteryMaxChargeCurrent(5000);	
				}
				
			} else { // Laden
				//int iSet = (int) Math.ceil(iTarget);	
				iSet = (int) Math.floor(iTarget);
				// register 142
	
				this.ess.setChargeDischargeMode(true, Math.abs(batteryPowerTarget));			
				battery.setBatteryMaxChargeCurrent(iSet * -1000); // Reg 108 // Channel is in mA
				// If value is 0 inverter lowers PV production!!! wtf!
				if (battery.getBatteryMaxDischargeCurrent().get() == null || battery.getBatteryMaxDischargeCurrent().get() < 5000) {
					battery.setBatteryMaxDischargeCurrent(5000);	
				}				
			}			
			// feeding channels
			this.ess.setTargetActivePower(targetPower);
			this.ess.setTargetCurrent(iSet);				
		} else {
			if (this.ess.getEnergyManagementModel() != EnergyManagementModel.LOAD_FIRST) {
				this.ess.setEnergyManagementModel(EnergyManagementModel.LOAD_FIRST);	
			}
			if (this.ess.getLimitControlFunction() != LimitControlFunction.BUILT_IN) {
				this.ess.setLimitControlFunction(LimitControlFunction.BUILT_IN);	
			}
			
		}
			
	

		this.ess.logDebug(log,
				"\n-> Target Power: " + activePowerTarget +  
				"\n PvPower : " + pvPower +  " max: " + maxPvPower + "/min: " + minPvPower +
				"\n PseudoPvPower : " + pseudoPvPower +
				"\n ActivePower : " + activePower + " max: " + maxActivePower + "/min: " + minActivePower +
				"\n GridPower : " + powerAcGrid + " max: " + maxGridPower + "/min: " + minGridPower +
				"\n DcDischargePower : " + dcDischargePower + 
				"\n Battery Target: " + batteryPowerTarget +
				"\n CalculatedBattery Target: " + batteryPowerTarget1 +
				"\n current BatteryPower: " + batteryPower +  " max: " + maxBatteryPower + "/min: " + minBatteryPower +
				//"\n  Delta: " + pErr +
				"\n I target : " + iSet +   
				"\n P target : " + sellPSet + 
				"\n \n Energy Management Model: " + this.ess.getEnergyManagementModel() + 
				"\n Limit Control Function: " + this.ess.getLimitControlFunction()  
				

						+ "\n TimeOfUseSellingEnabled : " + this.ess.getTimeOfUseSellingEnabled() + "\n"
						+ "SellModeTimePoint1 " + this.ess.getSellModeTimePoint1().get() + "/"
						+ this.ess.getSellModeTimePoint1Power().get() + "W/" + this.ess.getSellModeTimePoint1Capacity()
						+ "\n" + "SellModeTimePoint2 " + this.ess.getSellModeTimePoint2().get() + "/"
						+ this.ess.getSellModeTimePoint2Power().get() + "W/" + this.ess.getSellModeTimePoint2Capacity()
						+ "\n" + "SellModeTimePoint3 " + this.ess.getSellModeTimePoint3().get() + "/"
						+ this.ess.getSellModeTimePoint3Power().get() + "W/" + this.ess.getSellModeTimePoint3Capacity()
						+ "\n" + "SellModeTimePoint4 " + this.ess.getSellModeTimePoint4().get() + "/"
						+ this.ess.getSellModeTimePoint4Power().get() + "W/" + this.ess.getSellModeTimePoint4Capacity()
						+ "\n" + "SellModeTimePoint5 " + this.ess.getSellModeTimePoint5().get() + "/"
						+ this.ess.getSellModeTimePoint5Power().get() + "W/" + this.ess.getSellModeTimePoint5Capacity()
						+ "\n" + "SellModeTimePoint6 " + this.ess.getSellModeTimePoint6().get() + "/"
						+ this.ess.getSellModeTimePoint6Power().get() + "W/" + this.ess.getSellModeTimePoint6Capacity()
						+ "\n" + "RemoteLockState : " + this.ess.getRemoteLockState() + "\n" + "EnableSwitch : "
						+ this.ess.getEnableSwitchState() + "\n" + "LimitControlFunction : "
						+ this.ess.getLimitControlFunction() + "\n" + "SolarSellMode : " + this.ess.getSolarSellMode()
						+ "\n" + "EnableGridCharge : " + this.ess.getEnableGridChargeState() + "\n"

		);
	}

	/**
	 * Limits the requested power to what the battery can actually provide.
	 */
	private int applyBatteryLimits(int requestedPower) {
		if (battery == null) {
			return requestedPower;
		}

		Integer allowedChargePower = ess.getAllowedChargePower().get();
		Integer allowedDischargePower = ess.getAllowedDischargePower().get();

		if (allowedChargePower == null || allowedDischargePower == null) {
			return requestedPower;
		}

		if (requestedPower < 0) { // charging

			return (int) Math.max(requestedPower, -allowedChargePower);
		} else { // discharging
			return (int) Math.min(requestedPower, allowedDischargePower);
		}
	}

	/**
	 * Calculate and store Max-AC-Export and -Import channels.
	 *
	 * @param maxApparentPower the max apparent power
	 */
	protected void calculateMaxAcPower(int maxApparentPower) {
		if (this.battery == null || this.ess == null || this.dcCharger == null) {
			return;
		}

		Integer allowedChargePower = ess.getAllowedChargePower().orElse(0);
		Integer allowedDischargePower = ess.getAllowedDischargePower().orElse(0);

		int pvProduction = TypeUtils.max(0, dcCharger.getActualPower().orElse(0));

		// Battery charging, substract PV
		long maxAcImport = allowedChargePower - Math.min(allowedChargePower, pvProduction);
		long maxAcExport = allowedDischargePower + pvProduction;

		//
		maxAcImport = Math.min(maxAcImport, maxApparentPower);
		maxAcExport = Math.min(maxAcExport, maxApparentPower);

		this.ess._setMaxAcImport((int) -maxAcImport); // negativ bei Import
		this.ess._setMaxAcExport((int) maxAcExport);
	}

	private void setActivePower(int activePowerWatt) throws OpenemsNamedException {

		var maxApparentPower = this.ess.getMaxApparentPower().get();

		if (maxApparentPower == null) {
			this.ess.logDebug(log, "Max Apparent Power is not available. Cannot set value");
			return;
		}

		// Prozentwert berechnen
		int activePowerPercent = (int) ((double) activePowerWatt / maxApparentPower * 100);

		// Prozentwert auf gültigen Bereich beschränken
		activePowerPercent = Math.max(0, Math.min(100, activePowerPercent));

		// Wert an Register 77 übergeben
		IntegerWriteChannel setActivePowerRegulationChannel = this.ess
				.channel(DeyeSunHybrid.ChannelId.ACTIVE_POWER_REGULATION);
		setActivePowerRegulationChannel.setNextWriteValue(activePowerPercent);

		// Debug Log für Überprüfung
		this.ess.logDebug(log, "Set Active Power: " + activePowerWatt + " W (" + activePowerPercent + "%)");
	}

}
