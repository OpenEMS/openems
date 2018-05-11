import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from "../app/shared/service/defaulttypes";

class DefaultEnvironment extends Environment {
  public readonly production = false;
  // For OpenEMS Edge
  public readonly url = "ws://" + location.hostname + ":8085";
  public readonly backend: DefaultTypes.Backend = "OpenEMS Edge";
  // For OpenEMS Backend
  // public readonly url = "ws://" + location.hostname + ":8087";
  // public readonly backend: DefaultTypes.Backend = "OpenEMS Backend";
  public debugMode = true;
}

export const environment = new DefaultEnvironment();