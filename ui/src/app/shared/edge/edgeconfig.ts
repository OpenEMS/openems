import { ChannelAddress } from '../type/channeladdress';
import { Widgets } from '../type/widget';
import { Edge } from './edge';

export interface CategorizedComponents {
    category: {
        title: string,
        icon: string
    },
    components: EdgeConfig.Component[]
};

export interface CategorizedFactories {
    category: {
        title: string,
        icon: string
    },
    factories: EdgeConfig.Factory[]
};

export class EdgeConfig {

    constructor(edge: Edge, source?: EdgeConfig) {
        if (source) {
            this.components = source.components;
            this.factories = source.factories;
        }

        // initialize Components
        for (let componentId in this.components) {
            let component = this.components[componentId];
            component.id = componentId;
            if ('enabled' in component.properties) {
                component.isEnabled = component.properties['enabled']
            } else {
                component.isEnabled = true;
            }
        }

        // initialize Factorys
        for (let factoryId in this.factories) {
            let factory = this.factories[factoryId];
            factory.id = factoryId;
            factory.componentIds = [];

            // Fill 'natures' map
            for (let natureId of factory.natureIds) {
                if (!(natureId in this.natures)) {
                    let parts = natureId.split(".");
                    let name = parts[parts.length - 1];
                    this.natures[natureId] = {
                        id: natureId,
                        name: name,
                        factoryIds: []
                    };
                }
                this.natures[natureId].factoryIds.push(factoryId);
            }
        }

        if (Object.keys(this.components).length != 0 && Object.keys(this.factories).length == 0) {
            console.warn("Factory definitions are missing.");
        } else {
            for (let componentId in this.components) {
                let component = this.components[componentId];
                if (component.factoryId === "") {
                    continue; // Singleton components have no factory-PID
                }
                let factory = this.factories[component.factoryId];
                if (!factory) {
                    console.warn("Factory definition for [" + component.factoryId + "] is missing.");
                    continue;
                }

                // Complete 'factories' map
                factory.componentIds.push(componentId);
            }
        }

        // Initialize Widgets
        this.widgets = Widgets.parseWidgets(edge, this);
    }

    /**
     * Component-ID -> Component.
     */
    public readonly components: { [id: string]: EdgeConfig.Component } = {};

    /**
     * Factory-PID -> OSGi Factory.
     */
    public readonly factories: { [id: string]: EdgeConfig.Factory } = {};

    /**
     * Nature-PID -> Component-IDs.
     */
    public readonly natures: { [id: string]: EdgeConfig.Nature } = {}

    /**
     * UI-Widgets.
     */
    public readonly widgets: Widgets;

    public isValid(): boolean {
        return Object.keys(this.components).length > 0 && Object.keys(this.factories).length > 0;
    }

    /**
     * Get Component-IDs of Component instances by the given Factory.
     * 
     * @param factoryId the Factory PID.
     */
    public getComponentIdsByFactory(factoryId: string): string[] {
        let factory = this.factories[factoryId];
        if (factory) {
            return factory.componentIds;
        } else {
            return [];
        }
    }

    /**
     * Get Factories of Nature.
     * 
     * @param natureId the given Nature.
     */
    public getFactoriesByNature(natureId: string): EdgeConfig.Factory[] {
        let result = [];
        let nature = this.natures[natureId];
        if (nature) {
            for (let factoryId of nature.factoryIds) {
                if (factoryId in this.factories) {
                    result.push(this.factories[factoryId])
                }
            }
        }
        return result;
    }

    /**
     * Get Factories by Factory-IDs.
     * 
     * @param ids the given Factory-IDs.
     */
    public getFactoriesByIds(factoryIds: string[]): EdgeConfig.Factory[] {
        let result = [];
        for (let factoryId of factoryIds) {
            if (factoryId in this.factories) {
                result.push(this.factories[factoryId])
            }
        }
        return result;
    }

    /**
     * Get Component instances by the given Factory.
     * 
     * @param factoryId the Factory PID.
     */
    public getComponentsByFactory(factoryId: string): EdgeConfig.Component[] {
        let componentIds = this.getComponentIdsByFactory(factoryId);
        let result: EdgeConfig.Component[] = [];
        for (let componentId of componentIds) {
            result.push(this.components[componentId]);
        }
        return result;
    }

    /**
     * Get Component-IDs of Components that implement the given Nature.
     * 
     * @param nature the given Nature.
     */
    public getComponentIdsImplementingNature(natureId: string): string[] {
        let result: string[] = [];
        let nature = this.natures[natureId];
        if (nature) {
            for (let factoryId of nature.factoryIds) {
                result.push.apply(result, this.getComponentIdsByFactory(factoryId));
            }
        }
        return result;
    }

    /**
     * Get Components that implement the given Nature.
     * 
     * @param nature the given Nature.
     */
    public getComponentsImplementingNature(natureId: string): EdgeConfig.Component[] {
        let result: EdgeConfig.Component[] = [];
        let nature = this.natures[natureId];
        if (nature) {
            for (let factoryId of nature.factoryIds) {
                result.push.apply(result, this.getComponentsByFactory(factoryId));
            }
        }
        return result;
    }

    /**
     * Get the implemented NatureIds by Factory-ID.
     * 
     * @param factoryId the Factory-ID
     */
    public getNatureIdsByFactoryId(factoryId: string): string[] {
        let factory = this.factories[factoryId];
        if (factory) {
            return factory.natureIds;
        } else {
            return [];
        }
    }

    /**
     * Determines if Edge has a Storage device
     */
    public hasStorage(): boolean {
        if (this.getComponentIdsImplementingNature('io.openems.edge.ess.api.SymmetricEss').length > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if Edge has a Meter device
     */
    public hasMeter(): boolean {
        if (this.getComponentIdsImplementingNature('io.openems.edge.meter.api.SymmetricMeter').length > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if Edge has a producing device
     */
    public hasProducer(): boolean {
        // Do we have a Ess DC Charger?
        if (this.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').length > 0) {
            return true;
        }
        // Do we have a Meter with type PRODUCTION?
        for (let component of this.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")) {
            if (component.isEnabled) {
                // TODO make sure 'type' is provided for all Meters
                if (component.properties['type'] == "PRODUCTION") {
                    return true;
                }
                // TODO remove, once all Edges are at least version 2019.15
                switch (component.factoryId) {
                    case 'Fenecon.Mini.PvMeter':
                    case 'Fenecon.Dess.PvMeter':
                    case 'Fenecon.Pro.PvMeter':
                    case 'Kostal.Piko.Charger':
                    case 'Kaco.BlueplanetHybrid10.PvInverter':
                    case 'PV-Inverter.Solarlog':
                    case 'PV-Inverter.KACO.blueplanet':
                    case 'PV-Inverter.SunSpec':
                    case 'SolarEdge.PV-Inverter':
                    case 'Simulator.PvInverter':
                    case 'Simulator.ProductionMeter.Acting':
                        return true;
                }
            }
        }
        return false;
    }

    public isProducer(component: EdgeConfig.Component) {
        // TODO make sure 'type' is provided for all Meters
        if (component.properties['type'] == "PRODUCTION") {
            return true;
        } else {
            // TODO remove, once all Edges are at least version 2019.15
            switch (component.factoryId) {
                case 'Fenecon.Mini.PvMeter':
                case 'Fenecon.Dess.PvMeter':
                case 'Fenecon.Pro.PvMeter':
                case 'Kostal.Piko.Charger':
                case 'Kaco.BlueplanetHybrid10.PvInverter':
                case 'PV-Inverter.Solarlog':
                case 'PV-Inverter.KACO.blueplanet':
                case 'PV-Inverter.SunSpec':
                case 'SolarEdge.PV-Inverter':
                case 'Simulator.PvInverter':
                case 'Simulator.ProductionMeter.Acting':
                    return true;
            }
        }
        return false;
    }

    /**
     * Lists all available Factories, grouped by category.
     */
    public listAvailableFactories(): CategorizedFactories[] {
        let allFactories = [
            {
                category: { title: 'Simulatoren', icon: 'flask-outline' },
                factories: Object.values(this.factories).filter(factory => factory.id.startsWith('Simulator.'))
            },
            {
                category: { title: 'Serielle Verbindungen', icon: 'swap-horizontal-outline' },
                factories: [
                    this.getFactoriesByIds([
                        'Bridge.Mbus', 'Bridge.Onewire', 'Bridge.Modbus.Serial', 'Bridge.Modbus.Tcp'
                    ])
                ]
            },
            {
                category: { title: 'Zähler', icon: 'speedometer-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.meter.api.SymmetricMeter")
                ]
            },
            {
                category: { title: 'Speichersysteme', icon: 'battery-charging-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.ess.api.SymmetricEss"),
                    this.getFactoriesByNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                ]
            },
            {
                category: { title: 'Batterien', icon: 'battery-full-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.battery.api.Battery")
                ]
            },
            {
                category: { title: 'I/Os', icon: 'log-in-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.io.api.DigitalOutput"),
                    this.getFactoriesByNature("io.openems.edge.io.api.DigitalInput")
                ]
            },
            {
                category: { title: 'E-Auto-Ladestation', icon: 'car-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.evcs.api.Evcs")
                ]
            },
            {
                category: { title: 'Standard-Controller', icon: 'resize-outline' },
                factories: [
                    this.getFactoriesByIds(['Controller.Debug.Log', 'Controller.Debug.DetailedLog'])
                ]
            },
            {
                category: { title: 'Externe Schnittstellen', icon: 'megaphone-outline' },
                factories: [
                    this.getFactoriesByIds([
                        'Controller.Api.Backend', 'Controller.Api.Websocket', 'Controller.Api.ModbusTcp', 'Controller.Api.ModbusTcp.ReadOnly',
                        'Controller.Api.ModbusTcp.ReadWrite', 'Controller.Api.Rest.ReadOnly', 'Controller.Api.Rest.ReadWrite'
                    ])
                ]
            },
            {
                category: { title: 'Spezial-Controller', icon: 'repeat-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.controller.api.Controller"),
                ]
            },
            {
                category: { title: 'Timeseries-Datenbank', icon: 'repeat-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.timedata.api.Timedata"),
                ]
            },
            {
                category: { title: 'Scheduler', icon: 'stopwatch-outline' },
                factories: [
                    this.getFactoriesByNature("io.openems.edge.scheduler.api.Scheduler")
                ]
            },
            {
                category: { title: 'Weitere', icon: 'radio-button-off-outline' },
                factories: Object.values(this.factories)
            }
            // TODO weitere Factories?
        ];

        let ignoreFactoryIds: string[] = [];
        let result: CategorizedFactories[] = [];
        allFactories.forEach(item => {
            let factories =
                // create one flat array
                [].concat(...item.factories)
                    // remove Factories from list that have already been listed before
                    .filter(factory => !ignoreFactoryIds.includes(factory.id))
                    // remove duplicates
                    .filter((e, i, arr) => arr.indexOf(e) === i);
            if (factories.length > 0) {
                factories.forEach(factory => {
                    ignoreFactoryIds.push(factory.id);
                });
                result.push({ category: item.category, factories: factories.sort((a, b) => a.id.localeCompare(b.id)) })
            }
        })
        return result;
    }

    /**
     * Lists all active Components, grouped by category.
     */
    public listActiveComponents(ignoreComponentIds: string[]): CategorizedComponents[] {
        let allComponents = [];
        let factories = this.listAvailableFactories();
        for (let entry of factories) {
            let components = [];
            for (let factory of entry.factories) {
                components.push(this.getComponentsByFactory(factory.id));
                // components.concat(...this.getComponentsByFactory(factory.id));
            }
            allComponents.push({
                category: entry.category,
                components: components
            });
        }
        let result: CategorizedComponents[] = [];
        allComponents.forEach(item => {
            let components =
                // create one flat array
                [].concat(...item.components)
                    // remove Components from list that have already been listed before
                    .filter(component => !ignoreComponentIds.includes(component.id))
                    // remove duplicates
                    .filter((e, i, arr) => arr.indexOf(e) === i);
            if (components.length > 0) {
                components.forEach(component => {
                    ignoreComponentIds.push(component.id);
                });
                result.push({ category: item.category, components: components })
            }
        })
        return result;
    }


    /**
     * Get the implemented Natures by Component-ID.
     * 
     * @param componentId the Component-ID
     */
    public getNatureIdsByComponentId(componentId: string): string[] {
        let component = this.components[componentId];
        if (!component) {
            return [];
        }
        let factoryId = component.factoryId;
        return this.getNatureIdsByFactoryId(factoryId);
    }

    /**
     * Get the Component.
     * 
     * @param componentId the Component-ID
     */
    public getComponent(componentId: string): EdgeConfig.Component {
        return this.components[componentId];
    }

    /**
     * Get the Component properties.
     * 
     * @param componentId the Component-ID
     */
    public getComponentProperties(componentId: string): { [key: string]: any } {
        let component = this.components[componentId];
        if (component) {
            return component.properties;
        } else {
            return {};
        }
    }

    /**
     * Get Channel.
     * 
     * @param address the ChannelAddress
     */
    public getChannel(address: ChannelAddress): EdgeConfig.ComponentChannel {
        let component = this.components[address.componentId];
        if (component) {
            return component.channels[address.channelId];
        } else {
            return null;
        }
    }
}

export module EdgeConfig {
    export class ComponentChannel {
        public readonly type: "BOOLEAN" | "SHORT" | "INTEGER" | "LONG" | "FLOAT" | "DOUBLE" | "STRING";
        public readonly accessMode: "RO" | "RW" | "WO";
        public readonly unit: string;
        public readonly category: "OPENEMS_TYPE" | "ENUM" | "STATE";
    }

    export class Component {
        public id: string = "";
        public alias: string = "";
        public isEnabled: boolean = false;

        constructor(
            public readonly factoryId: string = "",
            public readonly properties: { [key: string]: any } = {},
            public readonly channels: { [channelId: string]: ComponentChannel } = {}
        ) { }
    }

    export class FactoryProperty {
        public readonly id: string;
        public readonly name: string;
        public readonly description: string;
        public readonly isRequired: boolean;
        public readonly defaultValue: any;
        public readonly schema: {};
    }

    export class Factory {
        public id: string = "";
        public componentIds: string[] = [];

        constructor(
            public readonly name: string,
            public readonly description: string,
            public readonly natureIds: string[] = [],
            public readonly properties: FactoryProperty[] = []
        ) { }

        /**
         * Gets the FactoryProperty definition for a Property-ID.
         * 
         * @param propertyId the Property-ID
         */
        static getPropertyForId(factory: Factory, propertyId: string): FactoryProperty {
            for (let property of factory.properties) {
                if (property.id === propertyId) {
                    return property;
                }
            }
            return null;
        }
    }

    export class Nature {
        public id: string = "";
        public name: string = "";
        public factoryIds: string[] = [];
    }
}