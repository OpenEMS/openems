import { Environment } from "src/environments";
import { theme } from "./theme";

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Backend',
        //url: "ws://" + location.hostname + ":8082/openems-backend-ui",
        url: "https://openems-backend.partec.org:8285/openems-backend-ui",

        production: true,
        debugMode: false,
    },
};
