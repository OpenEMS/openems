// @ts-strict-ignore
import { Edge } from "../components/edge/edge";
import { EdgeConfig } from "../components/edge/edgeconfig";
import { EdgePermission } from "../shared";

export enum WidgetClass {
    "Energymonitor",
    "Common_Autarchy",
    "Common_Selfconsumption",
    "Storage",
    "Grid",
    "Common_Production",
    "Consumption",
    "Controller_ChannelThreshold",
    "Controller_Io_Digital_Outputs",
}

export enum WidgetNature {
    "IO.OPENEMS.EDGE.EVCS.API.EVCS",
    "IO.OPENEMS.EDGE.HEAT.API.MANAGED_HEAT_ELEMENT",
    "IO.OPENEMS.IMPL.CONTROLLER.CHANNELTHRESHOLD.CHANNEL_THRESHOLD_CONTROLLER", // TODO deprecated
    "IO.OPENEMS.EDGE.IO.API.DIGITAL_INPUT",
}

export enum WidgetFactory {
    "EVSE.CONTROLLER.SINGLE",
    "EVSE.CONTROLLER.CLUSTER",
    "CONTROLLER.API.MODBUS_TCP.READ_WRITE",
    "CONTROLLER.ASYMMETRIC.PEAK_SHAVING",
    "CONTROLLER.CHANNEL_THRESHOLD",
    "CONTROLLER.CHP.SO_C",
    "CONTROLLER.ESS.DELAYED_SELL_TO_GRID",
    "CONTROLLER.ESS.FIX_ACTIVE_POWER",
    "CONTROLLER.ESS.GRID_OPTIMIZED_CHARGE",
    "CONTROLLER.ESS.TIME-Of-Use-TARIFF.DISCHARGE",
    "CONTROLLER.ESS.TIME-Of-Use-Tariff",
    "CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD",
    "CONTROLLER.IO.FIX_DIGITAL_OUTPUT",
    "CONTROLLER.IO.HEATING_ELEMENT",
    "CONTROLLER.IO.HEATING.ROOM",
    "CONTROLLER.IO.HEAT_PUMP.SG_READY",
    "CONTROLLER.HEAT.HEATINGELEMENT",
    "CONTROLLER.SYMMETRIC.PEAK_SHAVING",
    "CONTROLLER.TIMESLOT_PEAKSHAVING",
    "EVCS.CLUSTER.PEAK_SHAVING",
    "EVCS.CLUSTER.SELF_CONSUMPTION",
}

export type Icon = {
    color: string;
    size: string;
    name: string;
};

export type ImageIcon = {
    src: string;
    large: boolean;
};

export class Widget {
    public name: WidgetNature | WidgetFactory | string;
    public componentId: string;
    public alias: string;
}

export class Widgets {
    /**
     * Names of Widgets.
     */
    public readonly names: string[] = [];

    private constructor(
        /**
         * List of all Widgets.
         */
        public readonly list: Widget[],
        /**
         * List of Widget-Classes.
         */
        public readonly classes: string[],
    ) {
        // fill names
        for (const widget of list) {
            const name: string = WIDGET.NAME.TO_STRING();
            if (!THIS.NAMES.INCLUDES(name)) {
                THIS.NAMES.PUSH(name);
            }
        }
    }

    public static parseWidgets(edge: Edge, config: EdgeConfig): Widgets {
        const classes: string[] = OBJECT.VALUES(WidgetClass) //
            .filter(v => typeof v === "string")
            .filter(clazz => {
                if (!EDGE.IS_VERSION_AT_LEAST("2018.8")) {

                    // no filter for deprecated versions
                    return true;
                }
                switch (clazz) {
                    case "Common_Autarchy":
                    case "Grid":
                        return CONFIG.HAS_METER();
                    case "Energymonitor":
                    case "Consumption":
                        if (CONFIG.HAS_METER() == true || CONFIG.HAS_PRODUCER() == true || CONFIG.HAS_STORAGE() == true) {
                            return true;
                        } else {
                            return false;
                        }
                    case "Storage":
                        return CONFIG.HAS_STORAGE();
                    case "Common_Production":
                    case "Common_Selfconsumption":
                        return CONFIG.HAS_PRODUCER();
                    case "Controller_ChannelThreshold":
                        return CONFIG.GET_COMPONENT_IDS_BY_FACTORY("CONTROLLER.CHANNEL_THRESHOLD")?.length > 0;
                    case "Controller_Io_Digital_Outputs":
                        return CONFIG.GET_COMPONENT_IDS_BY_FACTORIES("CONTROLLER.IO.FIX_DIGITAL_OUTPUT", "CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD")?.length > 0;
                    case "CONTROLLER.API.MODBUS_TCP.READ_WRITE":
                        return EDGE_PERMISSION.IS_MODBUS_TCP_API_WIDGET_ALLOWED(edge);
                    default:
                        return false;
                }
            }).map(clazz => CLAZZ.TO_STRING());
        const list: Widget[] = [];

        for (const nature of OBJECT.VALUES(WidgetNature).filter(v => typeof v === "string")) {
            for (const componentId of CONFIG.GET_COMPONENT_IDS_IMPLEMENTING_NATURE(NATURE.TO_STRING())) {
                if (nature === "IO.OPENEMS.EDGE.IO.API.DIGITAL_INPUT" && LIST.SOME(e => E.NAME === "IO.OPENEMS.EDGE.IO.API.DIGITAL_INPUT")) {
                    continue;
                }
                const component = CONFIG.GET_COMPONENT(componentId);
                if (COMPONENT.IS_ENABLED) {
                    LIST.PUSH({ name: nature, componentId: componentId, alias: COMPONENT.ALIAS });
                }
            }
        }
        for (const factory of OBJECT.VALUES(WidgetFactory).filter(v => typeof v === "string")) {
            for (const componentId of CONFIG.GET_COMPONENT_IDS_BY_FACTORY(FACTORY.TO_STRING())) {
                const component = CONFIG.GET_COMPONENT(componentId);
                if (COMPONENT.IS_ENABLED) {
                    LIST.PUSH({ name: factory, componentId: componentId, alias: COMPONENT.ALIAS });
                }
            }
        }

        // explicitely sort ChannelThresholdControllers by their outputChannelAddress
        LIST.SORT((w1, w2) => {
            if (W1.NAME === "CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD" && W2.NAME === "CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD") {
                let outputChannelAddress1: string | string[] = CONFIG.GET_COMPONENT_PROPERTIES(W1.COMPONENT_ID)["outputChannelAddress"];
                if (typeof outputChannelAddress1 !== "string") {
                    // Takes only the first output for simplicity reasons
                    outputChannelAddress1 = outputChannelAddress1[0];
                }
                let outputChannelAddress2: string | string[] = CONFIG.GET_COMPONENT_PROPERTIES(W2.COMPONENT_ID)["outputChannelAddress"];
                if (typeof outputChannelAddress2 !== "string") {
                    // Takes only the first output for simplicity reasons
                    outputChannelAddress2 = outputChannelAddress2[0];
                }
                if (outputChannelAddress1 && outputChannelAddress2) {
                    return OUTPUT_CHANNEL_ADDRESS1.LOCALE_COMPARE(outputChannelAddress2);
                }
            }

            return W1.COMPONENT_ID.LOCALE_COMPARE(W1.COMPONENT_ID);
        });
        return new Widgets(list, classes);
    }
}

export enum ProductType {
}
