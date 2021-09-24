import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "Heckert",

    uiTitle: "Heckert Solar Symphon-E Online Monitoring",
    edgeShortName: "EMS",
    edgeLongName: "Heckert Solar Symphon-E Energiemanagementsystem",

    backend: 'OpenEMS Backend',
    url: "ws://" + location.hostname + ":8082",

    production: false,
    debugMode: true,
};