import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from '../app/shared/service/defaulttypes';

class OpenemsBackendDevEnvironment extends Environment {
  public readonly production = false;
  public readonly url = "ws://192.168.221.1/websocket";
  public readonly backend: DefaultTypes.Backend = "OpenEMS Backend"
  public debugMode = false;
}

export const environment = new OpenemsBackendDevEnvironment();
