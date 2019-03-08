import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
  production: false,
  debugMode: true,
  url: "ws://" + location.hostname + ":8078",
  backend: "OpenEMS Backend",
};
