import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from '../app/shared/service/defaulttypes';

class OpenemsBackendEnvironment extends Environment {
  public readonly production = true;
  public readonly url = (location.protocol == "https:" ? "wss" : "ws") +
  "://" + location.hostname + ":" + location.port + "/openems-backend-ui";
  public readonly backend: DefaultTypes.Backend = "OpenEMS Backend";
}

export const environment = new OpenemsBackendEnvironment();
