import { Environment , getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Edge",
        url: getWebsocketScheme() + location.hostname + ":8075",

        production: true,
        debugMode: false,
        loginWithUsername: false,
    },
};
