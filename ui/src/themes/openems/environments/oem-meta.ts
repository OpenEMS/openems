import { BaseMeta } from "src/environments";

export const OemMeta: BaseMeta = {
    images: {
        EVSE: {
            KEBA_P30: null,
            KEBA_P40: null,
            HARDY_BARTH: null,
        },
    },
    links: {
        DATA_PROTECTION: null,
        COMMON_STORAGE: null,
        FORGET_PASSWORD: "#",
        EVCS: null,

        CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE: "IO.OPENEMS.EDGE.CONTROLLER.ESS.GRIDOPTIMIZEDCHARGE/README.ADOC",
        CONTROLLER_CHP_SOC: "IO.OPENEMS.EDGE.CONTROLLER.CHP.SOC/README.ADOC",
        CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD: "IO.OPENEMS.EDGE.CONTROLLER.IO.CHANNELSINGLETHRESHOLD/README.ADOC",
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT: "IO.OPENEMS.EDGE.CONTROLLER.IO.FIXDIGITALOUTPUT/README.ADOC",
        CONTROLLER_IO_HEAT_PUMP_SG_READY: "IO.OPENEMS.EDGE.CONTROLLER.IO.HEATPUMP.SGREADY/README.ADOC",
        CONTROLLER_IO_HEATING_ELEMENT: "IO.OPENEMS.EDGE.CONTROLLER.IO.HEATINGELEMENT/README.ADOC",
        CONTROLLER_ESS_TIME_OF_USE_TARIFF: "IO.OPENEMS.EDGE.CONTROLLER.ESS.TIMEOFUSETARIFF/README.ADOC",

        CONTROLLER_API_MODBUSTCP_READ: "IO.OPENEMS.EDGE.CONTROLLER.API.MODBUS/README.ADOC",
        CONTROLLER_API_MODBUSTCP_READWRITE: "IO.OPENEMS.EDGE.CONTROLLER.API.MODBUS/README.ADOC",

        CONTROLLER_API_REST_READ: "IO.OPENEMS.EDGE.CONTROLLER.API.REST/README.ADOC",
        CONTROLLER_API_REST_READWRITE: "IO.OPENEMS.EDGE.CONTROLLER.API.REST/README.ADOC",

        SETTINGS_ALERTING: null,
        SETTINGS_NETWORK_CONFIGURATION: null,
        EVCS_CLUSTER: "IO.OPENEMS.EDGE.EVCS.CLUSTER/README.ADOC",

        SYSTEM: {
            INDUSTRIAL_S: "#",
            INDUSTRIAL_L: "#",
        },

        WARRANTY: {
            HOME: {
                EN: "#",
                DE: "#",
            },
            COMMERCIAL: {
                EN: "#",
                DE: "#",
            },
        },

        GTC: {
            EN: "#",
            DE: "#",
        },

        METER: {
            SOCOMEC: "#",
            KDK: "#",
        },

        MANUALS: {
            SYSTEM: {
                HOME: {
                    HOME_10: "#",
                    HOME_20_30: "#",
                    HOME_GEN_2: "#",
                },
                COMMERCIAL: {
                    COMMERCIAL_30: "#",
                    COMMERCIAL_50: "#",
                    COMMERCIAL_92: "#",
                    COMMERCIAL_92_CLUSTER: "#",
                },
            },

            RUNDSTEUER: {
                HOME: "#",
                HOME_GEN_2: "#",
            },

            AVU: "#",
        },
        APP_CENTER: {
            APP_IMAGE: (language: string, appId: string): string | null => {
                return null;
            },
        },
        APP: {
            ANDROID: null,
            IOS: null,
        },
        ENERGY_JOURNEY: {
            HOME_10: {
                DE: "#",
                EN: "#",
            },
            HOME_20_30: {
                DE: "#",
                EN: "#",
            },
            HOME_6_10_15: {
                DE: "#",
                EN: "#",
            },
        },
    },
};
