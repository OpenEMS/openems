import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "FENECON",

    uiTitle: "FENECON Online-Monitoring",
    edgeShortName: "FEMS",
    edgeLongName: "FENECON Energiemanagementsystem",

    backend: 'OpenEMS Backend',
    url: "wss://srv0.fenecon.de/ui",

    production: false,
    debugMode: true,
};