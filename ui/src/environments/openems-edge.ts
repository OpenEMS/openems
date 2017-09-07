import { Environment } from "../app/shared/type/environment";
import { Backend } from "../app/shared/type/backend";

class OpenemsEnvironment extends Environment {
  public readonly production = true;
  public readonly url = "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket";
  public readonly backend = Backend.OpenEMS_Edge;
}

export const environment = new OpenemsEnvironment();
