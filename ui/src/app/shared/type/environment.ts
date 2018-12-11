import { DefaultTypes } from '../service/defaulttypes';

export interface Environment {
  readonly production: boolean;
  readonly url: string;
  readonly backend: DefaultTypes.Backend;
  debugMode: boolean;
}