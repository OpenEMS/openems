import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "FENECON",

    uiTitle: "FENECON Online-Monitoring",
    edgeShortName: "FEMS",
    edgeLongName: "FENECON Energiemanagementsystem",

    backend: 'OpenEMS Edge',
    url: "ws://" + location.hostname + ":8085",

    production: false,
    debugMode: true,
};