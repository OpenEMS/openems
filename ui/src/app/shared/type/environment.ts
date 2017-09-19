import { CustomFieldDefinition } from './customfielddefinition';
import { DefaultTypes } from '../service/defaulttypes';

export abstract class Environment {
  public readonly abstract production: boolean;
  public readonly abstract url: string;
  public readonly abstract backend: DefaultTypes.Backend;
  public debugMode: boolean = false;

  public getCustomFields(): CustomFieldDefinition {
    return {};
  }
}