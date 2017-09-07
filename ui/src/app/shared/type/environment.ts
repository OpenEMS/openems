import { CustomFieldDefinition } from './customfielddefinition';
import { Backend } from './backend';

export abstract class Environment {
  public readonly abstract production: boolean;
  public readonly abstract url: string;
  public readonly abstract backend: Backend;

  public getCustomFields(): CustomFieldDefinition {
    return {};
  }
}