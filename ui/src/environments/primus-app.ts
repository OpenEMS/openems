import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from '../app/shared/service/defaulttypes';

class PrimusAppEnvironment extends Environment {
  public readonly production = true;
  public readonly url = "wss://www.energydepot.de/wss";

  public readonly backend: DefaultTypes.Backend = "App";
  public debugMode = false;
}

export const environment = new PrimusAppEnvironment();
