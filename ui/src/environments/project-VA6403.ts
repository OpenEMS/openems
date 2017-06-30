import { Environment } from "./environment.type";

export const environment: Environment = {
  production: true,
  websockets: [{
    name: "Station D04",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d04",
    backend: "openems"
  }, {
    name: "Station D02",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d02",
    backend: "openems"
  }, {
    name: "Station J08",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-j08",
    backend: "openems"
  }, {
    name: "Station D12",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d12",
    backend: "openems"
  }, {
    name: "Station D10",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d10",
    backend: "openems"
  }]
};
