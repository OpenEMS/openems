import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "FENECON",

    uiTitle: "FENECON Online-Monitoring",
    edgeShortName: "FEMS",
    edgeLongName: "FENECON Energiemanagementsystem",

    backend: 'OpenEMS Backend',
    url: "wss://portal.fenecon.de/openems-backend-ui2",

    production: false,
    debugMode: true,
};