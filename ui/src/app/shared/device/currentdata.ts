import { DefaultTypes } from '../service/defaulttypes';
import { ConfigImpl } from './config';
import { Utils } from '../service/utils';

export class CurrentDataAndSummary {
    public readonly summary: DefaultTypes.Summary;

    constructor(public data: DefaultTypes.Data, config: ConfigImpl) {
        this.summary = this.calculateSummary(data, config);
    }

    private calculateSummary(currentData: DefaultTypes.Data, config: ConfigImpl): DefaultTypes.Summary {
        let result: DefaultTypes.Summary = {
            storage: {
                soc: null,
                chargeActivePower: null, // sum of chargeActivePowerAC and chargeActivePowerDC
                chargeActivePowerAC: null,
                chargeActivePowerDC: null,
                maxChargeActivePower: null,
                dischargeActivePower: null, // sum of dischargeActivePowerAC and dischargeActivePowerDC
                dischargeActivePowerAC: null,
                dischargeActivePowerDC: null,
                maxDischargeActivePower: null
            }, production: {
                powerRatio: null,
                activePower: null, // sum of activePowerAC and activePowerDC
                activePowerAC: null,
                activePowerDC: null,
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
            let soc = null;
            let activePowerAC = null;
            let activePowerDC = null;
            let countSoc = 0;
            for (let thing of config.storageThings) {
                if (thing in currentData) {
                    let essData = currentData[thing];
                    if ("Soc" in essData) {
                        soc = Utils.addSafely(soc, essData.Soc);
                        countSoc += 1;
                    }
                    activePowerAC = Utils.addSafely(activePowerAC, this.getActivePower(essData));
                }
            }
            for (let thing of config.chargers) {
                if (thing in currentData) {
                    let essData = currentData[thing];
                    activePowerDC = Utils.subtractSafely(activePowerDC, essData.ActualPower);
                }
            }
            result.storage.soc = Utils.divideSafely(soc, countSoc);
            if (activePowerAC != null) {
                if (activePowerAC > 0) {
                    result.storage.chargeActivePowerAC = 0;
                    result.storage.dischargeActivePowerAC = activePowerAC;
                } else {
                    result.storage.chargeActivePowerAC = activePowerAC * -1;
                    result.storage.dischargeActivePowerAC = 0;
                }
            }
            if (activePowerDC != null) {
                if (activePowerDC > 0) {
                    result.storage.chargeActivePowerDC = 0;
                    result.storage.dischargeActivePowerDC = activePowerDC;
                } else {
                    result.storage.chargeActivePowerDC = activePowerDC * -1;
                    result.storage.dischargeActivePowerDC = 0;
                }
            }
            result.storage.chargeActivePower = Utils.addSafely(result.storage.chargeActivePowerAC, result.storage.chargeActivePowerDC);
            result.storage.dischargeActivePower = Utils.addSafely(result.storage.dischargeActivePowerAC, result.storage.dischargeActivePowerDC);
        }

        {
            /*
             * Grid
             * > 0 => Buy from grid
             * < 0 => Sell to grid
             */
            let activePower = null;
            let ratio = 0;
            let maxSell = 0;
            let maxBuy = 0;
            for (let thing of config.gridMeters) {
                let meterData = currentData[thing];
                let meterConfig = config.things[thing];
                activePower = Utils.addSafely(activePower, this.getActivePower(meterData));
                if ("maxActivePower" in meterConfig) {
                    maxBuy += meterConfig.maxActivePower;
                }
                if ("minActivePower" in meterConfig) {
                    maxSell += meterConfig.minActivePower;
                }
            }
            // set GridBuy and GridSell
            result.grid.maxSellActivePower = maxSell * -1;
            result.grid.maxBuyActivePower = maxBuy;
            if (activePower != null) {
                if (activePower > 0) {
                    result.grid.sellActivePower = 0;
                    result.grid.buyActivePower = activePower;
                    ratio = result.grid.buyActivePower / maxSell * 100;
                } else {
                    result.grid.sellActivePower = activePower * -1;
                    result.grid.buyActivePower = 0;
                    ratio = result.grid.sellActivePower / maxSell * -100;
                }
            }
            result.grid.powerRatio = ratio;
        }

        {
            /*
             * Production
             */
            let powerRatio = 0;
            let activePowerAC = null;
            let activePowerDC = null;
            let maxActivePower = 0;
            for (let thing of config.productionMeters) {
                let meterData = currentData[thing];
                let meterConfig = config.things[thing];
                activePowerAC = Utils.addSafely(activePowerAC, this.getActivePower(meterData));
                if ("ActualPower" in meterData && meterData.ActualPower != null) {
                    activePowerDC = Utils.addSafely(activePowerDC, meterData.ActualPower);
                }
                if ("maxActivePower" in meterConfig) {
                    maxActivePower += meterConfig.maxActivePower;
                }
                if ("maxActualPower" in meterConfig) {
                    maxActivePower += meterConfig.maxActualPower;
                }
            }

            // correct negative production
            if (activePowerAC < 0) {
                console.warn("negative production? ", config.productionMeters, activePowerAC)
                // TODO activePowerAC = 0;
            }
            if (maxActivePower < 0) { maxActivePower = 0; }

            if (maxActivePower == 0) {
                powerRatio = 100;
            } else {
                let activePowerACDC = Utils.addSafely(activePowerAC, activePowerDC);
                powerRatio = Utils.divideSafely(activePowerACDC, (maxActivePower / 100));
            }

            result.production.powerRatio = powerRatio;
            result.production.activePowerAC = activePowerAC;
            result.production.activePowerDC = activePowerDC;
            result.production.activePower = Utils.addSafely(activePowerAC, activePowerDC);
            result.production.maxActivePower = maxActivePower;
        }

        {
            /*
             * Consumption
             */
            // Consumption = GridBuy + Production + ESS-Discharge - GridSell - ESS-Charge
            let minus = Utils.addSafely(result.grid.sellActivePower, result.storage.chargeActivePowerAC);
            let plus = Utils.addSafely(Utils.addSafely(result.grid.buyActivePower, result.production.activePowerAC), result.storage.dischargeActivePowerAC);
            let activePower = Utils.subtractSafely(plus, minus);

            let maxActivePower = result.grid.maxBuyActivePower - result.grid.maxSellActivePower //
                + result.production.maxActivePower //
                + result.storage.maxChargeActivePower - result.storage.maxDischargeActivePower;
            result.consumption.powerRatio = Utils.divideSafely(activePower, (maxActivePower / 100));
            result.consumption.activePower = activePower;
        }
        return result;
    }

    private getActivePower(o: any): number {
        if ("ActivePowerL1" in o && o.ActivePowerL1 != null && "ActivePowerL2" in o && o.ActivePowerL2 != null && "ActivePowerL3" in o && o.ActivePowerL3 != null) {
            return o.ActivePowerL1 + o.ActivePowerL2 + o.ActivePowerL3;
        } else if ("ActivePower" in o && o.ActivePower != null) {
            return o.ActivePower;
        } else {
            return null;
        }
    }
}