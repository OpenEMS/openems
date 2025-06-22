import { TranslateService } from "@ngx-translate/core";
import { ChannelAddress, Widgets } from "../../shared";
import { Edge } from "./edge";

export interface CategorizedComponents {
    category: {
        title: string,
        icon: string,
    },
    components: EdgeConfig.Component[]
}

export interface CategorizedFactories {
    category: {
        title: string,
        icon: string
    },
    factories: EdgeConfig.Factory[]
}

export class EdgeConfig {

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
    public readonly natures: { [id: string]: EdgeConfig.Nature } = {};

    /**
     * UI-Widgets.
     */
    public readonly widgets: Widgets;

    constructor(edge: Edge, source?: EdgeConfig) {

        if (source) {
            this.components = source.components;
            this.factories = source.factories;
        }

        // initialize Components
        for (const componentId in this.components) {
            const component = this.components[componentId];
            component.id = componentId;
            if ("enabled" in component.properties) {
                component.isEnabled = component.properties["enabled"];
            } else {
                component.isEnabled = true;
            }
        }

        // initialize Factorys
        for (const factoryId in this.factories) {
            const factory = this.factories[factoryId];
            factory.id = factoryId;
            factory.componentIds = [];

            // Fill 'natures' map
            for (const natureId of factory.natureIds) {
                if (!(natureId in this.natures)) {
                    const parts = natureId.split(".");
                    const name = parts[parts.length - 1];
                    this.natures[natureId] = {
                        id: natureId,
                        name: name,
                        factoryIds: [],
                    };
                }
                this.natures[natureId].factoryIds.push(factoryId);
            }
        }

        if (Object.keys(this.components).length != 0 && Object.keys(this.factories).length == 0) {
            console.warn("Factory definitions are missing.");
        } else {
            for (const componentId in this.components) {
                const component = this.components[componentId];
                if (component.factoryId === "") {
                    continue; // Singleton components have no factory-PID
                }
                const factory = this.factories[component.factoryId];
                if (!factory) {
                    console.warn("Factory definition [" + component.factoryId + "] for [" + componentId + "] is missing.");
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
     * Lists all available Factories, grouped by category.
     */
    public static listAvailableFactories(factories: { [id: string]: EdgeConfig.Factory }, translate: TranslateService): CategorizedFactories[] {
        const allFactories: CategorizedFactories[] = [
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.SIMULATORS"), icon: "flask-outline" },
                factories: Object.entries(factories)
                    .filter(([factory]) => factory.startsWith("Simulator."))
                    .map(e => e[1]),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.METER"), icon: "speedometer-outline" },
                factories: [
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.meter.api.SymmetricMeter"), // TODO replaced by ElectricityMeter
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.meter.api.ElectricityMeter", "io.openems.edge.evcs.api.Evcs"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.ess.dccharger.api.EssDcCharger"),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.STORAGE_SYSTEMS"), icon: "battery-charging-outline" },
                factories: [
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.ess.api.SymmetricEss"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.battery.api.Battery"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter"),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.STORAGE_SYSTEM_CONTROL"), icon: "options-outline" },
                factories: [
                    EdgeConfig.getFactoriesByIdsPattern(factories, [
                        /Controller\.Asymmetric.*/,
                        /Controller\.Ess.*/,
                        /Controller\.Symmetric.*/,
                    ]),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.EV_CHARGING_STATION"), icon: "car-outline" },
                factories: [
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.evcs.api.Evcs"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.evse.api.chargepoint.EvseChargePoint"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle"),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.EV_CHARGING_STATION_CONTROL"), icon: "options-outline" },
                factories: [
                    EdgeConfig.getFactoriesByIds(factories, [
                        "Controller.Evcs",
                        "Evse.Controller.Single",
                        "Evse.Controller.Cluster",
                    ]),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.IOs"), icon: "log-in-outline" },
                factories: [
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.io.api.DigitalOutput"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.io.api.DigitalInput"),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.IO-CONTROL"), icon: "options-outline" },
                factories: [
                    EdgeConfig.getFactoriesByIds(factories, [
                        "Controller.IO.ChannelSingleThreshold",
                        "Controller.Io.FixDigitalOutput",
                        "Controller.IO.HeatingElement",
                        "Controller.Heat.Heatingelement",
                        "Controller.IO.Heating.Room",
                        "Controller.Io.HeatPump.SgReady",
                    ]),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.TEMPERATURE_SENSORS"), icon: "thermometer-outline" },
                factories: [
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.thermometer.api.Thermometer"),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.EXTERNAL_INTERFACES"), icon: "megaphone-outline" },
                factories: [
                    EdgeConfig.getFactoriesByIds(factories, [
                        "Controller.Api.Websocket",
                        "Controller.Api.ModbusTcp",
                        "Controller.Api.ModbusTcp.ReadOnly",
                        "Controller.Api.ModbusTcp.ReadWrite",
                        "Controller.Api.ModbusRtu.ReadOnly",
                        "Controller.Api.ModbusRtu.ReadWrite",
                        "Controller.Api.MQTT",
                        "Controller.Api.Rest.ReadOnly",
                        "Controller.Api.Rest.ReadWrite",
                    ]),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.CLOUD_INTERFACES"), icon: "cloud-outline" },
                factories: [
                    EdgeConfig.getFactoriesByIdsPattern(factories, [
                        /TimeOfUseTariff\.*/,
                    ]),
                    EdgeConfig.getFactoriesByIds(factories, [
                        "Controller.Api.Backend",
                        "Controller.Clever-PV",
                    ]),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.DEVICE_INTERFACES"), icon: "swap-horizontal-outline" },
                factories: [
                    EdgeConfig.getFactoriesByIds(factories, [
                        "Bridge.Mbus",
                        "Bridge.Onewire",
                        "Bridge.Modbus.Serial",
                        "Bridge.Modbus.Tcp",
                        "Kaco.BlueplanetHybrid10.Core",
                    ]),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.STANDARD_COMPONENTS"), icon: "resize-outline" },
                factories: [
                    EdgeConfig.getFactoriesByIds(factories, [
                        "Controller.Debug.Log",
                        "Controller.Debug.DetailedLog",
                    ]),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.timedata.api.Timedata"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.predictor.api.oneday.Predictor24Hours"),
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.scheduler.api.Scheduler"),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.DEDICATED_CONTROLLERS"), icon: "repeat-outline" },
                factories: [
                    EdgeConfig.getFactoriesByNature(factories, "io.openems.edge.controller.api.Controller"),
                ].flat(2),
            },
            {
                category: { title: translate.instant("SETTINGS.CATEGORY.TITLE.OTHERS"), icon: "radio-button-off-outline" },
                factories: Object.values(factories),
            },
        ];

        const ignoreFactoryIds: string[] = [];
        const result: CategorizedFactories[] = [];
        allFactories.forEach(item => {
            const factories =
                item.factories
                    // remove Factories from list that have already been listed before
                    .filter(factory => !ignoreFactoryIds.includes(factory.id))
                    // remove duplicates
                    .filter((e, i, arr) => arr.indexOf(e) === i);
            if (factories.length > 0) {
                factories.forEach(factory => {
                    ignoreFactoryIds.push(factory.id);
                });
                result.push({ category: item.category, factories: factories.sort((a, b) => a.id.localeCompare(b.id)) });
            }
        });
        return result;
    }

    /**
     * Get Factories of Nature.
     *
     * @param factories the given EdgeConfig.Factory
     * @param includeNature the name of the Nature to be included
     * @param excludeNature an optional name of a Nature to be excluded
     */
    public static getFactoriesByNature(factories: { [id: string]: EdgeConfig.Factory }, includeNature: string, excludeNature?: string): EdgeConfig.Factory[] {
        const result = [];
        const natures = EdgeConfig.getNaturesOfFactories(factories);
        const include = natures[includeNature];
        const excludes = excludeNature != null && excludeNature in natures ? natures[excludeNature].factoryIds : [];
        if (include) {
            for (const factoryId of include.factoryIds) {
                if (excludes.includes(factoryId)) {
                    continue;
                }
                if (factoryId in factories) {
                    result.push(factories[factoryId]);
                }
            }
        }
        return result;
    }

    public static getFactoriesByIds(factories: { [id: string]: EdgeConfig.Factory }, factoryIds: string[]): EdgeConfig.Factory[] {
        const result = [];
        for (const factoryId of factoryIds) {
            if (factoryId in factories) {
                result.push(factories[factoryId]);
            }
        }
        return result;
    }

    public static getFactoriesByIdsPattern(factories: { [id: string]: EdgeConfig.Factory }, patterns: RegExp[]): EdgeConfig.Factory[] {
        const result = [];
        for (const pattern of patterns) {
            for (const factoryId in factories) {
                if (pattern.test(factoryId)) {
                    result.push(factories[factoryId]);
                }
            }
        }
        return result;
    }

    public static getNaturesOfFactories(factories: { [id: string]: EdgeConfig.Factory }): { [natureId: string]: EdgeConfig.Nature } {
        const natures: { [natureId: string]: EdgeConfig.Nature } = {};
        // initialize Factorys
        for (const [factoryId, factory] of Object.entries(factories)) {
            // Fill 'natures' map
            for (const natureId of factory.natureIds) {
                if (!(natureId in natures)) {
                    const parts = natureId.split(".");
                    const name = parts[parts.length - 1];
                    natures[natureId] = {
                        id: natureId,
                        name: name,
                        factoryIds: [],
                    };
                }
                natures[natureId].factoryIds.push(factoryId);
            }
        }
        return natures;
    }

    public isValid(): boolean {
        return Object.keys(this.components).length > 0 && Object.keys(this.factories).length > 0;
    }

    /**
     * Get Component-IDs of Component instances by the given Factory.
     *
     * @param factoryId the Factory PID.
     */
    public getComponentIdsByFactory(factoryId: string): string[] {
        const factory = this.factories[factoryId];
        if (factory) {
            return factory.componentIds;
        } else {
            return [];
        }
    }

    public getFactoriesByNature(natureId: string): EdgeConfig.Factory[] {
        return EdgeConfig.getFactoriesByNature(this.factories, natureId);
    }

    /**
     * Get Factories by Factory-IDs.
     *
     * @param ids the given Factory-IDs.
     */
    public getFactoriesByIds(factoryIds: string[]): EdgeConfig.Factory[] {
        return EdgeConfig.getFactoriesByIds(this.factories, factoryIds);
    }

    /**
     * Get Factories by Factory-IDs pattern.
     *
     * @param ids the given Factory-IDs pattern.
     */
    public getFactoriesByIdsPattern(patterns: RegExp[]): EdgeConfig.Factory[] {
        return EdgeConfig.getFactoriesByIdsPattern(this.factories, patterns);
    }

    /**
     * Get Component instances by the given Factory.
     *
     * @param factoryId the Factory PID.
     */
    public getComponentsByFactory(factoryId: string): EdgeConfig.Component[] {
        const componentIds = this.getComponentIdsByFactory(factoryId);
        const result: EdgeConfig.Component[] = [];
        for (const componentId of componentIds) {
            result.push(this.components[componentId]);
        }
        return result;
    }

    /**
     * Gets the Component Ids by the given Factories.
     *
     * @param factoryIds the Factory PIDs.
     * @returns the component Ids
     */
    public getComponentIdsByFactories(...factoryIds: string[]): string[] {
        const componentIds: string[] = [];

        for (const factory of factoryIds) {
            componentIds.push(...this.getComponentIdsByFactory(factory));
        }
        return componentIds;
    }

    /**
     * Get Component-IDs of Components that implement the given Nature.
     *
     * @param nature the given Nature.
     */
    public getComponentIdsImplementingNature(natureId: string): string[] {
        const result: string[] = [];
        const nature = this.natures[natureId];
        if (nature) {
            for (const factoryId of nature.factoryIds) {
                result.push(...this.getComponentIdsByFactory(factoryId));
            }
        }

        // Backwards compatibilty
        // TODO drop after full migration to ElectricityMeter
        switch (natureId) {
            // ElectricityMeter replaces SymmetricMeter (and AsymmetricMeter implicitely)
            case "io.openems.edge.meter.api.ElectricityMeter":
                result.push(...this.getComponentIdsImplementingNature("io.openems.edge.meter.api.SymmetricMeter"));
        }

        return result;
    }

    /**
     * Get Components that implement the given Nature.
     *
     * @param nature the given Nature.
     */
    public getComponentsImplementingNature(natureId: string): EdgeConfig.Component[] {
        const result: EdgeConfig.Component[] = [];
        const nature = this.natures[natureId];
        if (nature) {
            for (const factoryId of nature.factoryIds) {
                result.push(...this.getComponentsByFactory(factoryId));
            }
        }

        // Backwards compatibilty
        // TODO drop after full migration to ElectricityMeter
        switch (natureId) {
            // ElectricityMeter replaces SymmetricMeter (and AsymmetricMeter implicitely)
            case "io.openems.edge.meter.api.ElectricityMeter":
                result.push(...this.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter"));
        }

        return result;
    }

    /**
     * Get the implemented NatureIds by Factory-ID.
     *
     * @param factoryId the Factory-ID
     */
    public getNatureIdsByFactoryId(factoryId: string): string[] {
        const factory = this.factories[factoryId];
        if (factory) {
            return factory.natureIds;
        } else {
            return [];
        }
    }

    /**
     * Get a component by another components property
     *
     * @param otherComponentId the other component
     * @param property the property of the other component
     * @returns the component, if found, else null
     */
    public getComponentFromOtherComponentsProperty(otherComponentId: string, property: string): EdgeConfig.Component | null {
        const component = this.components[otherComponentId];
        if (component && property in component.properties) {
            const id = component.properties[property];
            return this.components[id];
        } else {
            return null;
        }
    }

    /**
     * Determines if component has nature
     *
     * @param nature the given Nature.
     * @param componentId the Component-ID
     */
    public hasComponentNature(nature: string, componentId: string) {
        const natureIds = this.getNatureIdsByComponentId(componentId);
        return natureIds.includes(nature);
    }

    /**
     * Determines if component has factory id
     *
     * @param nature the given Nature.
     * @param componentId the Component-ID
     */
    public hasComponentFactory(factoryId: string, component: EdgeConfig.Component) {
        return component.factoryId === factoryId;
    }

    /**
     * Determines if Edge has a Storage device
     */
    public hasStorage(): boolean {
        if (this.getComponentIdsImplementingNature("io.openems.edge.ess.api.SymmetricEss").length > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if Edge has a Meter device
     */
    public hasMeter(): boolean {
        if (this.getComponentIdsImplementingNature("io.openems.edge.meter.api.ElectricityMeter").length > 0) {
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
        if (this.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").length > 0) {
            return true;
        }
        // Do we have a Meter with type PRODUCTION?
        for (const component of this.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")) {
            if (component.isEnabled && this.isProducer(component)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is the given Meter of type 'PRODUCTION'?
     *
     * @param component the Meter Component
     * @returns true for PRODUCTION
     */
    public isProducer(component: EdgeConfig.Component) {
        if (component.properties["type"] == "PRODUCTION") {
            return true;
        }
        const natureIds = this.getNatureIdsByFactoryId(component.factoryId);
        if (natureIds.includes("io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter")
            || natureIds.includes("io.openems.edge.ess.dccharger.api.EssDcCharger")) {
            return true;
        }
        // TODO properties in OSGi Component annotations are not transmitted correctly with Apache Felix SCR
        switch (component.factoryId) {
            case "Fenecon.Dess.PvMeter":
            case "Fenecon.Mini.PvMeter":
            case "Fenecon.Pro.PvMeter":
            case "Simulator.ProductionMeter.Acting":
                return true;
        }
        return false;
    }

    /**
     * Is the given Meter of type 'CONSUMPTION_METERED'?
     *
     * @param component the Meter Component
     * @returns true for CONSUMPTION_METERED
     */
    public isTypeConsumptionMetered(component: EdgeConfig.Component) {

        if (component.properties["type"] == "CONSUMPTION_METERED") {
            return true;
        }

        switch (component.factoryId) {
            case "GoodWe.EmergencyPowerMeter":
            case "Controller.IO.Heating.Room":
                return true;
        }
        const natures = this.getNatureIdsByFactoryId(component.factoryId);
        if (natures.includes("io.openems.edge.evcs.api.Evcs") && !natures.includes("io.openems.edge.evcs.api.MetaEvcs")) {
            return true;
        }
        if (natures.includes("io.openems.edge.evse.api.chargepoint.EvseChargePoint")) {
            return true;
        }
        return false;
    }

    /**
     * Is the given Meter of type 'GRID'?
     *
     * @param component the Meter Component
     * @returns true for GRID
     */
    public isTypeGrid(component: EdgeConfig.Component) {
        if (component.properties["type"] == "GRID") {
            return true;
        }

        switch (component.factoryId) {
            case "GoodWe.Grid-Meter":
            case "Kaco.BlueplanetHybrid10.GridMeter":
            case "Fenecon.Dess.GridMeter":
            case "Fenecon.Mini.GridMeter":
            case "Kostal.Piko.GridMeter":
            case "SolarEdge.Grid-Meter":
            case "Simulator.GridMeter.Acting":
            case "Simulator.GridMeter.Reacting":
                return true;
        }
        return false;
    }

    public listAvailableFactories(translate: TranslateService): CategorizedFactories[] {
        return EdgeConfig.listAvailableFactories(this.factories, translate);
    }

    /**
     * Returns the corresponding icon for a given factory
     */
    public getFactoryIcon(factory: EdgeConfig.Factory, translate: TranslateService): string {
        // default icon, if no icons are found
        let result = "stats-chart-outline";
        this.listAvailableFactories(translate).forEach(availableFactories => {
            availableFactories.factories.forEach(availableFactory => {
                if (factory == availableFactory) {
                    result = availableFactories.category.icon;
                }
            });
        });
        return result;
    }

    /**
     * Lists all active Components, grouped by category.
     */
    public listActiveComponents(ignoreComponentIds: string[] = [], translate: TranslateService): CategorizedComponents[] {
        const allComponents = [];
        const factories = this.listAvailableFactories(translate);
        for (const entry of factories) {
            const components: EdgeConfig.Component[] = [];
            for (const factory of entry.factories) {
                components.push(...this.getComponentsByFactory(factory.id));
            }
            allComponents.push({
                category: entry.category,
                components: components,
            });
        }
        const result: CategorizedComponents[] = [];
        allComponents.forEach((item: CategorizedComponents) => {
            const components =
                item.components
                    // remove Components from list that have already been listed before
                    .filter(component => !ignoreComponentIds.includes(component.id))
                    // remove duplicates
                    .filter((e, i, arr) => arr.indexOf(e) === i)
                    // sort by ID
                    .sort((c1, c2) => c1.id.localeCompare(c2.id));
            if (components.length > 0) {
                components.forEach(component => {
                    ignoreComponentIds.push(component.id);
                });
                // item.category.title = translate.instant()
                result.push({ category: item.category, components: components });
            }
        });
        return result;
    }

    /**
     * Get the implemented Natures by Component-ID.
     *
     * @param componentId the Component-ID
     */
    public getNatureIdsByComponentId(componentId: string): string[] {
        const component = this.components[componentId];
        if (!component) {
            return [];
        }
        const factoryId = component.factoryId;
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
        const component = this.components[componentId];
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
    public getChannel(address: ChannelAddress): EdgeConfig.ComponentChannel | null {
        const component = this.components[address.componentId];
        if (component?.channels) {
            return component.channels[address.channelId];
        } else {
            return null;
        }
    }

    /**
     * Safely gets a property from a component, if it exists, else returns null.
     *
     * @param component The component from which to retrieve the property.
     * @param property The property name to retrieve.
     * @returns The property value if it exists, otherwise null.
     */
    public getPropertyFromComponent<T>(component: EdgeConfig.Component | null, property: string): T | null {
        return component?.properties[property] ?? null;
    }

    /**
     * Safely gets a property from a component, if it exists, else returns false.
     *
     * @param component The component from which to retrieve the property.
     * @param property The property name to retrieve.
     * @returns The property value if it exists, otherwise null.
     */
    public hasComponentPropertyValue<T>(component: EdgeConfig.Component | null, property: string, value: T): boolean {
        if (component == null) {
            return false;
        }
        return component.hasPropertyValue<T>(property, value);
    }
}

export enum PersistencePriority {
    VERY_LOW = "VERY_LOW", //
    LOW = "LOW", //
    MEDIUM = "MEDIUM", //
    HIGH = "HIGH", //
    VERY_HIGH = "VERY_HIGH", //
}

export namespace PersistencePriority {

    export const DEFAULT_CHANNEL_PRIORITY: string = PersistencePriority.VERY_LOW;
    export const DEFAULT_GLOBAL_PRIORITY: string = PersistencePriority.HIGH;

    /**
     * Checks if given prio1 is less than prio2
     *
     * @param prio1 the prio that will be compared
     * @param prio2 the prio to compare it to
     * @returns true if prio1 is less than prio2
     */
    export function isLessThan(prio1: string, prio2: string): boolean {
        if (typeof prio1 !== "string" || typeof prio2 !== "string") {
            return false;
        }
        return Object.keys(PersistencePriority).indexOf(prio1) < Object.keys(PersistencePriority).indexOf(prio2);
    }
}

export namespace EdgeConfig {
    export class ComponentChannel {
        public readonly type!: "BOOLEAN" | "SHORT" | "INTEGER" | "LONG" | "FLOAT" | "DOUBLE" | "STRING";
        public readonly accessMode!: "RO" | "RW" | "WO";
        public readonly unit!: string;
        public readonly category!: "OPENEMS_TYPE" | "ENUM" | "STATE";
        public readonly level!: "INFO" | "OK" | "WARNING" | "FAULT";
        public readonly persistencePriority?: PersistencePriority;
        public readonly text!: string;
        public readonly options?: { [key: string]: number };
    }

    export class Component {
        public id: string = "";
        public alias: string = "";
        public isEnabled: boolean = false;

        constructor(
            public readonly factoryId: string = "",
            public readonly properties: { [key: string]: any } = {},
            public readonly channels?: { [channelId: string]: ComponentChannel },
        ) { }

        public static of(component: EdgeConfig.Component) {
            return new EdgeConfig.Component(component.factoryId, component.properties, component.channels ?? {});
        }

        /* Safely gets a property from a component, if it exists, else returns null.
        *
        * @param component The component from which to retrieve the property.
        * @param property The property name to retrieve.
        * @returns The property value if it exists, otherwise null.
        */
        public getPropertyFromComponent<T>(property: string): T | null {
            return this.properties[property] ?? null;
        }

        /**
         * Checks if property has a given value
         *
         *@param propertyName - The name of the property to check.
         *@param value - The value to compare against.
         *@returns True if the property exists and has the given value; otherwise, false.
         */
        public hasPropertyValue<T>(propertyName: string, value: T): boolean {
            const propertyValue = this.getPropertyFromComponent<T>(propertyName);
            if (!propertyValue) {
                return false;
            }
            return propertyValue === value;
        }
    }

    export class FactoryProperty {
        public readonly id!: string;
        public readonly name!: string;
        public readonly description!: string;
        public readonly type!: string;
        public readonly isRequired!: boolean;
        public readonly isPassword!: boolean;
        public readonly defaultValue!: any;
        public readonly schema!: {};
    }


    export class Factory {
        public id: string = "";
        public componentIds: string[] = [];

        constructor(
            public readonly name: string,
            public readonly description: string,
            public readonly natureIds: string[] = [],
            public readonly properties: FactoryProperty[] = [],
        ) { }

        /**
         * Gets the FactoryProperty definition for a Property-ID.
         *
         * @param propertyId the Property-ID
         */
        static getPropertyForId(factory: Factory, propertyId: string): FactoryProperty | null {
            for (const property of factory.properties) {
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
