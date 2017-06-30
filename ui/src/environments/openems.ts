import { Environment } from "./environment.type";

export const environment: Environment = {
  production: true,
  websockets: [{
    name: location.hostname,
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket",
    backend: "openems"
  }]
};
