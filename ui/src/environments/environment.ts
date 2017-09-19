import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from "../app/shared/service/defaulttypes";

class DefaultEnvironment extends Environment {
  public readonly production = false;
  public readonly url = "ws://" + location.hostname + ":8085";
  public readonly backend: DefaultTypes.Backend = "OpenEMS Edge";
  public debugMode = true;
}

export const environment = new DefaultEnvironment();