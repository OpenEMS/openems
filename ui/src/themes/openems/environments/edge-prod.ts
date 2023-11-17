import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Edge',
        url: "ws://" + location.hostname + ":8075",

        production: true,
        debugMode: false
    }
};
