import { DefaultTypes } from '../service/defaulttypes'
import { Role } from '../type/role'
import { Widget } from '../type/widget'
import { Edge } from './edge';
import { ConfigImpl } from './config';

export class ConfigImpl_2018_8 extends ConfigImpl implements DefaultTypes.Config_2018_8 {

    public readonly components: {
        [id: string]: {
            'service.pid': string, // unique pid of configuration
            'service.factoryPid': string, // link to 'meta'
            enabled: boolean,
            [channel: string]: string | number | boolean
        }
    }

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
    public readonly gridMeters: string[] = [];
    public readonly productionMeters: string[] = [];
    public readonly consumptionMeters: string[] = [];
    public readonly otherMeters: string[] = []; // TODO show otherMeters in Energymonitor
    public readonly bridges: string[] = [];
    public readonly scheduler: string = null;
    public readonly controllers: string[] = [];
    public readonly persistences: string[] = [];
    public readonly simulatorDevices: string[] = [];
    public readonly evcsDevices: string[] = [];

    constructor(private readonly edge: Edge, private readonly config: DefaultTypes.Config_2018_8) {
        super();

        let esss: string[] = []
        let chargers: string[] = [];

        Object.assign(this, config);

        for (let componentId in config.components) {
            const i = this.getImplements(componentId);

            // Ess
            if (i.includes("Ess")) {
                esss.push(componentId);
            }
            if (i.includes("EssDcCharger")) {
                chargers.push(componentId);
            }
        }
        this.esss = esss.sort();
        this.chargers = chargers.sort();
    }

    public getStateChannels(): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}

        // Set "ignoreNatures"
        // TODO
        // for (let thingId in this.config.things) {
        //     result[thingId] = ["State"];
        // }
        return result;
    }

    /**
    * Return ChannelAddresses of power channels
    */
    public getPowerChannels(): DefaultTypes.ChannelAddresses {
        return {
            "_sum": [
                'EssActivePower', 'GridActivePower', 'ProductionActivePower', 'ConsumptionActivePower'
            ]
        }
    }

    /**
     * Returns ChannelAddresses of ESS Soc channels
     */
    public getEssSocChannels(): DefaultTypes.ChannelAddresses {
        return {
            "_sum": [
                'EssSoc'
            ]
        }
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

    /**
     * Return ChannelAddresses of power and soc channels
     */
    public getImportantChannels(): DefaultTypes.ChannelAddresses {
        return {};
    }

    public getWidgets(): Widget[] {
        let widgets: Widget[] = [];
        if (this.evcsDevices.length > 0) {
            widgets.push("EVCS");
        }
        return widgets;
    }

    private getImplements(componentId: string): string[] {
        let component = this.config.components[componentId];
        let i;
        if (component["service.factoryPid"] in this.meta) {
            i = this.meta[component["service.factoryPid"]].implements;
        } else {
            i = [];
        }
        return i;
    }
}