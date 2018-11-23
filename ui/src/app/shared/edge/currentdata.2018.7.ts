import { DefaultTypes } from '../service/defaulttypes';
import { Utils } from '../service/utils';
import { Edge } from './edge';
import { CurrentDataAndSummary } from './currentdata';
import { ConfigImpl_2018_7 } from './config.2018.7';

export class CurrentDataAndSummary_2018_7 extends CurrentDataAndSummary {

    constructor(private edge: Edge, data: DefaultTypes.Data, config: ConfigImpl_2018_7) {
        super(data);
        this.summary = this.calculateSummary(data, config);
    }

    private calculateSummary(currentData: DefaultTypes.Data, config: ConfigImpl_2018_7): DefaultTypes.Summary {
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
            }, evcs: {
                actualPower: null
            }
        };

        {
            /*
            * EVCS
            * > 0 => Charge
            * = 0 => No Charge
            */
            let actualPower = 0;
            for (let thing of config.evcsDevices) {
                if (thing in currentData) {
                    let evcsData = currentData[thing];
                    actualPower = this.getActualPower(evcsData);
                    if ("ActualPower" in evcsData) {
                        actualPower = Utils.addSafely(actualPower, evcsData.actualPower)
                    }
                }
            }
            if (actualPower) {
                result.evcs.actualPower = actualPower;
            }
        }

        {
            /*
             * Storage
             * > 0 => Discharge
             * < 0 => Charge
             */
            let soc = null;
            let isAsymmetric = false;
            let activePowerAC = null;
            let activePowerACL1 = null;
            let activePowerACL2 = null;
            let activePowerACL3 = null;
            let activePowerDC = null;
            let countSoc = 0;
            for (let thing of config.esss) {
                if (thing in currentData) {
                    let essData = currentData[thing];
                    if ("Soc" in essData) {
                        soc = Utils.addSafely(soc, essData.Soc);
                        countSoc += 1;
                    }
                    let thisActivePowerAC = this.getActivePower(essData);
                    let thisActivePowerACL = Utils.divideSafely(thisActivePowerAC, 3);
                    activePowerAC = Utils.addSafely(activePowerAC, thisActivePowerAC);
                    if ("ActivePowerL1" in essData) {
                        isAsymmetric = true;
                        activePowerACL1 = Utils.addSafely(activePowerACL1, essData.ActivePowerL1);
                    } else {
                        activePowerACL1 = Utils.addSafely(activePowerACL1, thisActivePowerACL);
                    }
                    if ("ActivePowerL2" in essData) {
                        isAsymmetric = true;
                        activePowerACL2 = Utils.addSafely(activePowerACL2, essData.ActivePowerL2);
                    } else {
                        activePowerACL2 = Utils.addSafely(activePowerACL2, thisActivePowerACL);
                    }
                    if ("ActivePowerL3" in essData) {
                        isAsymmetric = true;
                        activePowerACL3 = Utils.addSafely(activePowerACL3, essData.ActivePowerL3);
                    } else {
                        activePowerACL3 = Utils.addSafely(activePowerACL3, thisActivePowerACL);
                    }
                }
            }
            for (let thing of config.chargers) {
                if (thing in currentData) {
                    let essData = currentData[thing];
                    activePowerDC = Utils.subtractSafely(activePowerDC, essData.ActualPower);
                }
            }
            result.storage.soc = Utils.divideSafely(soc, countSoc);
            result.storage.isAsymmetric = isAsymmetric;
            result.storage.hasDC = config.chargers.length > 0;
            if (result.storage.soc > 100 || result.storage.soc < 0) {
                result.storage.soc = null;
            }
            if (isAsymmetric) {
                activePowerAC = Utils.addSafely(Utils.addSafely(activePowerACL1, activePowerACL2), activePowerACL3)
            }
            if (activePowerAC != null) {
                if (activePowerAC > 0) {
                    result.storage.chargeActivePowerAC = 0;
                    result.storage.dischargeActivePowerAC = activePowerAC;
                } else {
                    result.storage.chargeActivePowerAC = activePowerAC * -1;
                    result.storage.dischargeActivePowerAC = 0;
                }
            }
            if (activePowerACL1 != null) {
                if (activePowerACL1 > 0) {
                    result.storage.chargeActivePowerACL1 = 0;
                    result.storage.dischargeActivePowerACL1 = activePowerACL1;
                } else {
                    result.storage.chargeActivePowerACL1 = activePowerACL1 * -1;
                    result.storage.dischargeActivePowerACL1 = 0;
                }
            }
            if (activePowerACL2 != null) {
                if (activePowerACL2 > 0) {
                    result.storage.chargeActivePowerACL2 = 0;
                    result.storage.dischargeActivePowerACL2 = activePowerACL2;
                } else {
                    result.storage.chargeActivePowerACL2 = activePowerACL2 * -1;
                    result.storage.dischargeActivePowerACL2 = 0;
                }
            }
            if (activePowerACL3 != null) {
                if (activePowerACL3 > 0) {
                    result.storage.chargeActivePowerACL3 = 0;
                    result.storage.dischargeActivePowerACL3 = activePowerACL3;
                } else {
                    result.storage.chargeActivePowerACL3 = activePowerACL3 * -1;
                    result.storage.dischargeActivePowerACL3 = 0;
                }
            }
            if (activePowerDC != null) {
                if (activePowerDC > 0) {
                    result.storage.chargeActivePowerDC = activePowerDC;
                    result.storage.dischargeActivePowerDC = 0;
                } else {
                    result.storage.chargeActivePowerDC = 0;
                    result.storage.dischargeActivePowerDC = activePowerDC * -1;
                }
            }
            let activePower = Utils.subtractSafely(
                Utils.addSafely(result.storage.chargeActivePowerAC, result.storage.chargeActivePowerDC),
                Utils.addSafely(result.storage.dischargeActivePowerAC, result.storage.dischargeActivePowerDC));
            if (activePower != null) {
                if (activePower > 0) {
                    result.storage.chargeActivePower = activePower;
                    result.storage.dischargeActivePower = 0;
                } else {
                    result.storage.chargeActivePower = 0;
                    result.storage.dischargeActivePower = activePower * -1;
                }
            }
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
            let isAsymmetric = false;
            let activePowerAC = null;
            let activePowerACL1 = null;
            let activePowerACL2 = null;
            let activePowerACL3 = null;
            let activePowerDC = null;
            let maxActivePower = 0;
            for (let thing of config.productionMeters) {
                let meterData = currentData[thing];
                let meterConfig = config.things[thing];
                let thisActivePowerAC = this.getActivePower(meterData);
                let thisActivePowerACL = Utils.divideSafely(thisActivePowerAC, 3);
                activePowerAC = Utils.addSafely(activePowerAC, thisActivePowerAC);
                if ("ActivePowerL1" in meterData) {
                    isAsymmetric = true;
                    activePowerACL1 = Utils.addSafely(activePowerACL1, meterData.ActivePowerL1);
                } else {
                    activePowerACL1 = Utils.addSafely(activePowerACL1, thisActivePowerACL);
                }
                if ("ActivePowerL2" in meterData) {
                    isAsymmetric = true;
                    activePowerACL2 = Utils.addSafely(activePowerACL2, meterData.ActivePowerL2);
                } else {
                    activePowerACL2 = Utils.addSafely(activePowerACL2, thisActivePowerACL);
                }
                if ("ActivePowerL3" in meterData) {
                    isAsymmetric = true;
                    activePowerACL3 = Utils.addSafely(activePowerACL3, meterData.ActivePowerL3);
                } else {
                    activePowerACL3 = Utils.addSafely(activePowerACL3, thisActivePowerACL);
                }
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
            if (isAsymmetric) {
                activePowerAC = Utils.addSafely(Utils.addSafely(activePowerACL1, activePowerACL2), activePowerACL3)
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
            result.production.isAsymmetric = isAsymmetric;
            result.production.hasDC = config.chargers.length > 0;
            result.production.activePowerAC = activePowerAC;
            result.production.activePowerACL1 = activePowerACL1;
            result.production.activePowerACL2 = activePowerACL2;
            result.production.activePowerACL3 = activePowerACL3;
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

    private getActualPower(o: any): number {
        if ("ActualPower" in o && o.ActualPower != null) {
            return o.ActualPower
        }
        else {
            return null;
        }
    }
}