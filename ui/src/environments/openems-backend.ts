import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class OpenemsBackendEnvironment extends Environment {
  public readonly production = true;
  public readonly url = (location.protocol == "https" ? "wss" : "ws") +
  "://" + location.hostname + ":" + location.port + "/femsmonitor";
  public readonly backend = Backend.OpenEMS_Backend;
}

export const environment = new OpenemsBackendEnvironment();
