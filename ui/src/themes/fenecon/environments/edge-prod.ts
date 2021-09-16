import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "FENECON",

    title: "FENECON Online-Monitoring",
    shortName: "FEMS",

    backend: 'OpenEMS Edge',
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket",

    production: true,
    debugMode: false,
};