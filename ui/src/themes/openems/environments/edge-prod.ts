import { Environment } from "src/environments";
import { getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Edge",
        url: getWebsocketScheme() + location.hostname + ":8075",

        production: true,
        debugMode: false,
    },
};
