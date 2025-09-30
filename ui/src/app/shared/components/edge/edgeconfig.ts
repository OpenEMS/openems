import { TranslateService } from "@ngx-translate/core";
import { ChannelAddress, Widgets } from "../../shared";
import { Edge } from "./edge";

export interface CategorizedComponents {
    category: {
        title: string,
        icon: string,
    },
    components: EDGE_CONFIG.COMPONENT[]
}

export interface CategorizedFactories {
    category: {
        title: string,
        icon: string
    },
    factories: EDGE_CONFIG.FACTORY[]
}

export class EdgeConfig {

    /**
     * Component-ID -> Component.
     */
    public readonly components: { [id: string]: EDGE_CONFIG.COMPONENT } = {};

    /**
     * Factory-PID -> OSGi Factory.
     */
    public readonly factories: { [id: string]: EDGE_CONFIG.FACTORY } = {};

    /**
     * Nature-PID -> Component-IDs.
     */
    public readonly natures: { [id: string]: EDGE_CONFIG.NATURE } = {};

    /**
     * UI-Widgets.
     */
    public readonly widgets: Widgets;

    constructor(edge: Edge, source?: EdgeConfig) {

        if (source) {
            THIS.COMPONENTS = OBJECT.ENTRIES(SOURCE.COMPONENTS).reduce((obj, [k, v]) => {
                const component = EDGE_CONFIG.COMPONENT.OF(v);
                if (component == null) {
                    return obj;
                }

                obj[k] = component;
                return obj;
            }, {} as { [id: string]: EDGE_CONFIG.COMPONENT });

            THIS.FACTORIES = SOURCE.FACTORIES;
        }

        // initialize Components
        for (const componentId in THIS.COMPONENTS) {
            const component = THIS.COMPONENTS[componentId];
            COMPONENT.ID = componentId;
            if ("enabled" in COMPONENT.PROPERTIES) {
                COMPONENT.IS_ENABLED = COMPONENT.PROPERTIES["enabled"];
            } else {
                COMPONENT.IS_ENABLED = true;
            }
        }

        // initialize Factorys
        for (const factoryId in THIS.FACTORIES) {
            const factory = THIS.FACTORIES[factoryId];
            FACTORY.ID = factoryId;
            FACTORY.COMPONENT_IDS = [];

            // Fill 'natures' map
            for (const natureId of FACTORY.NATURE_IDS) {
                if (!(natureId in THIS.NATURES)) {
                    const parts = NATURE_ID.SPLIT(".");
                    const name = parts[PARTS.LENGTH - 1];
                    THIS.NATURES[natureId] = {
                        id: natureId,
                        name: name,
                        factoryIds: [],
                    };
                }
                THIS.NATURES[natureId].FACTORY_IDS.PUSH(factoryId);
            }
        }

        if (OBJECT.KEYS(THIS.COMPONENTS).length != 0 && OBJECT.KEYS(THIS.FACTORIES).length == 0) {
            CONSOLE.WARN("Factory definitions are missing.");
        } else {
            for (const componentId in THIS.COMPONENTS) {
                const component = THIS.COMPONENTS[componentId];
                if (COMPONENT.FACTORY_ID === "") {
                    continue; // Singleton components have no factory-PID
                }
                const factory = THIS.FACTORIES[COMPONENT.FACTORY_ID];
                if (!factory) {
                    CONSOLE.WARN("Factory definition [" + COMPONENT.FACTORY_ID + "] for [" + componentId + "] is missing.");
                    continue;
                }

                // Complete 'factories' map
                FACTORY.COMPONENT_IDS.PUSH(componentId);
            }
        }

        // Initialize Widgets
        THIS.WIDGETS = WIDGETS.PARSE_WIDGETS(edge, this);
    }

    /**
     * Lists all available Factories, grouped by category.
     */
    public static listAvailableFactories(factories: { [id: string]: EDGE_CONFIG.FACTORY }, translate: TranslateService): CategorizedFactories[] {
        const allFactories: CategorizedFactories[] = [
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.SIMULATORS"), icon: "flask-outline" },
                factories: OBJECT.ENTRIES(factories)
                    .filter(([factory]) => FACTORY.STARTS_WITH("Simulator."))
                    .map(e => e[1]),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.METER"), icon: "speedometer-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.METER.API.SYMMETRIC_METER"), // TODO replaced by ElectricityMeter
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER", "IO.OPENEMS.EDGE.EVCS.API.EVCS"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER"),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.STORAGE_SYSTEMS"), icon: "battery-charging-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.BATTERY.API.BATTERY"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.BATTERYINVERTER.API.MANAGED_SYMMETRIC_BATTERY_INVERTER"),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.STORAGE_SYSTEM_CONTROL"), icon: "options-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS_PATTERN(factories, [
                        /Controller\.Asymmetric.*/,
                        /Controller\.Ess.*/,
                        /Controller\.Symmetric.*/,
                    ]),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.EV_CHARGING_STATION"), icon: "car-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.EVCS.API.EVCS"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.EVSE.API.CHARGEPOINT.EVSE_CHARGE_POINT"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.EVSE.API.ELECTRICVEHICLE.EVSE_ELECTRIC_VEHICLE"),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.EV_CHARGING_STATION_CONTROL"), icon: "options-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS(factories, [
                        "CONTROLLER.EVCS",
                        "EVSE.CONTROLLER.SINGLE",
                        "EVSE.CONTROLLER.CLUSTER",
                    ]),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.IOS"), icon: "log-in-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.IO.API.DIGITAL_OUTPUT"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.IO.API.DIGITAL_INPUT"),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.IO-CONTROL"), icon: "options-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS(factories, [
                        "CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD",
                        "CONTROLLER.IO.FIX_DIGITAL_OUTPUT",
                        "CONTROLLER.IO.HEATING_ELEMENT",
                        "CONTROLLER.HEAT.HEATINGELEMENT",
                        "CONTROLLER.IO.HEATING.ROOM",
                        "CONTROLLER.IO.HEAT_PUMP.SG_READY",
                    ]),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.TEMPERATURE_SENSORS"), icon: "thermometer-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.THERMOMETER.API.THERMOMETER"),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.EXTERNAL_INTERFACES"), icon: "megaphone-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS(factories, [
                        "CONTROLLER.API.WEBSOCKET",
                        "CONTROLLER.API.MODBUS_TCP",
                        "CONTROLLER.API.MODBUS_TCP.READ_ONLY",
                        "CONTROLLER.API.MODBUS_TCP.READ_WRITE",
                        "CONTROLLER.API.MODBUS_RTU.READ_ONLY",
                        "CONTROLLER.API.MODBUS_RTU.READ_WRITE",
                        "CONTROLLER.API.MQTT",
                        "CONTROLLER.API.REST.READ_ONLY",
                        "CONTROLLER.API.REST.READ_WRITE",
                    ]),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.CLOUD_INTERFACES"), icon: "cloud-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS_PATTERN(factories, [
                        /TimeOfUseTariff\.*/,
                    ]),
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS(factories, [
                        "CONTROLLER.API.BACKEND",
                        "CONTROLLER.CLEVER-PV",
                    ]),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.DEVICE_INTERFACES"), icon: "swap-horizontal-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS(factories, [
                        "BRIDGE.MBUS",
                        "BRIDGE.ONEWIRE",
                        "BRIDGE.MODBUS.SERIAL",
                        "BRIDGE.MODBUS.TCP",
                        "KACO.BLUEPLANET_HYBRID10.CORE",
                    ]),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.STANDARD_COMPONENTS"), icon: "resize-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_IDS(factories, [
                        "CONTROLLER.DEBUG.LOG",
                        "CONTROLLER.DEBUG.DETAILED_LOG",
                    ]),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.PREDICTOR.API.ONEDAY.PREDICTOR24_HOURS"),
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.SCHEDULER.API.SCHEDULER"),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.DEDICATED_CONTROLLERS"), icon: "repeat-outline" },
                factories: [
                    EDGE_CONFIG.GET_FACTORIES_BY_NATURE(factories, "IO.OPENEMS.EDGE.CONTROLLER.API.CONTROLLER"),
                ].flat(2),
            },
            {
                category: { title: TRANSLATE.INSTANT("SETTINGS.CATEGORY.TITLE.OTHERS"), icon: "radio-button-off-outline" },
                factories: OBJECT.VALUES(factories),
            },
        ];

        const ignoreFactoryIds: string[] = [];
        const result: CategorizedFactories[] = [];
        ALL_FACTORIES.FOR_EACH(item => {
            const factories =
                ITEM.FACTORIES
                    // remove Factories from list that have already been listed before
                    .filter(factory => !IGNORE_FACTORY_IDS.INCLUDES(FACTORY.ID))
                    // remove duplicates
                    .filter((e, i, arr) => ARR.INDEX_OF(e) === i);
            if (FACTORIES.LENGTH > 0) {
                FACTORIES.FOR_EACH(factory => {
                    IGNORE_FACTORY_IDS.PUSH(FACTORY.ID);
                });
                RESULT.PUSH({ category: ITEM.CATEGORY, factories: FACTORIES.SORT((a, b) => A.ID.LOCALE_COMPARE(B.ID)) });
            }
        });
        return result;
    }

    /**
     * Get Factories of Nature.
     *
     * @param factories the given EDGE_CONFIG.FACTORY
     * @param includeNature the name of the Nature to be included
     * @param excludeNature an optional name of a Nature to be excluded
     */
    public static getFactoriesByNature(factories: { [id: string]: EDGE_CONFIG.FACTORY }, includeNature: string, excludeNature?: string): EDGE_CONFIG.FACTORY[] {
        const result = [];
        const natures = EDGE_CONFIG.GET_NATURES_OF_FACTORIES(factories);
        const include = natures[includeNature];
        const excludes = excludeNature != null && excludeNature in natures ? natures[excludeNature].factoryIds : [];
        if (include) {
            for (const factoryId of INCLUDE.FACTORY_IDS) {
                if (EXCLUDES.INCLUDES(factoryId)) {
                    continue;
                }
                if (factoryId in factories) {
                    RESULT.PUSH(factories[factoryId]);
                }
            }
        }
        return result;
    }

    public static getFactoriesByIds(factories: { [id: string]: EDGE_CONFIG.FACTORY }, factoryIds: string[]): EDGE_CONFIG.FACTORY[] {
        const result = [];
        for (const factoryId of factoryIds) {
            if (factoryId in factories) {
                RESULT.PUSH(factories[factoryId]);
            }
        }
        return result;
    }

    public static getFactoriesByIdsPattern(factories: { [id: string]: EDGE_CONFIG.FACTORY }, patterns: RegExp[]): EDGE_CONFIG.FACTORY[] {
        const result = [];
        for (const pattern of patterns) {
            for (const factoryId in factories) {
                if (PATTERN.TEST(factoryId)) {
                    RESULT.PUSH(factories[factoryId]);
                }
            }
        }
        return result;
    }

    public static getNaturesOfFactories(factories: { [id: string]: EDGE_CONFIG.FACTORY }): { [natureId: string]: EDGE_CONFIG.NATURE } {
        const natures: { [natureId: string]: EDGE_CONFIG.NATURE } = {};
        // initialize Factorys
        for (const [factoryId, factory] of OBJECT.ENTRIES(factories)) {
            // Fill 'natures' map
            for (const natureId of FACTORY.NATURE_IDS) {
                if (!(natureId in natures)) {
                    const parts = NATURE_ID.SPLIT(".");
                    const name = parts[PARTS.LENGTH - 1];
                    natures[natureId] = {
                        id: natureId,
                        name: name,
                        factoryIds: [],
                    };
                }
                natures[natureId].FACTORY_IDS.PUSH(factoryId);
            }
        }
        return natures;
    }

    public isValid(): boolean {
        return OBJECT.KEYS(THIS.COMPONENTS).length > 0 && OBJECT.KEYS(THIS.FACTORIES).length > 0;
    }

    /**
     * Get Component-IDs of Component instances by the given Factory.
     *
     * @param factoryId the Factory PID.
     */
    public getComponentIdsByFactory(factoryId: string): string[] {
        const factory = THIS.FACTORIES[factoryId];
        if (factory) {
            return FACTORY.COMPONENT_IDS;
        } else {
            return [];
        }
    }

    public getFactoriesByNature(natureId: string): EDGE_CONFIG.FACTORY[] {
        return EDGE_CONFIG.GET_FACTORIES_BY_NATURE(THIS.FACTORIES, natureId);
    }

    /**
     * Get Factories by Factory-IDs.
     *
     * @param ids the given Factory-IDs.
     */
    public getFactoriesByIds(factoryIds: string[]): EDGE_CONFIG.FACTORY[] {
        return EDGE_CONFIG.GET_FACTORIES_BY_IDS(THIS.FACTORIES, factoryIds);
    }

    /**
     * Get Factories by Factory-IDs pattern.
     *
     * @param ids the given Factory-IDs pattern.
     */
    public getFactoriesByIdsPattern(patterns: RegExp[]): EDGE_CONFIG.FACTORY[] {
        return EDGE_CONFIG.GET_FACTORIES_BY_IDS_PATTERN(THIS.FACTORIES, patterns);
    }

    /**
     * Get Component instances by the given Factory.
     *
     * @param factoryId the Factory PID.
     */
    public getComponentsByFactory(factoryId: string): EDGE_CONFIG.COMPONENT[] {
        const componentIds = THIS.GET_COMPONENT_IDS_BY_FACTORY(factoryId);
        const result: EDGE_CONFIG.COMPONENT[] = [];
        for (const componentId of componentIds) {
            RESULT.PUSH(THIS.COMPONENTS[componentId]);
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
            COMPONENT_IDS.PUSH(...THIS.GET_COMPONENT_IDS_BY_FACTORY(factory));
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
        const nature = THIS.NATURES[natureId];
        if (nature) {
            for (const factoryId of NATURE.FACTORY_IDS) {
                RESULT.PUSH(...THIS.GET_COMPONENT_IDS_BY_FACTORY(factoryId));
            }
        }

        // Backwards compatibilty
        // TODO drop after full migration to ElectricityMeter
        switch (natureId) {
            // ElectricityMeter replaces SymmetricMeter (and AsymmetricMeter implicitely)
            case "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER":
                RESULT.PUSH(...THIS.GET_COMPONENT_IDS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.SYMMETRIC_METER"));
        }

        return result;
    }

    /**
     * Get Components that implement the given Nature.
     *
     * @param nature the given Nature.
     */
    public getComponentsImplementingNature(natureId: string): EDGE_CONFIG.COMPONENT[] {
        const result: EDGE_CONFIG.COMPONENT[] = [];
        const nature = THIS.NATURES[natureId];
        if (nature) {
            for (const factoryId of NATURE.FACTORY_IDS) {
                RESULT.PUSH(...THIS.GET_COMPONENTS_BY_FACTORY(factoryId));
            }
        }

        // Backwards compatibilty
        // TODO drop after full migration to ElectricityMeter
        switch (natureId) {
            // ElectricityMeter replaces SymmetricMeter (and AsymmetricMeter implicitely)
            case "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER":
                RESULT.PUSH(...THIS.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.SYMMETRIC_METER"));
        }

        return result;
    }

    /**
     * Get the implemented NatureIds by Factory-ID.
     *
     * @param factoryId the Factory-ID
     */
    public getNatureIdsByFactoryId(factoryId: string): string[] {
        const factory = THIS.FACTORIES[factoryId];
        if (factory) {
            return FACTORY.NATURE_IDS;
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
    public getComponentFromOtherComponentsProperty(otherComponentId: string, property: string): EDGE_CONFIG.COMPONENT | null {
        const component = THIS.COMPONENTS[otherComponentId];
        if (component && property in COMPONENT.PROPERTIES) {
            const id = COMPONENT.PROPERTIES[property];
            return THIS.COMPONENTS[id];
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
        const natureIds = THIS.GET_NATURE_IDS_BY_COMPONENT_ID(componentId);
        return NATURE_IDS.INCLUDES(nature);
    }

    /**
     * Determines if component has factory id
     *
     * @param nature the given Nature.
     * @param componentId the Component-ID
     */
    public hasComponentFactory(factoryId: string, component: EDGE_CONFIG.COMPONENT) {
        return COMPONENT.FACTORY_ID === factoryId;
    }

    /**
     * Determines if component has at least one of the given factory ids
     *
     * @param factoryIds the given factory ids.
     * @returns true, if at least one of the passed factory ids, exists in config
     */
    public hasFactories(factoryIds: string[]): boolean {
        return OBJECT.ENTRIES(THIS.COMPONENTS).some(([id, component]) => FACTORY_IDS.INCLUDES(COMPONENT.FACTORY_ID));
    }

    /**
     * Determines if Edge has a Storage device
     */
    public hasStorage(): boolean {
        if (THIS.GET_COMPONENT_IDS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS").length > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if Edge has a Meter device
     */
    public hasMeter(): boolean {
        if (THIS.GET_COMPONENT_IDS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER").length > 0) {
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
        if (THIS.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER").length > 0) {
            return true;
        }
        // Do we have a Meter with type PRODUCTION?
        for (const component of THIS.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")) {
            if (COMPONENT.IS_ENABLED && THIS.IS_PRODUCER(component)) {
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
    public isProducer(component: EDGE_CONFIG.COMPONENT) {
        if (COMPONENT.PROPERTIES["type"] == "PRODUCTION") {
            return true;
        }
        const natureIds = THIS.GET_NATURE_IDS_BY_FACTORY_ID(COMPONENT.FACTORY_ID);
        if (NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.PVINVERTER.API.MANAGED_SYMMETRIC_PV_INVERTER")
            || NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER")) {
            return true;
        }
        // TODO properties in OSGi Component annotations are not transmitted correctly with Apache Felix SCR
        switch (COMPONENT.FACTORY_ID) {
            case "FENECON.DESS.PV_METER":
            case "FENECON.MINI.PV_METER":
            case "FENECON.PRO.PV_METER":
            case "SIMULATOR.PRODUCTION_METER.ACTING":
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
    public isTypeConsumptionMetered(component: EDGE_CONFIG.COMPONENT) {

        if (COMPONENT.PROPERTIES["type"] == "CONSUMPTION_METERED") {
            return true;
        }

        switch (COMPONENT.FACTORY_ID) {
            case "GOOD_WE.EMERGENCY_POWER_METER":
            case "CONTROLLER.IO.HEATING.ROOM":
                return true;
        }
        const natures = THIS.GET_NATURE_IDS_BY_FACTORY_ID(COMPONENT.FACTORY_ID);
        if (NATURES.INCLUDES("IO.OPENEMS.EDGE.EVCS.API.EVCS") && !NATURES.INCLUDES("IO.OPENEMS.EDGE.EVCS.API.META_EVCS")) {
            return true;
        }
        if (NATURES.INCLUDES("IO.OPENEMS.EDGE.EVSE.API.CHARGEPOINT.EVSE_CHARGE_POINT")) {
            return true;
        }
        if (NATURES.INCLUDES("IO.OPENEMS.EDGE.HEAT.API.HEAT")) {
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
    public isTypeGrid(component: EDGE_CONFIG.COMPONENT) {
        if (COMPONENT.PROPERTIES["type"] == "GRID") {
            return true;
        }

        switch (COMPONENT.FACTORY_ID) {
            case "GOOD_WE.GRID-Meter":
            case "KACO.BLUEPLANET_HYBRID10.GRID_METER":
            case "FENECON.DESS.GRID_METER":
            case "FENECON.MINI.GRID_METER":
            case "KOSTAL.PIKO.GRID_METER":
            case "SOLAR_EDGE.GRID-Meter":
            case "SIMULATOR.GRID_METER.ACTING":
            case "SIMULATOR.GRID_METER.REACTING":
                return true;
        }
        return false;
    }

    public listAvailableFactories(translate: TranslateService): CategorizedFactories[] {
        return EDGE_CONFIG.LIST_AVAILABLE_FACTORIES(THIS.FACTORIES, translate);
    }

    /**
     * Returns the corresponding icon for a given factory
     */
    public getFactoryIcon(factory: EDGE_CONFIG.FACTORY, translate: TranslateService): string {
        // default icon, if no icons are found
        let result = "stats-chart-outline";
        THIS.LIST_AVAILABLE_FACTORIES(translate).forEach(availableFactories => {
            AVAILABLE_FACTORIES.FACTORIES.FOR_EACH(availableFactory => {
                if (factory == availableFactory) {
                    result = AVAILABLE_FACTORIES.CATEGORY.ICON;
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
        const factories = THIS.LIST_AVAILABLE_FACTORIES(translate);
        for (const entry of factories) {
            const components: EDGE_CONFIG.COMPONENT[] = [];
            for (const factory of ENTRY.FACTORIES) {
                COMPONENTS.PUSH(...THIS.GET_COMPONENTS_BY_FACTORY(FACTORY.ID));
            }
            ALL_COMPONENTS.PUSH({
                category: ENTRY.CATEGORY,
                components: components,
            });
        }
        const result: CategorizedComponents[] = [];
        ALL_COMPONENTS.FOR_EACH((item: CategorizedComponents) => {
            const components =
                ITEM.COMPONENTS
                    // remove Components from list that have already been listed before
                    .filter(component => !IGNORE_COMPONENT_IDS.INCLUDES(COMPONENT.ID))
                    // remove duplicates
                    .filter((e, i, arr) => ARR.INDEX_OF(e) === i)
                    // sort by ID
                    .sort((c1, c2) => C1.ID.LOCALE_COMPARE(C2.ID));
            if (COMPONENTS.LENGTH > 0) {
                COMPONENTS.FOR_EACH(component => {
                    IGNORE_COMPONENT_IDS.PUSH(COMPONENT.ID);
                });
                // ITEM.CATEGORY.TITLE = TRANSLATE.INSTANT()
                RESULT.PUSH({ category: ITEM.CATEGORY, components: components });
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
        const component = THIS.COMPONENTS[componentId];
        if (!component) {
            return [];
        }
        const factoryId = COMPONENT.FACTORY_ID;
        return THIS.GET_NATURE_IDS_BY_FACTORY_ID(factoryId);
    }

    /**
     * Get the Component.
     *
     * @param componentId the Component-ID
     */
    public getComponent(componentId: string): EDGE_CONFIG.COMPONENT {
        return THIS.COMPONENTS[componentId];
    }

    /**
     * Get the Component properties.
     *
     * @param componentId the Component-ID
     */
    public getComponentProperties(componentId: string): { [key: string]: any } {
        const component = THIS.COMPONENTS[componentId];
        if (component) {
            return COMPONENT.PROPERTIES;
        } else {
            return {};
        }
    }

    /**
     * Get Channel.
     *
     * @param address the ChannelAddress
     */
    public getChannel(address: ChannelAddress): EDGE_CONFIG.COMPONENT_CHANNEL | null {
        const component = THIS.COMPONENTS[ADDRESS.COMPONENT_ID];
        if (component?.channels) {
            return COMPONENT.CHANNELS[ADDRESS.CHANNEL_ID];
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
    public getPropertyFromComponent<T>(component: EDGE_CONFIG.COMPONENT | null, property: string): T | null {
        return component?.properties[property] ?? null;
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
    export const DEFAULT_GLOBAL_PRIORITY: string = PERSISTENCE_PRIORITY.HIGH;

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
        return OBJECT.KEYS(PersistencePriority).indexOf(prio1) < OBJECT.KEYS(PersistencePriority).indexOf(prio2);
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
        constructor(
            public id: string = "",
            public alias: string = "",
            public isEnabled: boolean = false,
            public readonly factoryId: string = "",
            public readonly properties: { [key: string]: any } = {},
            public readonly channels?: { [channelId: string]: ComponentChannel },
        ) { }

        public static of(component: EDGE_CONFIG.COMPONENT | null): EDGE_CONFIG.COMPONENT | null {
            if (component == null) {
                return null;
            }
            return new EDGE_CONFIG.COMPONENT(COMPONENT.ID, COMPONENT.ALIAS, COMPONENT.IS_ENABLED, COMPONENT.FACTORY_ID, COMPONENT.PROPERTIES, COMPONENT.CHANNELS ?? {});
        }

        /* Safely gets a property from a component, if it exists, else returns null.
        *
        * @param component The component from which to retrieve the property.
        * @param property The property name to retrieve.
        * @returns The property value if it exists, otherwise null.
        */
        public getPropertyFromComponent<T>(property: string): T | null {
            return THIS.PROPERTIES[property] ?? null;
        }

        /**
         * Checks if property has a given value
         *
         *@param propertyName - The name of the property to check.
         *@param value - The value to compare against.
         *@returns True if the property exists and has the given value; otherwise, false.
         */
        public hasPropertyValue<T>(propertyName: string, value: T): boolean {
            const propertyValue = THIS.GET_PROPERTY_FROM_COMPONENT<T>(propertyName);
            if (!propertyValue) {
                return false;
            }

            if (typeof value === "boolean" && typeof propertyValue === "string") {
                return PROPERTY_VALUE.TO_LOWER_CASE() === String(value);
            }

            if (typeof value === "string" && typeof propertyValue === "boolean") {
                return String(propertyValue) === VALUE.TO_LOWER_CASE();
            }

            if (typeof value === "number" && typeof propertyValue === "string") {
                return Number(propertyValue) === value;
            }

            if (typeof value === "string" && typeof propertyValue === "number") {
                return propertyValue === Number(value);
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
            for (const property of FACTORY.PROPERTIES) {
                if (PROPERTY.ID === propertyId) {
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
