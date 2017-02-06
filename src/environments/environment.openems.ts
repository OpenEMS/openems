import { Environment } from "./environment.type";

export const environment: Environment = {
  production: true,
  backend: "openems",
  url: "ws://" + location.hostname + ":" + location.port + "/websocket"
};
