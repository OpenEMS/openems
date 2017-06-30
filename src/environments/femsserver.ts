import { Environment } from "./environment.type";

export const environment: Environment = {
  production: true,
  websockets: [{
    name: location.hostname,
    url: "wss://fenecon.de:443/femsmonitor",
    backend: "femsserver"
  }]
};