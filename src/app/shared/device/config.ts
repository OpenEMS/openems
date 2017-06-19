export interface ChannelAddresses {
    [thing: string]: string[];
}

interface Channel {
    name: string,
    title: string,
    type: "Integer" | "String"
    optional: boolean
}

interface ThingClass {
    class: string,
    text: string,
    title: string,
    channels: Channel[]
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
        bridges: [ThingClass],
        controllers: [ThingClass],
        schedulers: [ThingClass],
        devices: [ThingClass]
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

        let natures = this._meta.natures;
        for (let thing in natures) {
            let i = natures[thing].implements;
            // Ess
            if (i.includes("EssNature")) {
                this.storageThings.push(thing);
            }
            // Meter
            if (i.includes("MeterNature")) {
                let type = natures[thing].channels["type"]["value"];
                if (type === "grid") {
                    this.gridMeters.push(thing);
                } else if (type === "production") {
                    this.productionMeters.push(thing);
                } else {
                    console.warn("Meter without type: " + thing);
                }
            }
            // Charger
            if (i.includes("ChargerNature")) {
                this.productionMeters.push(thing);
            }
        }
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
        let ignoreNatures = {};
        let result = {}
        for (let thing in natures) {
            let i = natures[thing].implements;
            let channels = [];

            if (i.includes("AsymmetricEssNature")) {
                channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
            } else if (i.includes("SymmetricEssNature")) {
                channels.push("ActivePower", "ReactivePower");
            }
            if (i.includes("FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
                ignoreNatures["AsymmetricMeterNature"] = true;
            }

            // Meter
            if (i.includes("AsymmetricMeterNature") && !ignoreNatures["AsymmetricMeterNature"]) {
                channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
            } else if (i.includes("SymmetricMeterNature")) {
                channels.push("ActivePower", "ReactivePower");
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