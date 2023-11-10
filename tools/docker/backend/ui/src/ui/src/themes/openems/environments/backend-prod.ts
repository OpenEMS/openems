import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket",

        production: true,
        debugMode: false
    }
};
