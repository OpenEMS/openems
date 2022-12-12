import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        // url: "wss://srv0.fenecon.de/ui",
        url: "ws://localhost:8082",

        production: false,
        debugMode: true,
    }
};