// @ts-strict-ignore
import { TimeUnit } from "CHART.JS";
import { SumState } from "src/app/index/shared/sumState";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";

import { Role } from "../../type/role";
import { ButtonLabel } from "../modal/modal-button/modal-button";
import { ModalLineComponent, TextIndentation } from "../modal/modal-line/modal-line";
import { OeChartTester, OeFormlyViewTester } from "../shared/testing/tester";
import { Edge } from "./edge";
import { EdgeConfig, PersistencePriority } from "./edgeconfig";

export namespace DummyConfig {

    export function dummyEdge(values: {
        edgeId?: string,
        comment?: string,
        producttype?: string,
        version?: string,
        role?: Role,
        isOnline?: boolean,
        lastmessage?: Date,
        sumState?: SumState,
        firstSetupProtocol?: Date,
    }): Edge {
        return new Edge(
            VALUES.EDGE_ID ?? "edge0",
            VALUES.COMMENT ?? "edge0",
            VALUES.PRODUCTTYPE ?? "",
            VALUES.VERSION ?? "2023.3.5",
            VALUES.ROLE ?? ROLE.ADMIN,
            VALUES.IS_ONLINE ?? true,
            VALUES.LASTMESSAGE ?? new Date(),
            VALUES.SUM_STATE ?? SUM_STATE.OK,
            VALUES.FIRST_SETUP_PROTOCOL ?? new Date(0),
        );
    }

    const DUMMY_EDGE: Edge = new Edge("edge0", "", "", "2023.3.5", ROLE.ADMIN, true, new Date(), SUM_STATE.OK, new Date(0));
    export function from(...components: Component[]): EdgeConfig {

        return new EdgeConfig(DUMMY_EDGE, <EdgeConfig>{
            components: <unknown>components?.reduce((acc, c) => {
                C.FACTORY_ID = C.FACTORY.ID;
                return ({ ...acc, [C.ID]: c });
            }, {}),
            factories: components?.reduce((p, c) => {
                p[C.FACTORY.ID] = new EDGE_CONFIG.FACTORY(C.FACTORY.ID, "", C.FACTORY.NATURE_IDS);
                return p;
            }, {}),
        });
    }

    export function convertDummyEdgeConfigToRealEdgeConfig(edgeConfig: EdgeConfig): EdgeConfig {
        const components = OBJECT.VALUES(edgeConfig?.components) ?? null;

        const factories = {};
        COMPONENTS.FOR_EACH(obj => {
            if (factories[OBJ.FACTORY_ID]) {
                factories[OBJ.FACTORY_ID].componentIds = [...factories[OBJ.FACTORY_ID].componentIds, OBJ.ID];
            } else {
                factories[OBJ.FACTORY_ID] = {
                    componentIds: [OBJ.ID],
                    description: "",
                    id: OBJ.FACTORY_ID,
                    name: OBJ.FACTORY_ID,
                    natureIds: EDGE_CONFIG.FACTORIES[OBJ.FACTORY_ID].natureIds,
                    properties: [],
                };
            }
        });

        return new EdgeConfig(DUMMY_EDGE, <EdgeConfig>{
            components: EDGE_CONFIG.COMPONENTS,
            factories: factories,
        });
    }

    export namespace Factory {

        export const SUM = {
            id: "CORE.SUM",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.SUM.SUM",
                "IO.OPENEMS.EDGE.COMMON.MODBUSSLAVE.MODBUS_SLAVE",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const METER_SOCOMEC_THREEPHASE = {
            id: "METER.SOCOMEC.THREEPHASE",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.BRIDGE.MODBUS.API.MODBUS_COMPONENT",
                "IO.OPENEMS.EDGE.COMMON.MODBUSSLAVE.MODBUS_SLAVE",
                "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER",
                "IO.OPENEMS.EDGE.METER.SOCOMEC.SOCOMEC_METER",
                "IO.OPENEMS.EDGE.METER.SOCOMEC.THREEPHASE.SOCOMEC_METER_THREEPHASE",
            ],
        };

        export const METER_GOODWE_GRID = {
            id: "GOOD_WE.GRID-Meter",
            natureIds: [
                "IO.OPENEMS.EDGE.GOODWE.GRIDMETER.GOOD_WE_GRID_METER",
                "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER",
                "IO.OPENEMS.EDGE.BRIDGE.MODBUS.API.MODBUS_COMPONENT",
                "IO.OPENEMS.EDGE.COMMON.MODBUSSLAVE.MODBUS_SLAVE",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const CHARGER_GOODWE_MPPT_TWO_STRING = {
            id: "GOOD_WE.CHARGER.MPPT.TWO-String",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.MODBUSSLAVE.MODBUS_SLAVE",
                "IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.GOODWE.CHARGER.GOOD_WE_CHARGER",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const EVCS_KEBA_KECONTACT = {
            id: "EVCS.KEBA.KE_CONTACT",
            natureIds: [
                "IO.OPENEMS.EDGE.EVCS.KEBA.KECONTACT.EVCS_KEBA_KE_CONTACT",
                "IO.OPENEMS.EDGE.COMMON.MODBUSSLAVE.MODBUS_SLAVE",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.EVCS.API.MANAGED_EVCS",
                "IO.OPENEMS.EDGE.EVCS.API.EVCS",
            ],
        };

        export const EVSE_CHARGEPOINT_KEBA_UDP = {
            id: "EVSE.CHARGE_POINT.KEBA.UDP",
            natureIds: [
                "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER",
                "IO.OPENEMS.EDGE.EVSE.CHARGEPOINT.KEBA.COMMON.KEBA_UDP",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.EVSE.API.CHARGEPOINT.EVSE_CHARGE_POINT",
                "IO.OPENEMS.EDGE.EVSE.CHARGEPOINT.KEBA.COMMON.EVSE_KEBA",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const ESS_GENERIC_MANAGEDSYMMETRIC = {
            id: "ESS.GENERIC.MANAGED_SYMMETRIC",
            natureIds: [
                "IO.OPENEMS.EDGE.GOODWE.COMMON.GOOD_WE",
                "IO.OPENEMS.EDGE.BRIDGE.MODBUS.API.MODBUS_COMPONENT",
                "IO.OPENEMS.EDGE.COMMON.MODBUSSLAVE.MODBUS_SLAVE",
                "IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.ESS.API.HYBRID_ESS",
                "IO.OPENEMS.EDGE.GOODWE.ESS.GOOD_WE_ESS",
                "IO.OPENEMS.EDGE.ESS.API.MANAGED_SYMMETRIC_ESS",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const ESS_LIMITER_14A = {
            id: "CONTROLLER.ESS.LIMITER14A",
            natureIds: [
                "IO.OPENEMS.EDGE.CONTROLLER.ESS.LIMITER14A",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",

            ],
        };

        export const SOLAR_EDGE_PV_INVERTER = {
            id: "SOLAR_EDGE.PV-Inverter",
            natureIds: [
                "IO.OPENEMS.EDGE.PVINVERTER.SUNSPEC.SUN_SPEC_PV_INVERTER",
                "IO.OPENEMS.EDGE.METER.API.ASYMMETRIC_METER",
                "IO.OPENEMS.EDGE.METER.API.SYMMETRIC_METER",
                "IO.OPENEMS.EDGE.BRIDGE.MODBUS.API.MODBUS_COMPONENT",
                "IO.OPENEMS.EDGE.COMMON.MODBUSSLAVE.MODBUS_SLAVE",
                "IO.OPENEMS.EDGE.PVINVERTER.API.MANAGED_SYMMETRIC_PV_INVERTER",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
            ],
        };
        export const EVCS_HARDY_BARTH = {
            id: "EVCS.HARDY_BARTH",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.EVCS.HARDYBARTH.EVCS_HARDY_BARTH",
                "IO.OPENEMS.EDGE.EVCS.API.MANAGED_EVCS",
                "IO.OPENEMS.EDGE.EVCS.API.EVCS",
                "IO.OPENEMS.EDGE.EVCS.API.DEPRECATED_EVCS",
                "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER",
            ],
        };

        export const EVCS_MENNEKES = {
            id: "EVCS.MENNEKES",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.EVCS.HARDYBARTH.EVCS_HARDY_BARTH",
                "IO.OPENEMS.EDGE.EVCS.API.MANAGED_EVCS",
                "IO.OPENEMS.EDGE.EVCS.API.EVCS",
                "IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER",
            ],
        };

        export const MODBUS_TCP_READWRITE = {
            id: "CONTROLLER.API.MODBUS_TCP.READ_WRITE",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.JSONAPI.JSON_API",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.CONTROLLER.API.MODBUS.MODBUS_TCP_API",
                "IO.OPENEMS.EDGE.CONTROLLER.API.MODBUS.READWRITE.CONTROLLER_API_MODBUS_TCP_READ_WRITE",
                "IO.OPENEMS.EDGE.CONTROLLER.API.CONTROLLER",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const MODBUS_RTU_READWRITE = {
            id: "CONTROLLER.API.MODBUS_RTU.READ_WRITE",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.JSONAPI.JSON_API",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.CONTROLLER.API.MODBUS.MODBUS_RTU_API",
                "IO.OPENEMS.EDGE.CONTROLLER.API.MODBUS.READWRITE.CONTROLLER_API_MODBUS_RTU_READ_WRITE",
                "IO.OPENEMS.EDGE.CONTROLLER.API.CONTROLLER",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const HEAT_PUMP_SG_READY = {
            id: "CONTROLLER.IO.HEAT_PUMP.SG_READY",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.CONTROLLER.IO.HEATPUMP.SGREADY.CONTROLLER_IO_HEAT_PUMP_SG_READY",
                "IO.OPENEMS.EDGE.CONTROLLER.API.CONTROLLER",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const GOODWE_CHARGER_PV_1 = {
            id: "GOOD_WE.CHARGER-PV1",
            natureIds: [
                "IO.OPENEMS.EDGE.BRIDGE.MODBUS.API.MODBUS_COMPONENT",
                "IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.GOODWE.CHARGER.GOOD_WE_CHARGER",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };

        export const CONTROLLER_ESS_EMERGENCY_CAPACITY_RESERVE = {
            id: "CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE",
            natureIds: [
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.CONTROLLER.ESS.EMERGENCYCAPACITYRESERVE.CONTROLLER_ESS_EMERGENCY_CAPACITY_RESERVE",
                "IO.OPENEMS.EDGE.CONTROLLER.API.CONTROLLER",
            ],
        };
        export const Heat_MYPV_ACTHOR = {
            id: "HeatMyPv",
            natureIds: [
                "IO.OPENEMS.EDGE.HEAT.MYPV.ACTHOR9S.HEAT_MY_PV_AC_THOR9S",
                "IO.OPENEMS.EDGE.BRIDGE.MODBUS.API.MODBUS_COMPONENT",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.HEAT.API.MANAGED_HEAT_ELEMENT",
                "IO.OPENEMS.EDGE.HEAT.API.HEAT",
            ],
        };

        export const CONTROLLER_IO_FIX_DIGITAL_OUTPUT = {
            id: "CONTROLLER.IO.FIX_DIGITAL_OUTPUT",
            natureIds: [
                "IO.OPENEMS.EDGE.CONTROLLER.IO.FIXDIGITALOUTPUT.CONTROLLER_IO_FIX_DIGITAL_OUTPUT",
                "IO.OPENEMS.EDGE.COMMON.COMPONENT.OPENEMS_COMPONENT",
                "IO.OPENEMS.EDGE.CONTROLLER.API.CONTROLLER",
                "IO.OPENEMS.EDGE.TIMEDATA.API.TIMEDATA_PROVIDER",
            ],
        };
    }

    export namespace Component {

        export const SUM = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: "CORE.SUM",
            factory: FACTORY.SUM,
            properties: {
                enabled: "true",
            },
            channels: {},
        });
        export const EVCS_HARDY_BARTH = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: "EVCS.HARDY_BARTH",
            factory: Factory.EVCS_HARDY_BARTH,
            properties: {
                enabled: "true",
            },
            channels: {},
        });

        export const EVCS_MENNEKES = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: "EVCS.MENNEKES",
            factory: Factory.EVCS_MENNEKES,
            properties: {
                enabled: "true",
            },
            channels: {},
        });

        export const SOCOMEC_GRID_METER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: "METER.SOCOMEC.THREEPHASE",
            factory: Factory.METER_SOCOMEC_THREEPHASE,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "GRID",
            },
            channels: {},
        });

        export const SOCOMEC_CONSUMPTION_METER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.METER_SOCOMEC_THREEPHASE,
            factoryId: Factory.METER_SOCOMEC_THREEPHASE.id,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "CONSUMPTION_METERED",
            },
            channels: {},
        });
        export const GOODWE_GRID_METER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.METER_GOODWE_GRID,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "PRODUCTION",
            },
            channels: {},
        });

        export const GOODWE_CHARGER_MPPT_TWO_STRING = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias,
            factory: Factory.CHARGER_GOODWE_MPPT_TWO_STRING,
            properties: {
                "alias": "MPPT 1",
                "enabled": true,
                "ESS_OR_BATTERY_INVERTER.ID": "batteryInverter0",
                "mpptPort": "MPPT_1",
            },
            channels: {},
        });

        export const SOLAR_EDGE_PV_INVERTER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: "SOLAR_EDGE.PV-Inverter",
            factory: Factory.SOLAR_EDGE_PV_INVERTER,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "PRODUCTION",
            },
            channels: {},
        });

        export const ESS_GENERIC_MANAGEDSYMMETRIC = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: "ESS.GENERIC.MANAGED_SYMMETRIC",
            factory: Factory.ESS_GENERIC_MANAGEDSYMMETRIC,
            properties: {
                invert: false,
                modbusUnitId: 5,
            },
            channels: {},
        });

        export const ESS_LIMITER_14A = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.ESS_LIMITER_14A,
            properties: {
                enabled: "true",
                ["ESS.ID"]: "ess0",
            },
            channels: {},
        });

        export const EVCS_KEBA_KECONTACT = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.EVCS_KEBA_KECONTACT,
            properties: {
                invert: false,
                modbusUnitId: 5,
                // TODO
                type: "CONSUMPTION_METERED",
            },
            channels: {},
        });

        export const EVSE_CHARGEPOINT_KEBA_UDP = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.EVSE_CHARGEPOINT_KEBA_UDP,
            properties: {
                alias: alias ?? id,
                enabled: true,
                readOnly: false,
            },
            channels: {},
        });

        export const GOODWE_CHARGER_PV_1 = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.GOODWE_CHARGER_PV_1,
            properties: {
                modbusUnitId: 5,
            },
            channels: {},
        });
        export const Heat_MYPV_ACTHOR = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.Heat_MYPV_ACTHOR,
            properties: {
                enabled: "true",
                modbusUnitId: 1,
                // TODO
                type: "CONSUMPTION_METERED",
            },
            channels: {},
        });

        export const MODBUS_TCP_READWRITE = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.MODBUS_TCP_READWRITE,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "PRODUCTION",
                writeChannels: [
                    "Ess0SetActivePowerEquals",
                ],
            },
            channels: {},
        });


        export const CONTROLLER_ESS_EMERGENCY_CAPACITY_RESERVE = ({ id = "ctrlEmergencyCapacityReserve0", essId = "ess0", isReserveSocEnabled = true, alias = id }: { id?: string, essId?: string, isReserveSocEnabled?: boolean, alias?: string }): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: "CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE",
            factory: Factory.CONTROLLER_ESS_EMERGENCY_CAPACITY_RESERVE,
            isEnabled: true,
            properties: {
                "modbusUnitId": 5,
                "ESS.ID": essId,
                "isReserveSocEnabled": isReserveSocEnabled,
            },
            channels: {},

        });

        export const HEAT_PUMP_SG_READY = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.HEAT_PUMP_SG_READY,
            properties: {
                enabled: true,
                mode: "AUTOMATIC",
            },
            channels: {},
        });

        export const CONTROLLER_IO_FIX_DIGITAL_OUTPUT = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.CONTROLLER_IO_FIX_DIGITAL_OUTPUT,
            properties: {
                enabled: true,
                isOn: true,
            },
            channels: {},
        });
    }
}

/**
 * Factories.
 */
// identifier `Factory` is also used in namespace

type Factory = {
    id: string,
    natureIds: string[],
};

/**
 * Components
 */
// identifier `Component` is also used in namespace
// eslint-disable-next-line @typescript-eslint/no-unused-vars
type Component = {
    id: string,
    alias: string, // defaults to id
    factory: Factory,
    factoryId?: string // generated
    properties: { [property: string]: any },
    channels?: {},
    isEnabled?: boolean
};

export const CHANNEL_LINE = (name: string, value: string, indentation?: TextIndentation): OE_FORMLY_VIEW_TESTER.FIELD => ({
    type: "channel-line",
    name: name,
    ...(indentation && { indentation: indentation }),
    value: value,
});

export const VALUE_FROM_CHANNELS_LINE = (name: string, value: string, indentation?: TextIndentation): OE_FORMLY_VIEW_TESTER.FIELD => ({
    type: "value-from-channels-line",
    name: name,
    ...(indentation && { indentation: indentation }),
    value: value,
});

export const PHASE_ADMIN = (name: string, voltage: string, current: string, power: string): OE_FORMLY_VIEW_TESTER.FIELD => ({
    type: "children-line",
    name: name,
    indentation: TEXT_INDENTATION.SINGLE,
    children: [
        {
            type: "item",
            value: voltage,
        },
        {
            type: "item",
            value: current,
        },
        {
            type: "item",
            value: power,
        },
    ],
});

export const PHASE_GUEST = (name: string, power: string): OE_FORMLY_VIEW_TESTER.FIELD => ({
    type: "children-line",
    name: name,
    indentation: TEXT_INDENTATION.SINGLE,
    children: [
        {
            type: "item",
            value: power,
        },
    ],
});

export const LINE_HORIZONTAL: OE_FORMLY_VIEW_TESTER.FIELD = {
    type: "horizontal-line",
};

export const LINE_INFO_PHASES_DE: OE_FORMLY_VIEW_TESTER.FIELD = {
    type: "info-line",
    name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen.",
};

export const LINE_INFO = (text: string): OE_FORMLY_VIEW_TESTER.FIELD => ({
    type: "info-line",
    name: text,
});
export const LINE_BUTTONS_FROM_FORM_CONTROL = (text: string, controlName: string, buttons: ButtonLabel[]): OE_FORMLY_VIEW_TESTER.FIELD => ({
    type: "buttons-from-form-control-line",
    name: text,
    buttons: buttons,
    controlName: controlName,
});
export const RANGE_BUTTONS_FROM_FORM_CONTROL_LINE = <T>(controlName: string, expectedValue: T, properties: Partial<Extract<ModalLineComponent["control"], { type: "RANGE" }>["properties"]>,): OE_FORMLY_VIEW_TESTER.FIELD => ({
    type: "range-button-from-form-control-line",
    controlName,
    expectedValue,
    properties,
});

export namespace ChartConfig {


    export const BAR_CHART_OPTIONS = (period: string, chartType: "line" | "bar", options: { [key: string]: { scale: { min: number, max: number }, ticks: { stepSize: number } } }, title?: string): OE_CHART_TESTER.DATASET.OPTION => ({
        type: "option", options: {
            "responsive": true,
            "maintainAspectRatio": false,
            "elements": {
                "point": {
                    "radius": 0,
                    "hitRadius": 0,
                    "hoverRadius": 0,
                },
                "line": {
                    "stepped": false,
                    "fill": true,
                },
            },
            "datasets": {
                "bar": {
                    "barPercentage": 1,
                },
                "line": {
                },
            },
            "plugins": {
                "colors": {
                    "enabled": false,
                },
                "legend": {
                    "display": true,
                    "position": "bottom",
                    "labels": {
                        "color": "",
                    },
                },
                "tooltip": {
                    "intersect": false,
                    "mode": "x",
                    "callbacks": {
                    },
                },
            },
            "scales": {
                "x": {
                    "stacked": true,
                    "offset": true,
                    "type": "time",
                    "ticks": {
                        "source": "auto",
                        "maxTicksLimit": 31,
                    },
                    "bounds": "ticks",
                    "adapters": {
                        "date": {
                            "locale": {
                                "code": "de",
                                "formatLong": {
                                },
                                "localize": {
                                },
                                "match": {
                                },
                                "options": {
                                    "weekStartsOn": 1,
                                    "firstWeekContainsDate": 4,
                                },
                            },
                        },
                    },
                    "time": {
                        "unit": period as TimeUnit,
                        "displayFormats": {
                            "datetime": "yyyy-MM-dd HH:mm:ss",
                            "millisecond": "SSS [ms]",
                            "second": "HH:mm:ss a",
                            "minute": "HH:mm",
                            "hour": "HH:00",
                            "day": "dd",
                            "week": "ll",
                            "month": "MM",
                            "quarter": "[Q]Q - YYYY",
                            "year": "yyyy",
                        },
                    },
                },
                "left": {
                    ...options["left"]?.scale, ...(chartType === "line" ? { stacked: false } : {}),
                    "title": {
                        "text": "kWh",
                        "display": false,
                        "padding": 5,
                        "font": { "size": 11 },
                    },
                    "beginAtZero": true,
                    "position": "left",
                    "grid": { "display": true },
                    "ticks": {
                        ...options["left"]?.ticks,
                        "color": "",
                        "padding": 5,
                        "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                    },
                },
            },
        },
    });
    export const LINE_CHART_OPTIONS = (period: string, chartType: "line" | "bar", options: { [key: string]: { scale: { min: number, max: number }, ticks: { stepSize: number } } }, title?: string): OE_CHART_TESTER.DATASET.OPTION => ({
        type: "option",
        options: {
            "responsive": true,
            "maintainAspectRatio": false,
            "elements": {
                "point": {
                    "radius": 0,
                    "hitRadius": 0,
                    "hoverRadius": 0,
                },
                "line": {
                    "stepped": false,
                    "fill": true,
                },
            },
            "datasets": {
                "bar": {
                },
                "line": {
                },
            },
            "plugins": {
                "colors": {
                    "enabled": false,
                },
                "legend": {
                    "display": true,
                    "position": "bottom",
                    "labels": {
                        "color": "",
                    },
                },
                "tooltip": {
                    "intersect": false,
                    "mode": "index",
                    "callbacks": {
                    },
                },
            },
            "scales": {
                "x": {
                    "stacked": true,
                    "offset": false,
                    "type": "time",
                    "ticks": {
                        "source": "auto",
                        "maxTicksLimit": 31,
                    },
                    "bounds": "ticks",
                    "adapters": {
                        "date": {
                            "locale": {
                                "code": "de",
                                "formatLong": {
                                },
                                "localize": {
                                },
                                "match": {
                                },
                                "options": {
                                    "weekStartsOn": 1,
                                    "firstWeekContainsDate": 4,
                                },
                            },
                        },
                    },
                    "time": {
                        "unit": period as TimeUnit,
                        "displayFormats": {
                            "datetime": "yyyy-MM-dd HH:mm:ss",
                            "millisecond": "SSS [ms]",
                            "second": "HH:mm:ss a",
                            "minute": "HH:mm",
                            "hour": "HH:00",
                            "day": "dd",
                            "week": "ll",
                            "month": "MM",
                            "quarter": "[Q]Q - YYYY",
                            "year": "yyyy",
                        },
                    },
                },
                "left": {
                    ...options["left"]?.scale, ...(chartType === "line" ? { stacked: false } : {}),
                    "title": {
                        "text": "kW",
                        "display": false,
                        "padding": 5,
                        "font": { "size": 11 },
                    },
                    "beginAtZero": true,
                    "position": "left",
                    "grid": { "display": true },
                    "ticks": {
                        ...options["left"]?.ticks,
                        "color": "",
                        "padding": 5,
                        "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                    },
                },
            },
        },
    });
    export const LINE_CHART_OPTIONS_TYPE_PERCENTAGE = (period: string, chartType: "line" | "bar", options: { [key: string]: { scale: { min: number, max: number }, ticks: { stepSize: number } } }, title?: string): OE_CHART_TESTER.DATASET.OPTION => ({
        type: "option",
        options: {
            "responsive": true,
            "maintainAspectRatio": false,
            "interaction": {
                "mode": "index",
                "intersect": false,
            },
            "elements": {
                "point": {
                    "radius": 0,
                    "hitRadius": 0,
                    "hoverRadius": 0,
                },
                "line": {
                    "stepped": false,
                    "fill": true,
                },
            },
            "datasets": {
                "bar": {
                },
                "line": {
                },
            },
            "plugins": {
                "colors": {
                    "enabled": false,
                },
                "legend": {
                    "display": true,
                    "position": "bottom",
                    "labels": {
                        "color": "",
                    },
                },
                "tooltip": {
                    "enabled": true,
                    "intersect": false,
                    "mode": "index",
                    "callbacks": {
                    },
                },
                "annotation": { "annotations": {} }, "datalabels": {
                    display: false,
                },
            },
            "scales": {
                "x": {
                    "stacked": true,
                    "offset": false,
                    "type": "time",
                    "ticks": {
                        "source": "auto",
                        "maxTicksLimit": 31,
                    },
                    "bounds": "ticks",
                    "adapters": {
                        "date": {
                            "locale": {
                                "code": "de",
                                "formatLong": {
                                },
                                "localize": {
                                },
                                "match": {
                                },
                                "options": {
                                    "weekStartsOn": 1,
                                    "firstWeekContainsDate": 4,
                                },
                            },
                        },
                    },
                    "time": {
                        "unit": period as TimeUnit,
                        "displayFormats": {
                            "datetime": "yyyy-MM-dd HH:mm:ss",
                            "millisecond": "SSS [ms]",
                            "second": "HH:mm:ss a",
                            "minute": "HH:mm",
                            "hour": "HH:00",
                            "day": "dd",
                            "week": "ll",
                            "month": "MM",
                            "quarter": "[Q]Q - YYYY",
                            "year": "yyyy",
                        },
                    },
                },
                "left": {
                    ...options["left"]?.scale, ...(chartType === "line" ? { stacked: false } : {}),
                    "title": {
                        "text": "%",
                        "display": false,
                        "padding": 5,
                        "font": { "size": 11 },
                    },
                    "position": "left",
                    "grid": { "display": true },
                    "ticks": {
                        ...options["left"]?.ticks,
                        "color": "",
                        "padding": 5,
                        "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                    },
                    "beginAtZero": true,
                    "type": "linear",
                },
            },
        },
    });
}

describe("PersistencePriority", () => {
    it("#isLessThan", () => {
        expect(PERSISTENCE_PRIORITY.IS_LESS_THAN(PERSISTENCE_PRIORITY.LOW, PERSISTENCE_PRIORITY.HIGH)).toBe(true);
        expect(PERSISTENCE_PRIORITY.IS_LESS_THAN(PersistencePriority.VERY_HIGH, PERSISTENCE_PRIORITY.HIGH)).toBe(false);
        expect(PERSISTENCE_PRIORITY.IS_LESS_THAN(PERSISTENCE_PRIORITY.HIGH, PERSISTENCE_PRIORITY.HIGH)).toBe(false);
        expect(PERSISTENCE_PRIORITY.IS_LESS_THAN(null, PERSISTENCE_PRIORITY.HIGH)).toBe(false);
        expect(PERSISTENCE_PRIORITY.IS_LESS_THAN(undefined, PERSISTENCE_PRIORITY.HIGH)).toBe(false);
        expect(PERSISTENCE_PRIORITY.IS_LESS_THAN(undefined, null)).toBe(false);
    });
});

describe("hasPropertyValue", () => {

    const component = new EDGE_CONFIG.COMPONENT("component0", "", true, "factoryId", {
        "booleanValue": true,
        "booleanValueString": "true",
        "numberValueStrng": "42",
    });

    it("#booleanValue", () => {
        expect(COMPONENT.HAS_PROPERTY_VALUE("booleanValue", true)).toBeTrue();
    });

    it("#booleanValueString", () => {
        expect(COMPONENT.HAS_PROPERTY_VALUE("booleanValueString", true)).toBeTrue();
    });

    it("#wrongEquals", () => {
        expect(COMPONENT.HAS_PROPERTY_VALUE("booleanValueString", false)).toBeFalse();
    });

    it("#compareWrongTypes", () => {
        expect(COMPONENT.HAS_PROPERTY_VALUE("numberValueStrng", 42)).toBeTrue();
    });
});
