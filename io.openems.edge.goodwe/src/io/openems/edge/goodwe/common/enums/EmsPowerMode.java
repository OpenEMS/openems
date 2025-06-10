package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

/**
 * See table 8-16 EMS Power Mode.
 *
 * <p>
 * Note:
 *
 * <ul>
 * <li>for low-priority energy sources, when the battery charging power is
 * limited or the rated output power of the inverter is limited, the load shall
 * be reduced first.
 * <li>Xmax represents the upper limit of the power control value, and the
 * actual power will be adjusted according to the working condition.
 * <li>Xset represents the target value of power control, and the actual power
 * must reach the set value.
 * </ul>
 */
public enum EmsPowerMode implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //

	/**
	 * Scenario: System shutdown.
	 *
	 * <p>
	 * Stop working and turn to wait mode
	 */
	STOPPED(0xFF, "Stopped"),

	/**
	 * Scenario: Self-use.
	 *
	 * <p>
	 * PBattery = PInv - Pmeter - Ppv (Discharge/Charge)
	 *
	 * <p>
	 * The battery power is controlled by the meter power when the meter
	 * communication is normal.
	 */
	AUTO(0x01, "Auto"), //

	/**
	 * Scenario: Control the battery to keep charging.
	 *
	 * <p>
	 * PBattery = Xmax + PV (Charge)
	 *
	 * <p>
	 * Xmax is to allow the power to be taken from the grid, and PV power is
	 * preferred. When set to 0, only PV power is used. Charging power will be
	 * limited by charging current limit.
	 *
	 * <p>
	 * Interpretation: Charge Battery from PV (high priority) or Grid (low
	 * priority); EmsPowerSet = negative ESS ActivePower (if possible because of
	 * PV).
	 *
	 * <ul>
	 * <li>Grid: low priority
	 * <li>PV: high priority
	 * <li>Battery: Charge Mode
	 * <li>The control object is 'Grid'
	 * </ul>
	 */
	CHARGE_PV(0x02, "Charge PV"), //

	/**
	 * Scenario: Control the battery to keep discharging.
	 *
	 * <p>
	 * PBattery = Xmax (Discharge)
	 *
	 * <p>
	 * Xmax is the allowable discharge power of the battery. When the power fed into
	 * the grid is limited, PV power will be used first.
	 *
	 * <p>
	 * Interpretation: ESS ActivePower = PV power + EmsPowerSet (i.e. battery
	 * discharge); useful for surplus feed-to-grid.
	 *
	 * <ul>
	 * <li>PV: high priority
	 * <li>Battery: low priority
	 * <li>Grid: Energy Out Mode
	 * <li>The control object is 'Battery'
	 * </ul>
	 */
	DISCHARGE_PV(0x03, "Discharge PV"), //

	/**
	 * Scenario: The inverter is used as a unit for power grid energy scheduling.
	 *
	 * <p>
	 * PBattery = Xset + PV (Charge)
	 *
	 * <p>
	 * Xset refers to the power purchased from the power grid. The power purchased
	 * from the grid is preferred. If the PV power is too large, the MPPT power will
	 * be limited. (grid side load is not considered)
	 *
	 * <p>
	 * Interpretation: Charge Battery from Grid (high priority) or PV (low
	 * priority); EmsPowerSet = negative ESS ActivePower; as long as
	 * BMS_CHARGE_MAX_CURRENT is > 0, no AC-Power is exported; when
	 * BMS_CHARGE_MAX_CURRENT == 0, PV surplus feed in starts!
	 *
	 * <ul>
	 * <li>Grid: high priority
	 * <li>PV: low priority
	 * <li>Battery: Charge Mode
	 * <li>The control object is 'Grid'
	 * </ul>
	 */
	IMPORT_AC(0x04, "Import AC"), //

	/**
	 * Scenario: The inverter is used as a unit for power grid energy scheduling.
	 *
	 * <p>
	 * PBattery = Xset (Discharge)
	 *
	 * <p>
	 * Xset is to sell power to the grid. PV power is preferred. When PV energy is
	 * insufficient, the battery will discharge. PV power will be limited by x.
	 * (grid side load is not considered)
	 *
	 * <p>
	 * Interpretation: EmsPowerSet = positive ESS ActivePower. But PV will be
	 * limited, i.e. remaining power is not used to charge battery.
	 *
	 * <ul>
	 * <li>PV: high priority
	 * <li>Battery: low priority
	 * <li>Grid: Energy Out Mode
	 * <li>The control object is 'Grid'
	 * </ul>
	 */
	EXPORT_AC(0x05, "Export AC"), //

	/**
	 * Scenario: Off-grid reservation mode.
	 *
	 * <p>
	 * PBattery = PV (Charge)
	 *
	 * <p>
	 * In on-grid mode, the battery is continuously charged, and only PV power (AC
	 * Couple model takes 10% of the rated power of the power grid) is used. The
	 * battery can only discharge in off-grid mode.
	 */
	CONSERVE(0x06, "Conserve"), //

	/**
	 * Scenario: Off-Grid Mode.
	 *
	 * <p>
	 * PBattery = Pbackup - Ppv (Charge/Discharge)
	 *
	 * <p>
	 * Forced off-grid operation.
	 */
	OFF_GRID(0x07, "Off-Grid"), //

	/**
	 * Scenario: The inverter is used as a unit for power grid energy scheduling.
	 *
	 * <p>
	 * PBattery = 0 (Standby)
	 *
	 * <p>
	 * The battery does not charge and discharge
	 */
	BATTERY_STANDBY(0x08, "Battery Standby"), //

	/**
	 * Scenario: Regional energy management.
	 *
	 * <p>
	 * PBattery = PInv - (Pmeter + Xset) - Ppv (Charge/Discharge)
	 *
	 * <p>
	 * When the meter communication is normal, the power purchased from the power
	 * grid is controlled as Xset. When the PV power is too large, the MPPT power
	 * will be limited. When the load is too large, the battery will discharge.
	 *
	 * <p>
	 * Interpretation: Control power at the point of common coupling.
	 *
	 * <ul>
	 * <li>Grid: high priority
	 * <li>PV: low priority
	 * <li>Battery: Energy In and Out Mode
	 * <li>The control object is 'Grid'
	 * </ul>
	 */
	BUY_POWER(0x09, "Buy Power"), //

	/**
	 * Scenario: Regional energy management.
	 *
	 * <p>
	 * PBattery = PInv - (Pmeter - Xset) - Ppv (Charge/Discharge)
	 *
	 * <p>
	 * When the communication of electricity meter is normal, the power sold from
	 * the power grid is controlled as Xset, PV power is preferred, and the battery
	 * discharges when PV energy is insufficient.PV power will be limited by Xset.
	 *
	 * <p>
	 * Interpretation: Control power at the point of common coupling.
	 *
	 * <ul>
	 * <li>PV: high priority
	 * <li>Battery: low priority
	 * <li>Grid: Energy Out Mode
	 * <li>The control object is 'Grid'
	 * </ul>
	 */
	SELL_POWER(0x0A, "Sell Power"), //

	/**
	 * Scenario: Force the battery to work at set power value.
	 *
	 * <p>
	 * PBattery = Xset (Charge)
	 *
	 * <p>
	 * Xset is the charging power of the battery. PV power is preferred. When PV
	 * power is insufficient, it will buy power from the power grid. The charging
	 * power is also affected by the charging current limit.
	 *
	 * <p>
	 * Interpretation: Charge Battery from PV (high priority) or Grid (low
	 * priority); priorities are inverted compared to IMPORT_AC.
	 *
	 * <ul>
	 * <li>PV: high priority
	 * <li>Grid: low priority
	 * <li>Battery: Energy In Mode
	 * <li>The control object is 'Battery'
	 * </ul>
	 */
	CHARGE_BAT(0x0B, "Charge Bat"), //

	/**
	 * Scenario: Force the battery to work at set power value.
	 *
	 * <p>
	 * PBattery = Xset (Discharge)
	 *
	 * <p>
	 * Xset is the discharge power of the battery, and the battery discharge has
	 * priority. If the PV power is too large, MPPT will be limited. Discharge power
	 * is also affected by discharge current limit.
	 *
	 * <p>
	 * Interpretation: ???
	 *
	 * <ul>
	 * <li>PV: low priority
	 * <li>Battery: high priority
	 * <li>Grid: Energy In Mode
	 * <li>The control object is 'Battery'
	 * </ul>
	 */
	DISCHARGE_BAT(0x0C, "Discharge Bat"); //

	private final int value;
	private final String option;

	private EmsPowerMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}