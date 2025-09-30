import { Environment , getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Backend",
        url: getWebsocketScheme() + LOCATION.HOSTNAME + ":8082",

        production: false,
        debugMode: true,
    },
};
