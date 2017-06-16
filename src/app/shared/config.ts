export interface ChannelAddresses {
    [thing: string]: [string];
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
    public _meta: {
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
    public persistence: [{ class: string }];
    public scheduler: Scheduler;
    public things: Bridge[]

    /**
     * Returns a list of thing ids which are EssNatures.
     * e.g. ["ess0", "ess1"]
     */
    public getEssThings(): string[] {
        // get all configured ESS devices
        let essThings: string[] = [];
        let natures = this._meta.natures;
        for (let thing in natures) {
            let i = natures[thing].implements;
            if (i.includes("EssNature")) {
                essThings.push(thing);
            }
        }
        return essThings;
    }

    /**
     * Returns ChannelAddresses of ESS Soc channels
     */
    public getEssSocChannels(): ChannelAddresses {
        let channels: ChannelAddresses = {};
        this.getEssThings().forEach(device => channels[device] = ['Soc']);
        return channels;
    }

    /**
     * Return ChannelAddresses of power channels
     */
    public getPowerChannels(): { [thing: string]: [string] } {
        let natures = this._meta.natures;
        let ignoreNatures = {};
        let result = {}
        for (let thing in natures) {
            let i = natures[thing].implements;
            let channels = []

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
    public getImportantChannels(): { [thing: string]: [string] } {
        let channels: ChannelAddresses = {};
        Object.assign(channels, this.getPowerChannels(), this.getEssSocChannels());
        return channels;
    }
}