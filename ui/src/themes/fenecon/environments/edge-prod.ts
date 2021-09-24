import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "FENECON",

    uiTitle: "FENECON Online-Monitoring",
    edgeShortName: "FEMS",
    edgeLongName: "FENECON Energiemanagementsystem",

    backend: 'OpenEMS Edge',
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket",

    production: true,
    debugMode: false,
};