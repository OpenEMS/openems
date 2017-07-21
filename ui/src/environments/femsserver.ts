import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class FemsserverEnvironment extends Environment {
  public readonly production = true;

  public readonly websockets = [{
    name: location.hostname,
    url: "wss://fenecon.de:443/femsmonitor",
    backend: Backend.FemsServer
  }];
}

export const environment = new FemsserverEnvironment();
