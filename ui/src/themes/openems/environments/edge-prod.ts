import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "OpenEMS",

    title: "OpenEMS UI",
    shortName: "OpenEMS",

    backend: 'OpenEMS Edge',
    url: "ws://" + location.hostname + ":8085",

    production: true,
    debugMode: false,
};