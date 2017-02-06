import { Environment } from "./environment.type";

export const environment: Environment = {
  production: false,
  //backend: "openems",
  //url: "ws://" + location.hostname + ":80/websocket"
  backend: "femsserver",
  url: "ws://localhost:8078/websocket"
};
