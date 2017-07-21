import { Role, ROLES } from '../type/role';

export interface ChannelAddresses {
    [thing: string]: string[];
}

interface Channel {
    name: string,
    title: string,
    type: "Integer" | "String"
    optional: boolean,
    array: boolean,
    accessLevel: string
}

interface ThingClass {
    class: string,
    text: string,
    title: string,
    channels: {
        [thing: string]: Channel[]
    }
}

interface Thing {
    id: string,
    class: string
}

interface Scheduler extends Thing {
    controllers: [{
        id: string,
        class: string
    }]
}

interface Bridge extends Thing {
    devices: [{
        id: string,
        class: string
    }]
}

export class Config {
    public readonly _meta: {
        natures: {
            [thing: string]: {
                channels: {},
                implements: string[]
            }
        },
        availableBridges: {
            [thing: string]: ThingClass
        },
        availableControllers: {
            [thing: string]: ThingClass
        },
        availableSchedulers: {
            [thing: string]: ThingClass
        },
        availableDevices: {
            [thing: string]: ThingClass
        }
    };
    public readonly persistence: [{ class: string }];
    public readonly scheduler: Scheduler;
    public readonly things: Bridge[]

    // A list of thing ids which are EssNatures. (e.g. ["ess0", "ess1"])
    public readonly storageThings: string[] = [];
    public readonly gridMeters: string[] = [];
    public readonly productionMeters: string[] = [];

    constructor(config: any) {
        Object.assign(this, config);
        console.log(this._meta)

        let storageThings: string[] = []
        let gridMeters: string[] = [];
        let productionMeters: string[] = [];

        if (this._meta && "natures" in this._meta) {
            let natures = this._meta.natures;
            for (let thing in natures) {
                if ("implements" in natures[thing]) {
                    let i = natures[thing].implements;
                    // Ess
                    if (i.includes("EssNature")) {
                        if (!i.includes("EssClusterNature")) { // ignore cluster
                            storageThings.push(thing);
                        }
                    }
                    // Meter
                    if (i.includes("MeterNature")) {
                        let type = natures[thing].channels["type"]["value"];
                        if (type === "grid") {
                            gridMeters.push(thing);
                        } else if (type === "production") {
                            productionMeters.push(thing);
                        } else {
                            console.warn("Meter without type: " + thing);
                        }
                    }
                    // Charger
                    if (i.includes("ChargerNature")) {
                        productionMeters.push(thing);
                    }
                }
            }
        }

        this.storageThings = storageThings.sort();
        this.gridMeters = gridMeters.sort();
        this.productionMeters = productionMeters.sort();
    }

    /**
     * Returns ChannelAddresses of ESS Soc channels
     */
    public getEssSocChannels(): ChannelAddresses {
        let channels: ChannelAddresses = {};
        this.storageThings.forEach(device => channels[device] = ['Soc']);
        return channels;
    }

    /**
     * Return ChannelAddresses of power channels
     */
    public getPowerChannels(): ChannelAddresses {
        let natures = this._meta.natures;
        let ignoreNatures = { EssClusterNature: true };
        let result = {}
        for (let thing in natures) {
            let i = natures[thing].implements;
            let channels = [];

            // ESS
            if (!i.includes("EssClusterNature")) { // ignore cluster
                if (i.includes("AsymmetricEssNature")) {
                    channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
                } else if (i.includes("SymmetricEssNature")) {
                    channels.push("ActivePower", "ReactivePower");
                }
                if (i.includes("FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
                    ignoreNatures["AsymmetricMeterNature"] = true;
                }
            }

            // Meter
            if (i.includes("AsymmetricMeterNature") && !ignoreNatures["AsymmetricMeterNature"]) {
                channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
            } else if (i.includes("SymmetricMeterNature")) {
                channels.push("ActivePower", "ReactivePower");
            }
            // Charger
            if (i.includes("ChargerNature")) {
                channels.push("ActualPower");
            }
            result[thing] = channels;
        }
        return result;
    }

    /**
     * Return ChannelAddresses of power and soc channels
     */
    public getImportantChannels(): ChannelAddresses {
        let channels: ChannelAddresses = this.getPowerChannels();
        let essChannels = this.getEssSocChannels();
        for (let thing in essChannels) {
            if (thing in channels) {
                let arr = essChannels[thing];
                channels[thing] = channels[thing].concat(arr);
            } else {
                channels[thing] = essChannels[thing];
            }
        }
        return channels;
    }
}

//   private refreshThingsFromConfig(): Things {
//     let result = new Things();
//     let config = this.config.getValue();
//     if ("_meta" in config && "natures" in config._meta) {
//       let natures = this.config.getValue()._meta.natures;
//       for (let thing in natures) {
//         let i = natures[thing]["implements"];

//       }
//     }
//     return result;
//   }