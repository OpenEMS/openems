import { Theme } from "src/environments";

export const theme = {
    theme: "OpenEMS" as Theme,

    uiTitle: "OpenEMS UI",
    edgeShortName: "OpenEMS",
    edgeLongName: "Open Energy Management System",

    docsUrlPrefix: "https://github.com/OpenEMS/openems/blob/develop/",
    links: {
        COMMON_STORAGE: null,
        FORGET_PASSWORD: "#",
        EVCS_HARDY_BARTH: "io.openems.edge.evcs.hardybarth/readme.adoc",
        EVCS_KEBA_KECONTACT: "io.openems.edge.evcs.keba.kecontact/readme.adoc",
        EVCS_OCPP_IESKEYWATTSINGLE: "io.openems.edge.evcs.ocpp.ies.keywatt.singleccs/readme.adoc",

        CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE: "io.openems.edge.controller.ess.gridoptimizedcharge/readme.adoc",
        CONTROLLER_CHP_SOC: "io.openems.edge.controller.chp.soc/readme.adoc",
        CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD: "io.openems.edge.controller.io.channelsinglethreshold/readme.adoc",
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT: "io.openems.edge.controller.io.fixdigitaloutput/readme.adoc",
        CONTROLLER_IO_HEAT_PUMP_SG_READY: "io.openems.edge.controller.io.heatpump.sgready/readme.adoc",
        CONTROLLER_IO_HEATING_ELEMENT: "io.openems.edge.controller.io.heatingelement/readme.adoc",
        CONTROLLER_ESS_TIME_OF_USE_TARIFF: "io.openems.edge.controller.ess.timeofusetariff/readme.adoc",

        CONTROLLER_API_MODBUSTCP_READ: "io.openems.edge.controller.api.modbus/readme.adoc",
        CONTROLLER_API_MODBUSTCP_READWRITE: "io.openems.edge.controller.api.modbus/readme.adoc",

        CONTROLLER_API_REST_READ: "io.openems.edge.controller.api.rest/readme.adoc",
        CONTROLLER_API_REST_READWRITE: "io.openems.edge.controller.api.rest/readme.adoc",

        SETTINGS_ALERTING: null,
        SETTINGS_NETWORK_CONFIGURATION: null,
        EVCS_CLUSTER: "io.openems.edge.evcs.cluster/readme.adoc",

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
            HOME: {
                HOME_10: "#",
                HOME_20_30: "#",
            },
            COMMERCIAL: {
                COMMERCIAL_30: "#",
                COMMERCIAL_50: "#",
            },
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
    },
    PRODUCT_TYPES: () => null,
};
