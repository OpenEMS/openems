import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class VA6403Environment extends Environment {
  public readonly production = true;

  public readonly websockets = [{
    name: "Station D04",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d04",
    backend: Backend.OpenEMS
  }, {
    name: "Station D02",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d02",
    backend: Backend.OpenEMS
  }, {
    name: "Station J08",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-j08",
    backend: Backend.OpenEMS
  }, {
    name: "Station D12",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d12",
    backend: Backend.OpenEMS
  }, {
    name: "Station D10",
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket-d10",
    backend: Backend.OpenEMS
  }];
}

export const environment = new VA6403Environment();
