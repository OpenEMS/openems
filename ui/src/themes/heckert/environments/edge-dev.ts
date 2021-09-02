import { Environment } from "src/environments";

export const environment: Environment = {
    title: "Heckert Solar Symphon-E Online Monitoring",
    shortName: "EMS",

    backend: 'OpenEMS Edge',
    url: "ws://" + location.hostname + ":8085",

    production: false,
    debugMode: true,
};