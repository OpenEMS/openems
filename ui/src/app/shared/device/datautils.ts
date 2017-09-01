import { DefaultTypes } from '../service/defaulttypes';
import { ConfigUtils } from './configutils';

export class DataUtils {

    private static getActivePower(o: any): number {
        if ("ActivePowerL1" in o && o.ActivePowerL1 != null && "ActivePowerL2" in o && o.ActivePowerL2 != null && "ActivePowerL3" in o && o.ActivePowerL3 != null) {
            return o.ActivePowerL1 + o.ActivePowerL2 + o.ActivePowerL3;
        } else if ("ActivePower" in o && o.ActivePower != null) {
            return o.ActivePower;
        } else {
            return 0;
        }
    }

    public static calculateSummary(currentData: DefaultTypes.CurrentData, config: DefaultTypes.Config): DefaultTypes.Summary {
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
                maxActivePower: null
            }, consumption: {
                powerRatio: null,
                activePower: null
            }
        };

        {
            /*
             * Storage
             */
            let soc = 0;
            let activePower = 0;
            let countSoc = 0;
            for (let thing of ConfigUtils.getEssNatures(config)) {
                if (thing in currentData) {
                    let ess = currentData[thing];
                    if ("Soc" in ess && ess.Soc != null) {
                        soc += ess.Soc;
                        countSoc += 1;
                    }
                    activePower += DataUtils.getActivePower(ess);
                }
            }
            result.storage.soc = soc / countSoc;
            result.storage.activePower = activePower;
        }

        {
            /*
             * Grid
             */
            let powerRatio = 0;
            let activePower = 0;
            let maxActivePower = 0;
            for (let thing of ConfigUtils.getMeterNatures(config)) {
                if (thing in currentData && thing in config.things && "type" in config.things[thing] && config.things[thing]["type"]) {
                    let meter = currentData[thing];
                    let power = DataUtils.getActivePower(meter);
                    if ("maxActivePower" in meter && meter.maxActivePower != null) {
                        if (activePower > 0) {
                            powerRatio = (power * 50.) / thingChannels["maxActivePower"]["value"]
                        } else {
                            powerRatio = (power * -50.) / thingChannels["minActivePower"]["value"]
                        }
                    } else {
                        console.log("no maxActivePower Grid");
                    }
                    activePower += power;
                    maxActivePower += thingChannels["maxActivePower"]["value"];
                }
            }
            this.grid.powerRatio = powerRatio;
            this.grid.activePower = activePower;
            this.grid.maxActivePower = maxActivePower;
        }

        {
            /*
             * Production
             */
            let powerRatio = 0;
            let activePowerAC = 0;
            let activePowerDC = 0;
            let maxActivePower = 0;
            for (let thing of config.productionMeters) {
                if (thing in data) {
                    let thingChannels = config._meta.natures[thing].channels;
                    let meter = data[thing];
                    activePowerAC += getActivePower(meter);
                    if ("ActualPower" in meter && meter.ActualPower != null) {
                        activePowerDC += meter.ActualPower;
                    }
                    if (thingChannels["maxActivePower"]) {
                        maxActivePower += thingChannels["maxActivePower"]["value"];
                    } else {
                        // no maxActivePower
                    }
                    if (thingChannels["maxActualPower"]) {
                        maxActivePower += thingChannels["maxActualPower"]["value"];
                    } else {
                        // no maxActualPower
                    }
                }
            }

            // correct negative production
            if (activePowerAC < 0) {
                // console.warn("negative production? ", this)
                activePowerAC = 0;
            }
            if (maxActivePower < 0) { maxActivePower = 0; }

            if (maxActivePower == 0) {
                powerRatio = 100;
            } else {
                powerRatio = ((activePowerAC + activePowerDC) * 100.) / maxActivePower;
            }

            this.production.powerRatio = powerRatio;
            this.production.activePowerAC = activePowerAC;
            this.production.activePowerDC = activePowerDC;
            this.production.activePower = activePowerAC + activePowerDC;
            this.production.maxActivePower = maxActivePower;
        }

        {
            /*
             * Consumption
             */
            let activePower = this.grid.activePower + this.production.activePowerAC + this.storage.activePower;
            let maxActivePower = this.grid.maxActivePower + this.production.maxActivePower + this.storage.maxActivePower;
            this.consumption.powerRatio = (activePower * 100.) / maxActivePower;
            this.consumption.activePower = activePower;
        }
        return result;
    }
}


//   private getkWhResult(channels: { [thing: string]: [string] }): { [thing: string]: [string] } {
//     let kWh = {};
//     let thingChannel = [];

//     for (let type in this.things) {
//       for (let thing in this.things[type]) {
//         for (let channel in channels[thing]) {
//           if (channels[thing][channel] == "ActivePower") {
//             kWh[thing + "/ActivePower"] = type;
//           } else if (channels[thing][channel] == "ActivePowerL1" || channels[thing][channel] == "ActivePowerL2" || channels[thing][channel] == "ActivePowerL3") {
//             kWh[thing + "/ActivePowerL1"] = type;
//             kWh[thing + "/ActivePowerL2"] = type;
//             kWh[thing + "/ActivePowerL3"] = type;
//           }
//         }
//       }
//     }

//     return kWh;
//   }