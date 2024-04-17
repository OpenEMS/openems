import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Edge',
        url: window["env"]["websocket"],

        production: true,
        debugMode: false,
    },
};
