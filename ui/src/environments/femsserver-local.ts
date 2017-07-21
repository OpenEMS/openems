import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class FemsserverLocalEnvironment extends Environment {
  public readonly production = false;

  public readonly websockets = [{
    name: location.hostname,
    url: "ws://" + location.hostname + ":8089",
    backend: Backend.FemsServer
  }];
}

export const environment = new FemsserverLocalEnvironment();
