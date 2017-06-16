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
     * Returns a list of device-ids which are EssNatures.
     * e.g. ["ess0", "ess1"]
     */
    public getEssDevices(): string[] {
        // get all configured ESS devices
        let essDevices: string[] = [];
        let natures = this._meta.natures;
        for (let nature in natures) {
            if (natures[nature].implements.includes("EssNature")) {
                essDevices.push(nature);
            }
        }
        return essDevices;
    }
}