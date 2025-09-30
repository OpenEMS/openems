import { Environment , getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Edge",
        url: getWebsocketScheme() + LOCATION.HOSTNAME + ":8075",

        production: true,
        debugMode: false,
    },
};
