import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        url: "ws://" + location.hostname + ":8082",

        production: true,
        debugMode: false,
    },
};
