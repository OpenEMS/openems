import { Environment } from "src/environments";
import { getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Backend",
        url: getWebsocketScheme() + location.hostname + ":8082",

        production: true,
        debugMode: false,
    },
};
