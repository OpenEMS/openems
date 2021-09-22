import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "Heckert",

    uiTitle: "Heckert Solar Symphon-E Online Monitoring",
    edgeShortName: "EMS",
    edgeLongName: "Heckert Solar Symphon-E Energiemanagementsystem",

    backend: 'OpenEMS Backend',
    url: "wss://portal.fenecon.de/openems-backend-ui2",

    production: true,
    debugMode: false,
};