import { DefaultTypes } from '../service/defaulttypes'
import { Role } from '../type/role'
import { Widget } from '../type/widget'
import { Edge } from './edge';
import { ConfigImpl } from './config';

export class ConfigImpl_2018_7 extends ConfigImpl implements DefaultTypes.Config_2018_7 {

    public readonly things?: {
        [id: string]: {
            id: string,
            alias: string,
            class: string | string[],
            [channel: string]: any
        }
    };

    public readonly meta: {
        [factoryPid: string]: {
            implements: string[],
            channels?: {
                [channel: string]: {
                    name: string,
                    title: string,
                    type: string | string[],
                    optional: boolean,
                    array: boolean,
                    readRoles: Role[],
                    writeRoles: Role[],
                    defaultValue: string
                }
            }
        }
    };

    // A list of thing ids which are matching Natures. (e.g. ["ess0", "ess1"])
    public readonly bridges: string[] = [];
    public readonly gridMeters: string[] = [];
    public readonly productionMeters: string[] = [];
    public readonly consumptionMeters: string[] = [];
    public readonly otherMeters: string[] = []; // TODO show otherMeters in Energymonitor
    public readonly scheduler: string = null;
    public readonly controllers: string[] = [];
    public readonly persistences: string[] = [];
    public readonly simulatorDevices: string[] = [];
    public readonly evcsDevices: string[] = [];
    public readonly thresholdDevices: string[] = [];
    public readonly essType: string[] = [];

    constructor(private readonly edge: Edge, private readonly config: DefaultTypes.Config_2018_7) {
        super();

        let esss: string[] = []
        let chargers: string[] = [];

        // convert role-strings to Role-objects
        for (let clazz in config.meta) {
            for (let channel in config.meta[clazz].channels) {
                let roles: Role[] = [];
                for (let roleString of config.meta[clazz].channels[channel].readRoles) {
                    roles.push(Role.getRole("" + roleString /* convert to string */));
                }
                config.meta[clazz].channels[channel].readRoles = roles;
            }
        }

        Object.assign(this, config);

        let gridMeters: string[] = [];
        let productionMeters: string[] = [];
        let consumptionMeters: string[] = [];
        let otherMeters: string[] = [];
        let bridges: string[] = [];
        let scheduler: string = null;
        let controllers: string[] = [];
        let persistences: string[] = [];
        let simulatorDevices: string[] = [];
        let evcsDevices: string[] = [];
        let thresholdDevices: string[] = [];
        let essType: string[] = [];

        for (let thingId in config.things) {
            let thing = config.things[thingId];
            let i = this.getImplements(thing);
            /*
            * Types
            */
            if (i.includes("FeneconCommercialEss")) {
                essType.push("commercial")
            }
            if (i.includes("FeneconMiniEss")) {
                essType.push("mini")
            }
            if (i.includes("AsymmetricSymmetricCombinationEssNature")) {
                essType.push("pro")
            }

            /*
             * Natures
             */
            // Ess
            if (i.includes("EssNature")
                && !i.includes("EssClusterNature") /* ignore cluster */
                && !i.includes("AsymmetricSymmetricCombinationEssNature") /* ignore symmetric Ess of Pro 9-12 */) {

                esss.push(thingId);
            }
            // Meter
            if (i.includes("MeterNature")
                && !i.includes("FeneconMiniConsumptionMeter") /* ignore Mini consumption meter */) {
                if ("type" in thing) {
                    if (thing.type == 'grid') {
                        gridMeters.push(thingId);
                    } else if (thing.type === "production") {
                        productionMeters.push(thingId);
                    } else if (thing.type === "consumption") {
                        consumptionMeters.push(thingId);
                    } else {
                        otherMeters.push(thingId);
                    }
                }
            }
            // Charger
            if (i.includes("ChargerNature")) {
                productionMeters.push(thingId);
                chargers.push(thingId);
            }
            /*
             * Other Things
             */
            // Bridge
            if (i.includes("io.openems.api.bridge.Bridge")) {
                bridges.push(thingId);
            }
            // Scheduler
            if (i.includes("io.openems.api.scheduler.Scheduler")) {
                scheduler = thingId;
            }
            // Controller
            if (i.includes("io.openems.api.controller.Controller")) {
                controllers.push(thingId);
            }
            // Persistence
            if (i.includes("io.openems.api.persistence.Persistence")) {
                persistences.push(thingId);
            }
            // Simulator Devices
            if (i.includes("io.openems.impl.device.simulator.Simulator")) {
                simulatorDevices.push(thingId);
            }
            // Simulator Devices
            if (i.includes("KebaDeviceNature")) {
                evcsDevices.push(thingId);
            }

            // Channelthreshold 
            if (thing.class == "io.openems.impl.controller.channelthreshold.ChannelThresholdController") {
                thresholdDevices.push(thingId);
            }
        }

        this.gridMeters = gridMeters.sort();
        this.productionMeters = productionMeters.sort();
        this.consumptionMeters = consumptionMeters.sort();
        this.otherMeters = otherMeters.sort();
        this.bridges = bridges.sort();
        this.scheduler = scheduler;
        this.controllers = controllers;
        this.persistences = persistences;
        this.simulatorDevices = simulatorDevices;
        this.evcsDevices = evcsDevices;
        this.esss = esss.sort();
        this.chargers = chargers.sort();
        this.thresholdDevices = thresholdDevices;
        this.essType = essType;
    }

    public getStateChannels(): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}

        // Set "ignoreNatures"
        for (let thingId in this.config.things) {
            result[thingId] = ["State"];
        }
        return result;
    }


    /**
    * Return ChannelAddresses of power channels
    */
    public getPowerChannels(): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}

        let ignoreNatures = { EssClusterNature: true };

        // Set "ignoreNatures"
        for (let thingId of this.esss) {
            let i = this.getImplements(this.config.things[thingId]);
            if (i.includes("FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
                ignoreNatures["AsymmetricMeterNature"] = true;
            }
        }
        // Parse all things
        for (let thingId in this.config.things) {
            let clazz = <string>this.config.things[thingId].class; // TODO casting
            let i = this.getImplements(this.config.things[thingId]);
            let channels = [];
            // ESS
            if (i.includes("EssNature")
                && !i.includes("EssClusterNature") /* ignore cluster */
                && !i.includes("AsymmetricSymmetricCombinationEssNature") /* ignore symmetric Ess of Pro 9-12 */) {
                if (i.includes("FeneconMiniEss")) {
                    channels.push("ActivePowerL1");
                } else if (i.includes("AsymmetricEssNature")) {
                    channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3");
                } else if (i.includes("SymmetricEssNature")) {
                    channels.push("ActivePower");
                }
            }
            // Meter
            if (i.includes("MeterNature")
                && !i.includes("FeneconMiniConsumptionMeter") /* ignore Mini consumption meter */) {
                if (i.includes("AsymmetricMeterNature") && !ignoreNatures["AsymmetricMeterNature"]) {
                    channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3");
                } else if (i.includes("SymmetricMeterNature")) {
                    channels.push("ActivePower");
                }
            }
            // Charger
            if (i.includes("ChargerNature")) {
                channels.push("ActualPower");
            }
            // store result
            if (channels.length > 0) {
                result[thingId] = channels;
            }
        }
        return result;
    }

    /**
    * Return ChannelAddresses of EVCS channels
    */
    public getEvcsChannels(): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}
        let ignoreNatures = { EssClusterNature: true };

        // Set "ignoreNatures"
        for (let thingId of this.esss) {
            let i = this.getImplements(this.config.things[thingId]);
            if (i.includes("FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
                ignoreNatures["AsymmetricMeterNature"] = true;
            }
        }
        for (let thingId in this.config.things) {
            let clazz = <string>this.config.things[thingId].class; // TODO casting
            let i = this.getImplements(this.config.things[thingId]);
            let channels = [];
            //EVCS
            for (let thingId of this.evcsDevices) {
                result[thingId] = ["State", "Plug", "CurrUser", "ActualPower", "EnergySession", "EnergyTotal"];
            }
            if (channels.length > 0) {
                result[thingId] = channels;
            }
        }
        return result;
    }

    /**
     * Returns ChannelAddresses of ESS Soc channels
     */
    public getEssSocChannels(): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}
        for (let thingId of this.esss) {
            let channels = [];
            // ESS
            channels.push("Soc");
            // store result
            if (channels.length > 0) {
                result[thingId] = channels;
            }
        }
        return result;
    }

    /**
     * Returns ChannelAddresses required by EVCS widget 
     */
    private getEvcsWidgetChannels(): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}
        for (let thingId of this.evcsDevices) {
            result[thingId] = ["State", "Plug", "CurrUser", "ActualPower", "EnergySession", "EnergyTotal"];
        }
        return result;
    }

    public getThresholdWidgetChannels(): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}
        for (let thingId of this.thresholdDevices) {
            let address = this.config.things[thingId]['outputChannelAddress'].split("/");
            if (!(address[0] in result)) {
                result[address[0]] = []
            };
            result[address[0]].push(address[1]);
        }
        return result;
    }

    /**
     * Return ChannelAddresses of power and soc channels
     */
    public getImportantChannels(): DefaultTypes.ChannelAddresses {
        let channels: DefaultTypes.ChannelAddresses = {};
        function merge(obj: DefaultTypes.ChannelAddresses) {
            for (let thing in obj) {
                if (thing in channels) {
                    channels[thing] = channels[thing].concat(obj[thing]);
                } else {
                    channels[thing] = obj[thing];
                }
            }
        }
        // basic channels
        merge(this.getStateChannels());
        merge(this.getPowerChannels());
        merge(this.getEssSocChannels());
        // widget channels
        merge(this.getEvcsWidgetChannels());
        merge(this.getThresholdWidgetChannels());
        return channels;
    }

    public getWidgets(): Widget[] {
        let widgets: Widget[] = [];
        if (this.evcsDevices.length > 0) {
            widgets.push("EVCS");
        }
        if (this.thresholdDevices.length > 0) {
            widgets.push("CHANNELTHRESHOLD");
        }
        return widgets;
    }

    private getImplements(thing: DefaultTypes.ThingConfig): string | string[] {
        if (<string>thing.class in this.meta) { // TODO casting
            // get implements from meta
            return this.meta[<string>thing.class].implements;
        } else {
            // use class
            return <string>thing.class;
        }
    }
}