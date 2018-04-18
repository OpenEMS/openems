import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from "../app/shared/service/defaulttypes";

class DefaultEnvironment extends Environment {
  public readonly production = false;
  public debugMode = true;
  // For OpenEMS Edge
  public readonly url = "ws://" + location.hostname + ":8085";
  public readonly backend: DefaultTypes.Backend = "OpenEMS Edge";
  // For OpenEMS Backend
  // public readonly url = "ws://" + location.hostname + ":8078";
  // public readonly backend: DefaultTypes.Backend = "OpenEMS Backend";
}

export const environment = new DefaultEnvironment();