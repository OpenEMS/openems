import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        url: "wss://srv0.fenecon.de/ui",

        production: false,
        debugMode: true,
    }
};