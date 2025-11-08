import { TranslateService } from "@ngx-translate/core";
import { SharedAutarchy } from "src/app/edge/live/common/autarchy/shared/shared";
import { SharedGrid } from "src/app/edge/live/common/grid/shared/shared";
import { SharedSelfConsumption } from "src/app/edge/live/common/selfconsumption/shared/shared";
import { Edge } from "../components/edge/edge";
import { EdgeConfig } from "../components/edge/edgeconfig";
import { NavigationTree } from "../components/navigation/shared";
import { EdgePermission } from "../shared";
import { TArrayElement, TEnumKeys } from "./utility";
import { Widget, WidgetClass, WidgetFactory, WidgetNature } from "./widget";

export class Widgets {
    /**
     * Names of Widgets.
     */
    public readonly names: string[] = [];

    constructor(
        /**
         * List of all Widgets.
         */
        public readonly list: Widget[],
        /**
         * List of Widget-Classes.
         */
        public readonly classes: TEnumKeys<typeof WidgetClass>[] | null,
    ) {
        // fill names
        for (const widget of list) {
            const name: string = widget.name.toString();
            if (!this.names.includes(name)) {
                this.names.push(name);
            }
        }
    }

    public static getCommonNavigationTree(edge: Edge, clazz: TArrayElement<Widgets["names"]>, translate: TranslateService, config: EdgeConfig): ConstructorParameters<typeof NavigationTree> | null {
        switch (clazz) {
            case "Common_Autarchy":
                return SharedAutarchy.getNavigationTree(translate);
            case "Grid":
                return SharedGrid.getNavigationTree(edge, config, translate);
            case "Common_Selfconsumption":
                return SharedSelfConsumption.getNavigationTree(translate);
            default:
                return null;
        }
    }

    public static parseWidgets(edge: Edge, config: EdgeConfig): Widgets {
        const classes: TEnumKeys<typeof WidgetClass>[] = Object.keys(WidgetClass)
            .filter((clazz) => {
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
                        return config.getComponentIdsByFactories("Controller.Io.FixDigitalOutput", "Controller.IO.ChannelSingleThreshold")?.length > 0;
                    case "Controller.Api.ModbusTcp.ReadWrite":
                        return EdgePermission.isModbusTcpApiWidgetAllowed(edge);
                    default:
                        return false;
                }
            }) as TEnumKeys<typeof WidgetClass>[];
        const list: Widget[] = [];

        for (const nature of Object.values(WidgetNature).filter(v => typeof v === "string")) {
            for (const componentId of config.getComponentIdsImplementingNature(nature.toString())) {
                if (nature === "io.openems.edge.io.api.DigitalInput" && list.some(e => e.name === "io.openems.edge.io.api.DigitalInput")) {
                    continue;
                }
                const component = config.getComponent(componentId);
                if (component.isEnabled) {
                    list.push({ name: nature, componentId: componentId, alias: component.alias });
                }
            }
        }
        for (const factory of Object.values(WidgetFactory).filter(v => typeof v === "string")) {
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
                    list.push({ name: factory, componentId: componentId, alias: component.alias });
                }
            }
        }

        // explicitely sort ChannelThresholdControllers by their outputChannelAddress
        list.sort((w1, w2) => {
            if (w1.name === "Controller.IO.ChannelSingleThreshold" && w2.name === "Controller.IO.ChannelSingleThreshold") {
                let outputChannelAddress1: string | string[] = config.getComponentProperties(w1.componentId)["outputChannelAddress"];
                if (typeof outputChannelAddress1 !== "string") {
                    // Takes only the first output for simplicity reasons
                    outputChannelAddress1 = outputChannelAddress1[0];
                }
                let outputChannelAddress2: string | string[] = config.getComponentProperties(w2.componentId)["outputChannelAddress"];
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
}
