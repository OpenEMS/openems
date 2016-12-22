import { Device } from './device';
export { Device } from './device';

class ThingConfig {
  id: String;
  class: String;
}

class SchedulerConfig extends ThingConfig {
  controllers: Object[] = [];
}

export class InfluxdbPersistence {
  ip: string;
  username: string;
  password: string;
  fems: number;
}

interface ControllerDefinition {
  class: string;
}

export class OpenemsConfig {
  public _devices: { [id: string]: Device } = {};
  public _controllers: ControllerDefinition[];
  public things: ThingConfig[] = [];
  public scheduler: SchedulerConfig = new SchedulerConfig();
  public persistence: Object[] = [];

  public getInfluxdbPersistence(): InfluxdbPersistence {
    for (let persistence of this.persistence) {
      if (persistence instanceof InfluxdbPersistence) {
        return persistence as InfluxdbPersistence;
      }
    };
    return null;
  }
}