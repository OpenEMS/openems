import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "Heckert",

    title: "Heckert Solar Symphon-E Online Monitoring",
    shortName: "EMS",

    backend: 'OpenEMS Backend',
    url: "ws://" + location.hostname + ":8082",

    production: false,
    debugMode: true,
};