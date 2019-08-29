import { DefaultTypes } from "../service/defaulttypes";
import { Utils } from "../service/utils";

export class CurrentData {

    public readonly summary: DefaultTypes.Summary;

    constructor(
        public readonly channel: { [channelAddress: string]: any } = {}
    ) {
        this.summary = this.getSummary(channel);
    }

    private getSummary(c: { [channelAddress: string]: any }): DefaultTypes.Summary {
        let result: DefaultTypes.Summary = {
            system: {
                totalPower: null,
                autarchy: null,
                selfConsumption: null
            }, storage: {
                soc: null,
                activePowerL1: null,
                activePowerL2: null,
                activePowerL3: null,
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
                maxActivePower: null
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
                maxSellActivePower: null
            }, consumption: {
                powerRatio: null,
                activePower: null,
                activePowerL1: null,
                activePowerL2: null,
                activePowerL3: null
            }
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
            result.storage.capacity = c['_sum/EssCapacity'];
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
            if (result.storage.chargeActivePowerAc == null && result.storage.dischargeActivePowerAc == null && result.production.activePowerDc == null) {
                effectivePower = null;
            } else {
                effectivePower = Utils.subtractSafely(
                    Utils.subtractSafely(
                        Utils.orElse(result.storage.dischargeActivePowerAc, 0), result.storage.chargeActivePowerAc
                    ), result.production.activePowerDc);
            }
            if (effectivePower != null) {
                if (effectivePower > 0) {
                    result.storage.effectiveDischargePower = effectivePower;
                } else {
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
                + (result.consumption.activePower > 0 ? result.consumption.activePower : 0)
            );
            result.system.autarchy = (1 - (Utils.orElse(result.grid.buyActivePower, 0) / result.consumption.activePower)) * 100;
            result.system.selfConsumption = (1 - (Utils.orElse(result.grid.sellActivePower, 0) / (Utils.orElse(result.production.activePower, 0) + Utils.orElse(result.storage.dischargeActivePower, 0)))) * 100;
            if (result.system.autarchy < 0 || isNaN(result.system.autarchy)) {
                result.system.autarchy = 0;
            }
            if (result.system.selfConsumption < 0 || isNaN(result.system.selfConsumption)) {
                result.system.selfConsumption = 0;
            }
        }
        return result;
    }

}