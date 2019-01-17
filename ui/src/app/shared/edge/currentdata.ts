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
            }, storage: {
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
                maxDischargeActivePower: null,
                powerRatio: null,
                maxApparentPower: null
            }, production: {
                isAsymmetric: false,
                hasDC: false,
                powerRatio: null,
                activePower: null, // sum of activePowerAC and activePowerDC
                activePowerAC: null,
                activePowerACL1: null,
                activePowerACL2: null,
                activePowerACL3: null,
                activePowerDC: null,
                maxActivePower: null
            }, grid: {
                gridMode: null,
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
            result.storage.maxApparentPower = c['_sum/MaxApparentPower'];
            if (!result.storage.maxApparentPower) {
                result.storage.maxApparentPower = 5000;
            }
            result.storage.chargeActivePowerDC = c['_sum/ProductionDcActualPower'];
            if (result.storage.chargeActivePowerDC) {
                result.storage.hasDC = true;
            }
            if (essActivePower > 0) {
                result.storage.chargeActivePower = 0;
                result.storage.dischargeActivePower = essActivePower;
                // TODO: should consider DC-Power of ratio
                result.storage.powerRatio = Utils.orElse(Utils.divideSafely(essActivePower, result.storage.maxApparentPower), 0);
            } else {
                result.storage.chargeActivePower = Utils.multiplySafely(essActivePower, -1);
                result.storage.dischargeActivePower = 0;
                result.storage.powerRatio = Utils.orElse(Utils.divideSafely(essActivePower, result.storage.maxApparentPower), 0);
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
            result.production.activePowerAC = c['_sum/ProductionAcActivePower'];
            result.production.activePower = c['_sum/ProductionActivePower'];
            result.production.maxActivePower = c['_sum/ProductionMaxActivePower'];
            if (!result.production.maxActivePower) {
                result.production.maxActivePower = 10000;
            }
            result.production.powerRatio = Utils.orElse(Utils.divideSafely(result.production.activePower, result.production.maxActivePower), 0);
            result.production.activePowerDC = c['_sum/ProductionDcActualPower'];
            if (result.production.activePowerDC) {
                result.production.hasDC = true;
            }
        }

        {
            /*
             * Consumption
             */
            result.consumption.activePower = Utils.orElse(c['_sum/ConsumptionActivePower'], 0);
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
            result.system.totalPower = Utils.orElse(Utils.addSafely(result.grid.buyActivePower, Utils.addSafely(result.production.activePower, result.storage.dischargeActivePower)), 0);
            if (result.system.totalPower < 0) {
                result.system.totalPower = 0;
            }
        }
        return result;
    }

}