import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Edge',
        url: "ws://" + location.hostname + ":8085",

        production: false,
        debugMode: true,
    }
};