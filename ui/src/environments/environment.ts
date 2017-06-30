import { Environment } from "./environment.type";

export const environment: Environment = {
  production: false,
  websockets: [{
    name: "FEMS",
    url: "ws://" + location.hostname + ":8085",
    backend: "openems"
  }]
};
