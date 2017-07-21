import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class OpenemsEnvironment extends Environment {
  public readonly production = true;

  public readonly websockets = [{
    name: location.hostname,
    url: "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket",
    backend: Backend.OpenEMS
  }];
}

export const environment = new OpenemsEnvironment();
