import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class DefaultEnvironment extends Environment {
  public readonly production = false;
  public readonly url = "ws://" + location.hostname + ":8085";
  public readonly backend = Backend.OpenEMS_Edge;
}

export const environment = new DefaultEnvironment();