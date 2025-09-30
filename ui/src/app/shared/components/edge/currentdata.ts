// @ts-strict-ignore
import { DefaultTypes } from "../../type/defaulttypes";
import { Utils } from "../../utils/utils";

/**
 * @deprecated this class will eventually be dropped, when abstract-widgets are finished and used everywhere.
 */
export class CurrentData {

  public readonly summary: DEFAULT_TYPES.SUMMARY;

  constructor(
    public readonly channel: { [channelAddress: string]: any } = {},
  ) {
    THIS.SUMMARY = THIS.GET_SUMMARY(channel);
  }

  public static calculateAutarchy(buyFromGrid: number, consumptionActivePower: number): number | null {
    if (buyFromGrid != null && consumptionActivePower != null) {
      return MATH.MAX(
        UTILS.OR_ELSE(
          (
            1 - (
              UTILS.DIVIDE_SAFELY(
                UTILS.OR_ELSE(buyFromGrid, 0),
                MATH.MAX(UTILS.OR_ELSE(consumptionActivePower, 0), 0),
              )
            )
          ) * 100, 0,
        ), 0);
    } else {
      return null;
    }
  }

  /**
  * Calculates the powerRatio depending on the available Channels for each version.
  * If version older than '2024.2.2' we use "_sum/EssMaxApparentPower", otherwise we use "_sum/EssMaxDischargePower" & "_sum/EssMinDischargePower" in newer versions.
  *
  * @param maxApparentPower the maxApparentPower
  * @param minDischargePower the minDischargePower
  * @param effectivePower the essActivePower
  * @param result the result
  * @returns the powerRatio
  */
  public static getEssPowerRatio(maxApparentPower: number | null, minDischargePower: number | null, effectivePower: number | null): number {
    if (!effectivePower) {
      return 0;
    }
    return UTILS.OR_ELSE(UTILS.DIVIDE_SAFELY(effectivePower,
      effectivePower > 0
        ? maxApparentPower
        : UTILS.MULTIPLY_SAFELY(minDischargePower, -1)), 0);
  }

  private getSummary(c: { [channelAddress: string]: any }): DEFAULT_TYPES.SUMMARY {
    const result: DEFAULT_TYPES.SUMMARY = {
      system: {
        totalPower: null,
        autarchy: null,
        selfConsumption: null,
        state: null,
      }, storage: {
        soc: null,
        activePowerL1: null,
        activePowerL2: null,
        activePowerL3: null,
        effectiveActivePowerL1: null,
        effectiveActivePowerL2: null,
        effectiveActivePowerL3: null,
        chargeActivePower: null, // sum of chargeActivePowerAc and chargeActivePowerDc
        chargeActivePowerAc: null,
        chargeActivePowerDc: null,
        maxChargeActivePower: null,
        dischargeActivePower: null, // equals dischargeActivePowerAc
        dischargeActivePowerAc: null,
        dischargeActivePowerDc: null,
        maxDischargeActivePower: null,
        powerRatio: null,
        maxApparentPower: null,
        effectivePower: null,
        effectiveChargePower: null,
        effectiveDischargePower: null,
        capacity: null,
      }, production: {
        hasDC: false,
        powerRatio: null,
        activePower: null, // sum of activePowerAC and activePowerDC
        activePowerAc: null,
        activePowerAcL1: null,
        activePowerAcL2: null,
        activePowerAcL3: null,
        activePowerDc: null,
        maxActivePower: null,
      }, grid: {
        gridMode: null,
        powerRatio: null,
        activePowerL1: null,
        activePowerL2: null,
        activePowerL3: null,
        buyActivePower: null,
        maxBuyActivePower: null,
        sellActivePower: null,
        sellActivePowerL1: null,
        sellActivePowerL2: null,
        sellActivePowerL3: null,
        maxSellActivePower: null,
        restrictionMode: null,
      }, consumption: {
        powerRatio: null,
        activePower: null,
        activePowerL1: null,
        activePowerL2: null,
        activePowerL3: null,
      },
    };

    {
      /*
       * Grid
       * > 0 => Buy from grid
       * < 0 => Sell to grid
       */
      const gridActivePower: number = c["_sum/GridActivePower"];
      RESULT.GRID.ACTIVE_POWER_L1 = c["_sum/GridActivePowerL1"];
      RESULT.GRID.ACTIVE_POWER_L2 = c["_sum/GridActivePowerL2"];
      RESULT.GRID.ACTIVE_POWER_L3 = c["_sum/GridActivePowerL3"];
      RESULT.GRID.MAX_BUY_ACTIVE_POWER = c["_sum/GridMaxActivePower"];
      if (!RESULT.GRID.MAX_BUY_ACTIVE_POWER) {
        RESULT.GRID.MAX_BUY_ACTIVE_POWER = 5000;
      }
      RESULT.GRID.MAX_SELL_ACTIVE_POWER = c["_sum/GridMinActivePower"] * -1;
      if (!RESULT.GRID.MAX_SELL_ACTIVE_POWER) {
        RESULT.GRID.MAX_SELL_ACTIVE_POWER = -5000;
      }
      RESULT.GRID.GRID_MODE = c["_sum/GridMode"];
      RESULT.GRID.RESTRICTION_MODE = c["ctrlEssLimiter14a0/RestrictionMode"];
      if (gridActivePower > 0) {
        RESULT.GRID.SELL_ACTIVE_POWER = 0;
        RESULT.GRID.BUY_ACTIVE_POWER = gridActivePower;
        RESULT.GRID.POWER_RATIO = UTILS.OR_ELSE(UTILS.DIVIDE_SAFELY(gridActivePower, RESULT.GRID.MAX_BUY_ACTIVE_POWER), 0);
      } else {
        RESULT.GRID.SELL_ACTIVE_POWER = gridActivePower * -1;
        RESULT.GRID.BUY_ACTIVE_POWER = 0;
        RESULT.GRID.POWER_RATIO = UTILS.OR_ELSE(UTILS.DIVIDE_SAFELY(gridActivePower, RESULT.GRID.MAX_SELL_ACTIVE_POWER), 0);
      }
    }

    {
      /*
       * Production
       */
      RESULT.PRODUCTION.ACTIVE_POWER_AC = c["_sum/ProductionAcActivePower"];
      RESULT.PRODUCTION.ACTIVE_POWER_AC_L1 = c["_sum/ProductionAcActivePowerL1"];
      RESULT.PRODUCTION.ACTIVE_POWER_AC_L2 = c["_sum/ProductionAcActivePowerL2"];
      RESULT.PRODUCTION.ACTIVE_POWER_AC_L3 = c["_sum/ProductionAcActivePowerL3"];
      RESULT.PRODUCTION.ACTIVE_POWER = c["_sum/ProductionActivePower"];
      RESULT.PRODUCTION.MAX_ACTIVE_POWER = c["_sum/ProductionMaxActivePower"];
      if (!RESULT.PRODUCTION.MAX_ACTIVE_POWER) {
        RESULT.PRODUCTION.MAX_ACTIVE_POWER = 10000;
      }
      RESULT.PRODUCTION.POWER_RATIO = UTILS.OR_ELSE(UTILS.DIVIDE_SAFELY(RESULT.PRODUCTION.ACTIVE_POWER, RESULT.PRODUCTION.MAX_ACTIVE_POWER), 0);
      RESULT.PRODUCTION.ACTIVE_POWER_DC = c["_sum/ProductionDcActualPower"];
    }

    {
      /*
       * Storage
       * > 0 => Discharge
       * < 0 => Charge
       */
      RESULT.STORAGE.SOC = c["_sum/EssSoc"];
      RESULT.STORAGE.ACTIVE_POWER_L1 = c["_sum/EssActivePowerL1"];
      RESULT.STORAGE.ACTIVE_POWER_L2 = c["_sum/EssActivePowerL2"];
      RESULT.STORAGE.ACTIVE_POWER_L3 = c["_sum/EssActivePowerL3"];
      RESULT.STORAGE.MAX_APPARENT_POWER = c["_sum/EssMaxApparentPower"];
      RESULT.STORAGE.CAPACITY = c["_sum/EssCapacity"];
      const essActivePower: number = c["_sum/EssActivePower"];

      if (!RESULT.STORAGE.MAX_APPARENT_POWER) {
        RESULT.STORAGE.MAX_APPARENT_POWER = 5000;
      }
      RESULT.STORAGE.CHARGE_ACTIVE_POWER_DC = c["_sum/ProductionDcActualPower"];
      if (essActivePower == null) {
        // keep 'null'
      } else if (essActivePower > 0) {
        RESULT.STORAGE.CHARGE_ACTIVE_POWER_AC = null;
        RESULT.STORAGE.DISCHARGE_ACTIVE_POWER_AC = essActivePower;
        // TODO: should consider DC-Power of ratio
        RESULT.STORAGE.POWER_RATIO = UTILS.OR_ELSE(UTILS.DIVIDE_SAFELY(essActivePower, RESULT.STORAGE.MAX_APPARENT_POWER), 0);
      } else {
        RESULT.STORAGE.CHARGE_ACTIVE_POWER_AC = UTILS.MULTIPLY_SAFELY(essActivePower, -1);
        RESULT.STORAGE.DISCHARGE_ACTIVE_POWER_AC = null;
        RESULT.STORAGE.POWER_RATIO = UTILS.OR_ELSE(UTILS.DIVIDE_SAFELY(essActivePower, RESULT.STORAGE.MAX_APPARENT_POWER), 0);
      }
      RESULT.STORAGE.CHARGE_ACTIVE_POWER = UTILS.ADD_SAFELY(RESULT.STORAGE.CHARGE_ACTIVE_POWER_AC, RESULT.STORAGE.CHARGE_ACTIVE_POWER_DC);
      RESULT.STORAGE.DISCHARGE_ACTIVE_POWER = RESULT.STORAGE.DISCHARGE_ACTIVE_POWER_AC;

      let effectivePower;
      let effectivePowerL1;
      let effectivePowerL2;
      let effectivePowerL3;
      if (RESULT.STORAGE.CHARGE_ACTIVE_POWER_AC == null && RESULT.STORAGE.DISCHARGE_ACTIVE_POWER_AC == null && RESULT.PRODUCTION.ACTIVE_POWER_DC == null) {
        RESULT.STORAGE.EFFECTIVE_POWER = null;
        effectivePower = null;
        effectivePowerL1 = null;
        effectivePowerL2 = null;
        effectivePowerL3 = null;
      } else {
        effectivePowerL1 = UTILS.SUBTRACT_SAFELY(
          RESULT.STORAGE.ACTIVE_POWER_L1, RESULT.PRODUCTION.ACTIVE_POWER_DC / 3);
        RESULT.STORAGE.EFFECTIVE_ACTIVE_POWER_L1 = effectivePowerL1;

        effectivePowerL2 = UTILS.SUBTRACT_SAFELY(
          RESULT.STORAGE.ACTIVE_POWER_L2, RESULT.PRODUCTION.ACTIVE_POWER_DC / 3);
        RESULT.STORAGE.EFFECTIVE_ACTIVE_POWER_L2 = effectivePowerL2;

        effectivePowerL3 = UTILS.SUBTRACT_SAFELY(
          RESULT.STORAGE.ACTIVE_POWER_L3, RESULT.PRODUCTION.ACTIVE_POWER_DC / 3);
        RESULT.STORAGE.EFFECTIVE_ACTIVE_POWER_L3 = effectivePowerL3;

        effectivePower = UTILS.SUBTRACT_SAFELY(
          UTILS.SUBTRACT_SAFELY(
            UTILS.OR_ELSE(RESULT.STORAGE.DISCHARGE_ACTIVE_POWER_AC, 0), RESULT.STORAGE.CHARGE_ACTIVE_POWER_AC,
          ), RESULT.PRODUCTION.ACTIVE_POWER_DC);
        RESULT.STORAGE.EFFECTIVE_POWER = effectivePower;
      }
      if (effectivePower != null) {
        if (effectivePower > 0) {
          RESULT.STORAGE.EFFECTIVE_POWER = effectivePower;
          RESULT.STORAGE.EFFECTIVE_DISCHARGE_POWER = effectivePower;
        } else {
          RESULT.STORAGE.EFFECTIVE_POWER = effectivePower;
          RESULT.STORAGE.EFFECTIVE_CHARGE_POWER = effectivePower * -1;
        }
      }
    }

    {
      /*
       * Consumption
       */
      RESULT.CONSUMPTION.ACTIVE_POWER = c["_sum/ConsumptionActivePower"];
      RESULT.CONSUMPTION.ACTIVE_POWER_L1 = c["_sum/ConsumptionActivePowerL1"];
      RESULT.CONSUMPTION.ACTIVE_POWER_L2 = c["_sum/ConsumptionActivePowerL2"];
      RESULT.CONSUMPTION.ACTIVE_POWER_L3 = c["_sum/ConsumptionActivePowerL3"];
      let consumptionMaxActivePower = c["_sum/ConsumptionMaxActivePower"];
      if (!consumptionMaxActivePower) {
        consumptionMaxActivePower = 10000;
      }
      RESULT.CONSUMPTION.POWER_RATIO = UTILS.OR_ELSE(UTILS.DIVIDE_SAFELY(RESULT.CONSUMPTION.ACTIVE_POWER, consumptionMaxActivePower), 0);
      if (RESULT.CONSUMPTION.POWER_RATIO < 0) {
        RESULT.CONSUMPTION.POWER_RATIO = 0;
      }
    }

    {
      /*
      * Total
      */
      RESULT.SYSTEM.TOTAL_POWER = MATH.MAX(
        // Productions
        RESULT.GRID.BUY_ACTIVE_POWER
        + (RESULT.PRODUCTION.ACTIVE_POWER > 0 ? RESULT.PRODUCTION.ACTIVE_POWER : 0)
        + RESULT.STORAGE.DISCHARGE_ACTIVE_POWER_AC,
        + (RESULT.CONSUMPTION.ACTIVE_POWER < 0 ? RESULT.CONSUMPTION.ACTIVE_POWER * -1 : 0),
        // Consumptions
        RESULT.GRID.SELL_ACTIVE_POWER
        + (RESULT.PRODUCTION.ACTIVE_POWER < 0 ? RESULT.PRODUCTION.ACTIVE_POWER * -1 : 0)
        + RESULT.STORAGE.CHARGE_ACTIVE_POWER_AC,
        + (RESULT.CONSUMPTION.ACTIVE_POWER > 0 ? RESULT.CONSUMPTION.ACTIVE_POWER : 0),
      );
      RESULT.SYSTEM.AUTARCHY = CURRENT_DATA.CALCULATE_AUTARCHY(RESULT.GRID.BUY_ACTIVE_POWER, RESULT.CONSUMPTION.ACTIVE_POWER);
      RESULT.SYSTEM.SELF_CONSUMPTION = UTILS.CALCULATE_SELF_CONSUMPTION(RESULT.GRID.SELL_ACTIVE_POWER, RESULT.PRODUCTION.ACTIVE_POWER);
      // State
      RESULT.SYSTEM.STATE = c["_sum/State"];
    }
    return result;
  }

}
