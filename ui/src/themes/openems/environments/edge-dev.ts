import { Environment } from "src/environments";
import { getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: "OpenEMS Edge",
        url: getWebsocketScheme() + location.hostname + ":8085",

        production: false,
        debugMode: true,
    },
};
