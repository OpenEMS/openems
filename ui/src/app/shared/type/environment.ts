import { CustomFieldDefinition } from './customfielddefinition';
import { Backend } from './backend';
import { Config } from "../device/config";

export abstract class Environment {
  public readonly abstract production: boolean;

  public abstract websockets: {
    name: string,
    url: string,
    backend: Backend
  }[];

  public getCustomFields(config: Config): CustomFieldDefinition {
    return {};
  }
}