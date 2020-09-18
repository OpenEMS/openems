import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
  production: false,
  debugMode: true,
  // gitpod puts the port number in front of the hostname
  url: "wss://8082-" + location.hostname.substring(location.hostname.indexOf("-") + 1),
  backend: "OpenEMS Backend"
};
