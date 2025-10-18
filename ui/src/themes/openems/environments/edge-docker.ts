// @ts-strict-ignore
import { Environment, getWebsocketScheme } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{
        backend: "OpenEMS Edge",
        url: `${getWebsocketScheme()}//${location.host}/openems-edge`,

        production: true,
        debugMode: false,
    },
};
