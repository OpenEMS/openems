import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class FemsserverDevEnvironment extends Environment {
  public readonly production = false;

  public readonly websockets = [{
    name: location.hostname,
    url: "ws://" + location.hostname + ":8078",
    backend: Backend.FemsServer
  }];
}

export const environment = new FemsserverDevEnvironment();
