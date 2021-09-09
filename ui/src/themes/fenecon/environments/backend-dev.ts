import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "FENECON",

    title: "FENECON Online-Monitoring",
    shortName: "FEMS",

    backend: 'OpenEMS Backend',
    url: "ws://" + location.hostname + ":8082",

    production: false,
    debugMode: true,
};