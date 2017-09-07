import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from '../app/shared/service/defaulttypes';

class OpenemsEnvironment extends Environment {
  public readonly production = true;
  public readonly url = "ws://" + location.hostname + (location.port ? ":" + location.port : "") + "/websocket";
  public readonly backend: DefaultTypes.Backend = "OpenEMS Edge";
}

export const environment = new OpenemsEnvironment();
