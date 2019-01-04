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
            storage: {
                soc: null,
                isAsymmetric: false,
                hasDC: false,
                chargeActivePower: null, // sum of chargeActivePowerAC and chargeActivePowerDC
                chargeActivePowerAC: null,
                chargeActivePowerACL1: null,
                chargeActivePowerACL2: null,
                chargeActivePowerACL3: null,
                chargeActivePowerDC: null,
                maxChargeActivePower: null,
                dischargeActivePower: null, // sum of dischargeActivePowerAC and dischargeActivePowerDC
                dischargeActivePowerAC: null,
                dischargeActivePowerACL1: null,
                dischargeActivePowerACL2: null,
                dischargeActivePowerACL3: null,
                dischargeActivePowerDC: null,
                maxDischargeActivePower: null
            }, production: {
                isAsymmetric: false,
                hasDC: false,
                powerRatio: null,
                activePower: null, // sum of activePowerAC and activePowerDC
                activePowerAC: null,
                activePowerACL1: null,
                activePowerACL2: null,
                activePowerACL3: null,
                activePowerDC: null, // TODO rename to actualPower
                maxActivePower: null
            }, grid: {
                powerRatio: null,
                buyActivePower: null,
                maxBuyActivePower: null,
                sellActivePower: null,
                maxSellActivePower: null
            }, consumption: {
                powerRatio: null,
                activePower: null
            }
        };

        {
            /*
             * Storage
             * > 0 => Discharge
             * < 0 => Charge
             */
            result.storage.soc = c['_sum/EssSoc'];
            const essActivePower: number = c['_sum/EssActivePower'];
            result.storage.chargeActivePowerAC = essActivePower < 0 ? essActivePower * -1 : 0;
            result.storage.chargeActivePower = result.storage.chargeActivePowerAC; // TODO
            result.storage.dischargeActivePowerAC = essActivePower > 0 ? essActivePower : 0;
            result.storage.dischargeActivePower = result.storage.dischargeActivePowerAC; // TODO
            result.storage.chargeActivePowerDC = c['_sum/ProductionDcActualPower'];
            if (result.storage.chargeActivePowerDC) {
                result.storage.hasDC = true;
            }
        }

        {
            /*
             * Grid
             * > 0 => Buy from grid
             * < 0 => Sell to grid
             */
            const gridActivePower: number = c['_sum/GridActivePower'];
            result.grid.maxBuyActivePower = c['_sum/GridMaxActivePower'];
            result.grid.maxSellActivePower = c['_sum/GridMinActivePower'] * -1;
            if (gridActivePower > 0) {
                result.grid.sellActivePower = 0;
                result.grid.buyActivePower = gridActivePower;
                result.grid.powerRatio = Utils.roundSafely(
                    Utils.divideSafely(result.grid.buyActivePower,
                        Utils.multiplySafely(result.grid.maxBuyActivePower, 100)));
            } else {
                result.grid.sellActivePower = gridActivePower * -1;
                result.grid.buyActivePower = 0;
                result.grid.powerRatio = Utils.roundSafely(
                    Utils.divideSafely(result.grid.buyActivePower,
                        Utils.multiplySafely(result.grid.maxSellActivePower, -100)));
            }
        }

        {
            /*
             * Production
             */
            result.production.activePowerAC = c['_sum/ProductionAcActivePower'];
            result.production.activePower = c['_sum/ProductionActivePower'];
            result.production.maxActivePower = c['_sum/ProductionMaxActivePower'];
            result.production.powerRatio = Utils.roundSafely(
                Utils.divideSafely(result.production.activePower,
                    Utils.multiplySafely(result.production.maxActivePower, 100)));
            result.production.activePowerDC = c['_sum/ProductionDcActualPower'];
            if (result.production.activePowerDC) {
                result.production.hasDC = true;
            }
        }

        {
            /*
             * Consumption
             */
            result.consumption.activePower = c['_sum/ConsumptionActivePower'];
            const consumptionMaxActivePower = c['_sum/ConsumptionMaxActivePower'];
            result.consumption.powerRatio = Utils.roundSafely(
                Utils.divideSafely(result.consumption.activePower,
                    Utils.multiplySafely(consumptionMaxActivePower, 100)));
        }
        return result;
    }

}