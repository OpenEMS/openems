import { TranslateService } from "@ngx-translate/core";
import { SharedConsumption } from "src/app/edge/live/common/consumption/shared/shared";
import { SharedGrid } from "src/app/edge/live/common/grid/shared/shared";
import { SharedProduction } from "src/app/edge/live/common/production/shared/shared";
import { SharedStorage } from "src/app/edge/live/common/storage/shared/shared";
import { SharedWeather } from "src/app/edge/live/common/weather/shared/shared";
import { SharedControllerChannelThreshold } from "src/app/edge/live/Controller/Channelthreshold/shared/shared";
import { SharedControllerChpSoc } from "src/app/edge/live/Controller/ChpSoc/shared/shared";
import { SharedControllerEnerixControl } from "src/app/edge/live/Controller/EnerixControl/shared/shared";
import { ControllerEvseSingleShared } from "src/app/edge/live/Controller/Evse/shared/shared";
import { SharedControllerHeat } from "src/app/edge/live/Controller/Heat/shared/shared";
import { ControllerBraiinsShared } from "src/app/edge/live/Controller/Io/Braiins/shared/shared";
import { SharedIoChannelSingleThreshold } from "src/app/edge/live/Controller/Io/ChannelSingleThreshold/shared/shared";
import { SharedControllerIoFixDigitalOutput } from "src/app/edge/live/Controller/Io/FixDigitalOutput/shared/shared";
import { SharedControllerIoHeatingElement } from "src/app/edge/live/Controller/Io/HeatingElement/shared/shared";
import { SharedControllerIoHeatpump } from "src/app/edge/live/Controller/Io/Heatpump/shared/shared";
import { SharedControllerIoHeatingRoom } from "../../edge/live/Controller/Io/HeatingRoom/shared/shared";
import { Edge } from "../components/edge/edge";
import { EdgeConfig } from "../components/edge/edgeconfig";
import { NavigationTree } from "../components/navigation/shared";
import { TEnumKeys } from "./utility";
import { Widget, WidgetClass, WidgetFactory, WidgetNature } from "./widget";

export class Widgets {
    public static readonly GROUPED_FACTORIES: Partial<
        Record<
            Widget["name"],
            {
                grouped: (
                    translate: TranslateService,
                    componentIds: Widget["componentId"][],
                    config: EdgeConfig,
                    factoryId: EdgeConfig.Factory["id"],
                ) => NavigationTree | null;
                single: (
                    translate: TranslateService,
                    componentId: Widget["componentId"],
                    config: EdgeConfig,
                ) => ConstructorParameters<typeof NavigationTree> | null;
            }
        >
    > = {
        "Controller.IO.Heating.Room": {
            grouped: SharedControllerIoHeatingRoom.getGroupedNavigationTree,
            single: SharedControllerIoHeatingRoom.getNavigationTree,
        },
        "Controller.Io.FixDigitalOutput": {
            grouped: SharedControllerIoFixDigitalOutput.getGroupedNavigationTree,
            single: SharedControllerIoFixDigitalOutput.getNavigationTree,
        },
        "Controller.IO.ChannelSingleThreshold": {
            grouped: SharedIoChannelSingleThreshold.getGroupedNavigationTree,
            single: SharedIoChannelSingleThreshold.getNavigationTree,
        },
        "Controller.BraiinsOS.Single": {
            grouped: ControllerBraiinsShared.getGroupedNavigationTree,
            single: ControllerBraiinsShared.getNavigationTree,
        },
    };

    /** Names of Widgets. */
    public readonly names: string[] = [];

    constructor(
        /** List of all Widgets. */
        public readonly list: Widget[] | null,
        /** List of Widget-Classes. */
        public readonly classes: TEnumKeys<typeof WidgetClass>[] | null,
    ) {
        // fill names
        if (list === null) {
            return;
        }
        for (const widget of list) {
            const name: string = widget.toString();
            if (!this.names.includes(name)) {
                this.names.push(name);
            }
        }
    }

    public static getCommonNavigationTree(
        edge: Edge,
        clazz: TEnumKeys<typeof WidgetClass>,
        translate: TranslateService,
        config: EdgeConfig,
    ): ConstructorParameters<typeof NavigationTree> | null {
        switch (clazz) {
            case "Grid":
                return SharedGrid.getNavigationTree(edge, config, translate);
            case "Consumption":
                return SharedConsumption.getNavigationTree(edge, config, translate);
            case "Common_Production":
                return SharedProduction.getNavigationTree(edge, config, translate);
            case "Storage":
                return SharedStorage.getNavigationTree(edge, translate, config);
            default:
                return null;
        }
    }

    public static getControllerNavigationTree(
        edge: Edge,
        widget: Widget,
        translate: TranslateService,
        config: EdgeConfig,
    ): ConstructorParameters<typeof NavigationTree> | null {
        const component = config.getComponentSafely(widget.componentId);
        if (component === null) {
            return null;
        }

        switch (widget.name) {
            case "Controller.CHP.SoC":
                return SharedControllerChpSoc.getNavigationTree(translate, component);
            case "Controller.Clever-PV":
                return SharedControllerEnerixControl.getNavigationTree(translate, component);
            case "Weather.OpenMeteo":
                return SharedWeather.getNavigationTree(translate, component);
            case "Controller.IO.HeatingElement":
                return SharedControllerIoHeatingElement.getNavigationTree(translate, component);
            case "Controller.Io.HeatPump.SgReady":
                return SharedControllerIoHeatpump.getNavigationTree(translate, component, edge);
            case "Heat.Askoma":
                return SharedControllerHeat.getNavigationTree(translate, component, true);
            case "Heat.MyPv":
                return SharedControllerHeat.getNavigationTree(translate, component, false);
            case "Heat.MyPv.AcThor9s":
                return SharedControllerHeat.getNavigationTree(translate, component, false);
            case "Evse.Controller.Single":
                return ControllerEvseSingleShared.getNavigationTree(edge, translate, widget.componentId, config);
            case "Controller.ChannelThreshold":
                return SharedControllerChannelThreshold.getNavigationTree(translate, component);
            default:
                return null;
        }
    }

    public static parseWidgets(edge: Edge, config: EdgeConfig): Widgets {
        const classes: TEnumKeys<typeof WidgetClass>[] = Object.keys(WidgetClass).filter((clazz) => {
            switch (clazz) {
                case "Common_Autarchy":
                case "Grid":
                    return config.hasMeter();
                case "Energymonitor":
                case "Consumption":
                    if (config.hasMeter() == true || config.hasProducer() == true || config.hasStorage() == true) {
                        return true;
                    } else {
                        return false;
                    }
                case "Storage":
                    return config.hasStorage();
                case "Common_Production":
                case "Common_Selfconsumption":
                    return config.hasProducer();
                case "Controller_ChannelThreshold":
                    return config.getComponentIdsByFactory("Controller.ChannelThreshold")?.length > 0;
                case "Controller_Io_Digital_Outputs":
                    return (
                        config.getComponentIdsByFactories(
                            "Controller.Io.FixDigitalOutput",
                            "Controller.IO.ChannelSingleThreshold",
                        )?.length > 0
                    );
                case "Controller.Api.ModbusTcp.ReadWrite":
                    return true;
                default:
                    return false;
            }
        }) as TEnumKeys<typeof WidgetClass>[];
        const list: Widget[] = [];

        for (const nature of Object.values(WidgetNature).filter((v) => typeof v === "string")) {
            for (const componentId of config.getComponentIdsImplementingNature(nature.toString())) {
                if (
                    nature === "io.openems.edge.io.api.DigitalInput" &&
                    list.some((e) => e.name === "io.openems.edge.io.api.DigitalInput")
                ) {
                    continue;
                }
                const component = config.getComponent(componentId);
                if (component.isEnabled) {
                    list.push({
                        name: nature,
                        componentId: componentId,
                        alias: component.alias,
                    });
                }
            }
        }
        for (const factory of Object.values(WidgetFactory).filter((v) => typeof v === "string")) {
            for (const componentId of config.getComponentIdsByFactory(factory.toString())) {
                const component = config.getComponent(componentId);
                if (factory === "Controller.Clever-PV") {
                    // Clever-PV Widget should be shown only if readOnly property is explicitely set to false
                    const readOnly = config.getPropertyFromComponent<boolean>(component, "readOnly");
                    if (readOnly !== false) {
                        continue;
                    }
                }
                if (component.isEnabled) {
                    list.push({
                        name: factory,
                        componentId: componentId,
                        alias: component.alias,
                    });
                }
            }
        }

        // explicitely sort ChannelThresholdControllers by their outputChannelAddress
        list.sort((w1, w2) => {
            if (
                w1.name === "Controller.IO.ChannelSingleThreshold" &&
                w2.name === "Controller.IO.ChannelSingleThreshold"
            ) {
                let outputChannelAddress1: string | string[] = config.getComponentProperties(w1.componentId)[
                    "outputChannelAddress"
                ];
                if (typeof outputChannelAddress1 !== "string") {
                    // Takes only the first output for simplicity reasons
                    outputChannelAddress1 = outputChannelAddress1[0];
                }
                let outputChannelAddress2: string | string[] = config.getComponentProperties(w2.componentId)[
                    "outputChannelAddress"
                ];
                if (typeof outputChannelAddress2 !== "string") {
                    // Takes only the first output for simplicity reasons
                    outputChannelAddress2 = outputChannelAddress2[0];
                }
                if (outputChannelAddress1 && outputChannelAddress2) {
                    return outputChannelAddress1.localeCompare(outputChannelAddress2);
                }
            }

            return w1.componentId.localeCompare(w1.componentId);
        });
        return new Widgets(list, classes);
    }

    public static getControllerNavigationTrees(
        edge: Edge,
        translate: TranslateService,
        config: EdgeConfig,
    ): ConstructorParameters<typeof NavigationTree>[] {
        const widgets = Widgets.parseWidgets(edge, config).list ?? [];
        const navigationTrees: ConstructorParameters<typeof NavigationTree>[] = [];
        const groupedComponentIdsByWidgetName: Partial<Record<Widget["name"], Widget["componentId"][]>> = {};

        for (const widget of widgets) {
            const groupedFactory = Widgets.GROUPED_FACTORIES[widget.name];
            if (groupedFactory != null) {
                groupedComponentIdsByWidgetName[widget.name] ??= [];
                const groupedComponentIds = groupedComponentIdsByWidgetName[widget.name];
                if (groupedComponentIds != null) {
                    groupedComponentIds.push(widget.componentId);
                }
                continue;
            }

            const navigationTree = Widgets.getControllerNavigationTree(edge, widget, translate, config);
            if (navigationTree != null) {
                navigationTrees.push(navigationTree);
            }
        }

        for (const [groupedWidgetName, componentIds] of Object.entries(groupedComponentIdsByWidgetName) as [
            Widget["name"],
            Widget["componentId"][],
        ][]) {
            const groupedFactory = Widgets.GROUPED_FACTORIES[groupedWidgetName];
            if (groupedFactory == null) {
                continue;
            }

            if (componentIds.length < 2) {
                for (const componentId of componentIds) {
                    const singleNavigationTree = groupedFactory.single(translate, componentId, config);
                    if (singleNavigationTree != null) {
                        navigationTrees.push(singleNavigationTree);
                    }
                }
                continue;
            }

            const groupedNavigationTree = groupedFactory.grouped(translate, componentIds, config, groupedWidgetName);
            if (groupedNavigationTree != null) {
                navigationTrees.push(groupedNavigationTree.toConstructorParams());
            }
        }

        return navigationTrees;
    }
}
