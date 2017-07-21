import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class DefaultEnvironment extends Environment {
  public readonly production = false;

  public readonly websockets = [{
    name: "FEMS",
    url: "ws://" + location.hostname + ":8085",
    backend: Backend.OpenEMS
  }];
}

export const environment = new DefaultEnvironment();