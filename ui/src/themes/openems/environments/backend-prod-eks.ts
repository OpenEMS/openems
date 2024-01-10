import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        url: "wss://" + location.hostname + "/openems-backend",

        production: true,
        debugMode: false,
    },
};
