import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
    production: false,
    debugMode: true,
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket",
    backend: "OpenEMS Edge",
};
