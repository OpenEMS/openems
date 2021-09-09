import { Environment } from "src/environments";

export const environment: Environment = {
    theme: "OpenEMS",

    title: "OpenEMS UI",
    shortName: "OpenEMS",

    backend: 'OpenEMS Edge',
    url: "ws://" + location.hostname + ":8075",

    production: false,
    debugMode: true,
};
