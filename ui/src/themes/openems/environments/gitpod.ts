// @ts-strict-ignore
import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        // gitpod puts the port number in front of the hostname
        url: "wss://8082-" + location.hostname.substring(location.hostname.indexOf("-") + 1),

        production: false,
        debugMode: true,
    },
};
