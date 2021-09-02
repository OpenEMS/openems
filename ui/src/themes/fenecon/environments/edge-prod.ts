import { Environment } from "src/environments";

export const environment: Environment = {
    title: "FENECON Online-Monitoring",
    shortName: "FEMS",

    backend: 'OpenEMS Edge',
    url: "ws://" + location.hostname + ":8085",

    production: true,
    debugMode: false,
};