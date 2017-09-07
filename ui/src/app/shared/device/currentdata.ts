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
                activePower: null,
                maxActivePower: null
            }, production: {
                powerRatio: null,
                activePower: null, // sum of activePowerAC and activePowerDC
                activePowerAC: null,
                activePowerDC: null,
                maxActivePower: null
            }, grid: {
                powerRatio: null,
                activePower: null,
                maxActivePower: null,
                minActivePower: null
            }, consumption: {
                powerRatio: null,
                activePower: null
            }
        };

        {
            /*
             * Storage
             */
            let soc = null;
            let activePower = null;
            let countSoc = 0;
            for (let thing of config.storageThings) {
                if (thing in currentData) {
                    let essData = currentData[thing];
                    if ("Soc" in essData) {
                        soc = Utils.addSafely(soc, essData.Soc);
                        countSoc += 1;
                    }
                    activePower = Utils.addSafely(activePower, this.getActivePower(essData));
                }
            }
            result.storage.soc = Utils.divideSafely(soc, countSoc);
            result.storage.activePower = activePower;
        }

        {
            /*
             * Grid
             */
            let powerRatio = 0;
            let activePower = null;
            let maxActivePower = 0;
            let minActivePower = 0;
            for (let thing of config.gridMeters) {
                let meterData = currentData[thing];
                let meterConfig = config.things[thing];
                activePower = Utils.addSafely(activePower, this.getActivePower(meterData));
                if ("maxActivePower" in meterConfig) {
                    maxActivePower += meterConfig.maxActivePower;
                }
                if ("minActivePower" in meterConfig) {
                    minActivePower += meterConfig.minActivePower;
                }
            }
            // calculate ratio
            if (activePower == null) {
                if (activePower > 0) {
                    powerRatio = 50 * activePower / maxActivePower
                } else {
                    powerRatio = -50 * activePower / minActivePower
                }
            }
            result.grid.powerRatio = powerRatio;
            result.grid.activePower = activePower;
            result.grid.maxActivePower = maxActivePower;
            result.grid.minActivePower = minActivePower;
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
            let activePower = Utils.addSafely(Utils.addSafely(result.grid.activePower, result.production.activePowerAC), result.storage.activePower);
            let maxActivePower = result.grid.maxActivePower + result.production.maxActivePower + result.storage.maxActivePower;
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