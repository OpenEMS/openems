import { CustomFieldDefinition } from './customfielddefinition';
import { Backend } from './backend';
import { Config } from "../device/config";

export abstract class Environment {
  public readonly abstract production: boolean;
  public readonly abstract url: string;
  public readonly abstract backend: Backend;

  public getCustomFields(config: Config): CustomFieldDefinition {
    return {};
  }
}