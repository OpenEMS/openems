import { Environment } from "./environment.type";

export const environment: Environment = {
  production: false,
  websockets: [{
    name: location.hostname,
    url: "ws://" + location.hostname + ":8089",
    backend: "femsserver"
  }]
};