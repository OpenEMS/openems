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

export interface Config {
    _meta: {
        natures: { [thing: string]: string[] },
        bridges: [ThingClass],
        controllers: [ThingClass],
        schedulers: [ThingClass],
        devices: [ThingClass]
    },
    persistence: [{ class: string }],
    scheduler: Scheduler,
    things: Bridge[]
}