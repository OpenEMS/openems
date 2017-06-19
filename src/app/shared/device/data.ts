import { Config } from './config';

export class Summary {
    public readonly storage = {
        soc: null,
        activePower: 0,
        maxActivePower: 0
    };
    public readonly production = {
        powerRatio: 0,
        activePower: 0,
        maxActivePower: 0
    };
    public readonly grid = {
        powerRatio: 0,
        activePower: 0,
        maxActivePower: 0
    };
    public readonly consumption = {
        powerRatio: 0,
        activePower: 0
    };

    /**
     * Calculate summary data from websocket reply
     */
    constructor(config: Config, data: ChannelData) {
        function getActivePower(o: any): number {
            let activePower = 0;
            if ("ActivePowerL1" in o && o.ActivePowerL1 != null && "ActivePowerL2" in o && o.ActivePowerL2 != null && "ActivePowerL3" in o && o.ActivePowerL3 != null) {
                activePower = o.ActivePowerL1 + o.ActivePowerL2 + o.ActivePowerL3;
            } else if ("ActivePower" in o && o.ActivePower != null) {
                activePower = o.ActivePower;
            } else {
                activePower = 0;
            }

            if ("ActualPower" in o && o.ActualPower != null) {
                activePower += o.ActualPower;
            }

            return activePower;
        }

        {
            /*
             * Storage
             */
            let soc = 0;
            let activePower = 0;
            let essThings = config.storageThings;
            for (let thing of essThings) {
                if (thing in data) {
                    let ess = data[thing];
                    soc += ess["Soc"];
                    activePower += getActivePower(ess);
                }
            }
            this.storage.soc = soc / Object.keys(essThings).length;
            this.storage.activePower = activePower;
        }

        {
            /*
             * Grid
             */
            let powerRatio = 0;
            let activePower = 0;
            let maxActivePower = 0;
            for (let thing of config.gridMeters) {
                if (thing in data) {
                    let thingChannels = config._meta.natures[thing].channels;
                    let meter = data[thing];
                    let power = getActivePower(meter);
                    if (thingChannels["maxActivePower"]) {
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
                    // + meter["ActivePowerL1"] + meter["ActivePowerL2"] + meter["ActivePowerL3"];
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
            let activePower = 0;
            let maxActivePower = 0;
            for (let thing of config.productionMeters) {
                if (thing in data) {
                    let thingChannels = config._meta.natures[thing].channels;
                    let meter = data[thing];
                    let power = getActivePower(meter);
                    activePower += power;
                    if (thingChannels["maxActivePower"]) {
                        maxActivePower += thingChannels["maxActivePower"]["value"];
                        // + meter["ActivePowerL1"] + meter["ActivePowerL2"] + meter["ActivePowerL3"];
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

            if (maxActivePower == 0) {
                throw "Teilen durch 0 nicht möglich!";
            } else if (activePower == 0) {
                powerRatio = 0;
            } else {
                powerRatio = (activePower * 100.) / maxActivePower;
            }

            this.production.powerRatio = powerRatio;
            this.production.activePower = activePower;
            this.production.maxActivePower = maxActivePower;
        }

        {
            /*
             * Consumption
             */
            let activePower = this.grid.activePower + this.production.activePower + this.storage.activePower;
            let maxActivePower = this.grid.maxActivePower + this.production.maxActivePower + this.storage.maxActivePower;
            this.consumption.powerRatio = (activePower * 100.) / maxActivePower;
            this.consumption.activePower = activePower;
        }
    }
}

export class ChannelData {
    [thing: string]: {
        [channel: string]: number;
    }
}

export class Data {
    public readonly summary: Summary;

    constructor(
        public readonly data: ChannelData,
        private config: Config
    ) {
        this.summary = new Summary(config, data);
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