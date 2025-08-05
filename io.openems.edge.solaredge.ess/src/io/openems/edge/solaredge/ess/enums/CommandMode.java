package io.openems.edge.solaredge.ess.enums;

import io.openems.common.types.OptionsEnum;

public  enum CommandMode implements OptionsEnum {
	
	UNDEFINED(-1, "Undefined"), //
	
	/**
	 * Scenario: System shutdown.
	 *
	 * <p>
	 * Stop working and turn to wait mode
	 */
	STOPPED(0, "Stopped"), //
	
	/**
	 * Scenario: Control the battery to charge PV excess only.
	 * 
	 * <p>
	 * Charge from PV first, before producing power to the AC.
	 * 
	 * <p>
	 * Only PV excess power not going to AC is used for charging the battery. Inverter NominalActivePowerLimit (or the
	 * inverter rated power whichever is lower) sets how much power the inverter is producing to the AC. In this mode,
	 * the battery cannot be discharged. If the PV power is lower than NominalActivePowerLimit the AC production will
	 * be equal to the PV power.
	 */
	CHARGE_BAT_EXCESS(1, "Only PV excess power not going to AC is used for charging the battery. "),
	
	/**
	 * Scenario: Control the battery to keep charging.
	 * 
	 * <p>
	 * Charge from PV first, before producing power to the AC.
	 * 
	 * <p>
	 * The Battery charge has higher priority than AC production. First charge the battery then produce AC.
	 * If StorageRemoteCtrl_ChargeLimit is lower than PV excess power goes to AC according to NominalActivePowerLimit.
	 * If NominalActivePowerLimit is reached and battery StorageRemoteCtrl_ChargeLimit is reached, PV power is curtailed.
	 */
	CHARGE_BAT_FIRST(2, "Charge from PV first, before producing power to the AC."),
	
	/**
	 * Scenario: Force the battery to work at set power value.
	 * 
	 * <p>
	 * Charge from PV+AC according to the max battery power
	 * 
	 * <p>
	 * Charge from both PV and AC with priority on PV power.
	 * If PV production is lower than StorageRemoteCtrl_ChargeLimit, the battery will be charged from AC up to
	 * NominalActivePow-erLimit. In this case AC power = StorageRemoteCtrl_ChargeLimit- PVpower.
	 * If PV power is larger than StorageRemoteCtrl_ChargeLimit the excess PV power will be directed to the AC up to the
	 * Nominal-ActivePowerLimit beyond which the PV is curtailed.
	 */	
	CHARGE_BAT(3, "Charge Bat"),
	
	/**
	 * Scenario: Force the battery to work at set power value.
	 * 
	 * <p>
	 * Maximize export – discharge battery to meet max inverter AC limit.
	 * 
	 * <p>
	 * AC power is maintained to NominalActivePowerLimit, using PV power and/or battery power. If the PV power is not
	 * sufficient, battery power is used to complement AC power up to StorageRemoteCtrl_DishargeLimit. In this mode,
	 * charging excess power will occur if there is more PV than the AC limit.
	 */
	DISCHARGE_BAT(4, "Discharge Bat"),
	
	/**
	 * Scenario: Force the battery to balance grid meter to zero.
	 * 
	 * <p>
	 * Discharge to meet loads consumption. Discharging to the grid is not allowed. This mode requires installation
	 * of a SolarEdge Electricity Meter on the grid connection point.
	 */
	DISCHARGE_BAT_WITH_BALANCE(5, "Discharge Bat+BalanceZero"),
	
	/**
	 * Scenario: Force the battery to reduce electricity purchased from the grid.
	 * 
	 * <p>
	 * Maximize self-consumption.
	 *
	 * <p>
	 * In this mode, the battery is automatically charged and discharged to meet consumption needs and reduce the amount
	 * of electricity purchased from the grid. This mode requires installation of a SolarEdge Electricity Meter, either
	 * on the grid connection point or on the load connection point.
	 */
	AUTO(7, "Auto"); //

	private final int value;
	private final String name;

	private CommandMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}	

