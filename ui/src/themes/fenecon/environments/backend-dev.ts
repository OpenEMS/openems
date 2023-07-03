import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        url: "wss://portal.fenecon.de/openems-backend-ui2",

        production: false,
        debugMode: true
    }
};