import { DefaultTypes } from "../service/defaulttypes";
import { Utils } from "../service/utils";

/**
 * @deprecated this class will eventually be dropped, when abstract-widgets are finished and used everywhere.
 */
export class CurrentData {

  public readonly summary: DefaultTypes.Summary;

  constructor(
    public readonly channel: { [channelAddress: string]: any } = {},
  ) {
    this.summary = this.getSummary(channel);
  }

  private getSummary(c: { [channelAddress: string]: any }): DefaultTypes.Summary {
    const result: DefaultTypes.Summary = {
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
      const gridActivePower: number = c['_sum/GridActivePower'];
      result.grid.activePowerL1 = c['_sum/GridActivePowerL1'];
      result.grid.activePowerL2 = c['_sum/GridActivePowerL2'];
      result.grid.activePowerL3 = c['_sum/GridActivePowerL3'];
      result.grid.maxBuyActivePower = c['_sum/GridMaxActivePower'];
      if (!result.grid.maxBuyActivePower) {
        result.grid.maxBuyActivePower = 5000;
      }
      result.grid.maxSellActivePower = c['_sum/GridMinActivePower'] * -1;
      if (!result.grid.maxSellActivePower) {
        result.grid.maxSellActivePower = -5000;
      }
      result.grid.gridMode = c['_sum/GridMode'];
      if (gridActivePower > 0) {
        result.grid.sellActivePower = 0;
        result.grid.buyActivePower = gridActivePower;
        result.grid.powerRatio = Utils.orElse(Utils.divideSafely(gridActivePower, result.grid.maxBuyActivePower), 0);
      } else {
        result.grid.sellActivePower = gridActivePower * -1;
        result.grid.buyActivePower = 0;
        result.grid.powerRatio = Utils.orElse(Utils.divideSafely(gridActivePower, result.grid.maxSellActivePower), 0);
      }
    }

    {
      /*
       * Production
       */
      result.production.activePowerAc = c['_sum/ProductionAcActivePower'];
      result.production.activePowerAcL1 = c['_sum/ProductionAcActivePowerL1'];
      result.production.activePowerAcL2 = c['_sum/ProductionAcActivePowerL2'];
      result.production.activePowerAcL3 = c['_sum/ProductionAcActivePowerL3'];
      result.production.activePower = c['_sum/ProductionActivePower'];
      result.production.maxActivePower = c['_sum/ProductionMaxActivePower'];
      if (!result.production.maxActivePower) {
        result.production.maxActivePower = 10000;
      }
      result.production.powerRatio = Utils.orElse(Utils.divideSafely(result.production.activePower, result.production.maxActivePower), 0);
      result.production.activePowerDc = c['_sum/ProductionDcActualPower'];
    }

    {
      /*
       * Storage
       * > 0 => Discharge
       * < 0 => Charge
       */
      result.storage.soc = c['_sum/EssSoc'];
      result.storage.activePowerL1 = c['_sum/EssActivePowerL1'];
      result.storage.activePowerL2 = c['_sum/EssActivePowerL2'];
      result.storage.activePowerL3 = c['_sum/EssActivePowerL3'];
      result.storage.maxApparentPower = c['_sum/EssMaxApparentPower'];
      const essActivePower: number = c['_sum/EssActivePower'];

      if (!result.storage.maxApparentPower) {
        result.storage.maxApparentPower = 5000;
      }
      result.storage.chargeActivePowerDc = c['_sum/ProductionDcActualPower'];
      if (essActivePower == null) {
        // keep 'null'
      } else if (essActivePower > 0) {
        result.storage.chargeActivePowerAc = null;
        result.storage.dischargeActivePowerAc = essActivePower;
        // TODO: should consider DC-Power of ratio
        result.storage.powerRatio = Utils.orElse(Utils.divideSafely(essActivePower, result.storage.maxApparentPower), 0);
      } else {
        result.storage.chargeActivePowerAc = Utils.multiplySafely(essActivePower, -1);
        result.storage.dischargeActivePowerAc = null;
        result.storage.powerRatio = Utils.orElse(Utils.divideSafely(essActivePower, result.storage.maxApparentPower), 0);
      }
      result.storage.chargeActivePower = Utils.addSafely(result.storage.chargeActivePowerAc, result.storage.chargeActivePowerDc);
      result.storage.dischargeActivePower = result.storage.dischargeActivePowerAc;

      let effectivePower;
      let effectivePowerL1;
      let effectivePowerL2;
      let effectivePowerL3;
      if (result.storage.chargeActivePowerAc == null && result.storage.dischargeActivePowerAc == null && result.production.activePowerDc == null) {
        result.storage.effectivePower = null;
        effectivePower = null;
        effectivePowerL1 = null;
        effectivePowerL2 = null;
        effectivePowerL3 = null;
      } else {
        effectivePowerL1 = Utils.subtractSafely(
          result.storage.activePowerL1, result.production.activePowerDc / 3);
        result.storage.effectiveActivePowerL1 = effectivePowerL1;

        effectivePowerL2 = Utils.subtractSafely(
          result.storage.activePowerL2, result.production.activePowerDc / 3);
        result.storage.effectiveActivePowerL2 = effectivePowerL2;

        effectivePowerL3 = Utils.subtractSafely(
          result.storage.activePowerL3, result.production.activePowerDc / 3);
        result.storage.effectiveActivePowerL3 = effectivePowerL3;

        effectivePower = Utils.subtractSafely(
          Utils.subtractSafely(
            Utils.orElse(result.storage.dischargeActivePowerAc, 0), result.storage.chargeActivePowerAc,
          ), result.production.activePowerDc);
        result.storage.effectivePower = effectivePower;
      }
      if (effectivePower != null) {
        if (effectivePower > 0) {
          result.storage.effectivePower = effectivePower;
          result.storage.effectiveDischargePower = effectivePower;
        } else {
          result.storage.effectivePower = effectivePower;
          result.storage.effectiveChargePower = effectivePower * -1;
        }
      }
    }

    {
      /*
       * Consumption
       */
      result.consumption.activePower = c['_sum/ConsumptionActivePower'];
      result.consumption.activePowerL1 = c['_sum/ConsumptionActivePowerL1'];
      result.consumption.activePowerL2 = c['_sum/ConsumptionActivePowerL2'];
      result.consumption.activePowerL3 = c['_sum/ConsumptionActivePowerL3'];
      let consumptionMaxActivePower = c['_sum/ConsumptionMaxActivePower'];
      if (!consumptionMaxActivePower) {
        consumptionMaxActivePower = 10000;
      }
      result.consumption.powerRatio = Utils.orElse(Utils.divideSafely(result.consumption.activePower, consumptionMaxActivePower), 0);
      if (result.consumption.powerRatio < 0) {
        result.consumption.powerRatio = 0;
      }
    }

    {
      /*
      * Total
      */
      result.system.totalPower = Math.max(
        // Productions
        result.grid.buyActivePower
        + (result.production.activePower > 0 ? result.production.activePower : 0)
        + result.storage.dischargeActivePowerAc,
        + (result.consumption.activePower < 0 ? result.consumption.activePower * -1 : 0),
        // Consumptions
        result.grid.sellActivePower
        + (result.production.activePower < 0 ? result.production.activePower * -1 : 0)
        + result.storage.chargeActivePowerAc,
        + (result.consumption.activePower > 0 ? result.consumption.activePower : 0),
      );
      result.system.autarchy = CurrentData.calculateAutarchy(result.grid.buyActivePower, result.consumption.activePower);
      result.system.selfConsumption = Utils.calculateSelfConsumption(result.grid.sellActivePower, result.production.activePower);
      // State
      result.system.state = c['_sum/State'];
    }
    return result;
  }
  public static calculateAutarchy(buyFromGrid: number, consumptionActivePower: number): number | null {
    if (buyFromGrid != null && consumptionActivePower != null) {
      return Math.max(
        Utils.orElse(
          (
            1 - (
              Utils.divideSafely(
                Utils.orElse(buyFromGrid, 0),
                Math.max(Utils.orElse(consumptionActivePower, 0), 0),
              )
            )
          ) * 100, 0,
        ), 0);
    } else {
      return null;
    }
  }

}
