import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class OpenemsBackendDevEnvironment extends Environment {
  public readonly production = false;
  public readonly url = "ws://" + location.hostname + ":8078";
  public readonly backend = Backend.OpenEMS_Backend;
}

export const environment = new OpenemsBackendDevEnvironment();
