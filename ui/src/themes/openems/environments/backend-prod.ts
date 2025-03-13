import { Environment , getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Backend",
        url: getWebsocketScheme() + location.hostname + ":8082",

        production: true,
        debugMode: false,
        loginWithUsername: false,
    },
};
