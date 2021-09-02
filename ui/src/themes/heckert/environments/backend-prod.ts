import { Environment } from "src/environments";

export const environment: Environment = {
    title: "Heckert Solar Symphon-E Online Monitoring",
    shortName: "EMS",

    backend: 'OpenEMS Backend',
    url: "wss://portal.fenecon.de/openems-backend-ui2",

    production: true,
    debugMode: false,
};