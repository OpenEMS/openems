import { Environment } from "src/environments";

export const environment: Environment = {
    title: "FENECON Online-Monitoring",
    shortName: "FEMS",

    backend: 'OpenEMS Backend',
    url: "wss://portal.fenecon.de/openems-backend-ui2",

    production: true,
    debugMode: false,
};