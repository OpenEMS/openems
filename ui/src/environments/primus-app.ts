import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from '../app/shared/service/defaulttypes';

class PrimusAppEnvironment extends Environment {
  public readonly production = false;
  public readonly url = "wss://www.energydepot.de/primus-ui-dev";
  public readonly backend: DefaultTypes.Backend = "OpenEMS Backend";
  public debugMode = true;
}

export const environment = new PrimusAppEnvironment();
