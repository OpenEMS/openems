import { Theme } from 'src/environments';

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

        SETTINGS_ALERTING: null,
        SETTINGS_NETWORK_CONFIGURATION: null
    },
    PRODUCT_TYPES: () => null
};