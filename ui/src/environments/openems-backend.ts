import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
  production: true,
  debugMode: false,
  url: (location.protocol == "https:" ? "wss" : "ws") +
    "://" + location.hostname + ":" + location.port + "/openems-backend-ui2",
  backend: "OpenEMS Backend",
};