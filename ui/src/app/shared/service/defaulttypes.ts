import { Role } from '../type/role'

export module DefaultTypes {

  export type Backend = "OpenEMS Backend" | "OpenEMS Edge";

  export type ConnectionStatus = "online" | "connecting" | "waiting for authentication" | "failed";

  export interface ChannelAddresses {
    [thing: string]: string[];
  }

  export interface ComponentConfig {
    'service.pid': string, // unique pid of configuration
    'service.factoryPid': string, // link to 'meta'
    enabled: boolean,
    [channel: string]: string | number | boolean
  }

  export interface ThingConfig {
    id: string,
    class: string | string[],
    [channel: string]: any
  }

  export interface Config {
    meta: {
      [factoryPid: string]: {
        implements: string[],
        channels?: {
          [channel: string]: {
            name: string,
            title: string,
            type: string | string[],
            optional: boolean,
            array: boolean,
            readRoles: Role[],
            writeRoles: Role[]
          }
        }
      }
    }
  }

  export interface Config_2018_8 extends Config {
    components?: {
      [id: string]: ComponentConfig
    },
  }

  export interface Config_2018_7 extends Config {
    things?: {
      [id: string]: ThingConfig
    },
  }

  export interface Data {
    [thing: string]: {
      [channel: string]: any
    }
  }

  export interface HistoricData {
    data: [{
      time: string,
      channels: Data
    }]
  }

  export interface Summary {
    storage: {
      soc: number,
      isAsymmetric: boolean,
      hasDC: boolean,
      chargeActivePower: number,
      chargeActivePowerAC: number,
      chargeActivePowerACL1: number,
      chargeActivePowerACL2: number,
      chargeActivePowerACL3: number,
      chargeActivePowerDC: number,
      maxChargeActivePower?: number,
      dischargeActivePower: number,
      dischargeActivePowerAC: number,
      dischargeActivePowerACL1: number,
      dischargeActivePowerACL2: number,
      dischargeActivePowerACL3: number,
      dischargeActivePowerDC: number,
      maxDischargeActivePower?: number,
      powerRatio: number
    }, production: {
      powerRatio: number,
      isAsymmetric: boolean,
      hasDC: boolean,
      activePower: number, // sum of activePowerAC and activePowerDC
      activePowerAC: number,
      activePowerACL1: number,
      activePowerACL2: number,
      activePowerACL3: number,
      activePowerDC: number,
      maxActivePower: number
    }, grid: {
      powerRatio: number,
      buyActivePower: number,
      maxBuyActivePower: number,
      sellActivePower: number,
      maxSellActivePower: number,
      gridMode: number,
    }, consumption: {
      powerRatio: number,
      activePower: number
    }
  }

  export interface MessageMetadataEdge {
    id: number,
    name: string,
    comment: string,
    producttype: string,
    role: string,
    online: boolean
  }

  export type NotificationType = "success" | "error" | "warning" | "info";

  export interface Notification {
    type: NotificationType;
    message: string;
    code?: number,
    params?: string[]
  }

  export interface Log {
    time: number | string,
    level: string,
    source: string,
    message: string,
    color?: string /* is added later */
  }

  export type LanguageTag = "de" | "en" | "cz" | "nl";

  export interface IdentifiedMessage {
    messageId: {
      ui: string,
      backend?: string
    },
    edgeId?: number,
    [thing: string]: {}
  }

  export interface ConfigUpdate extends IdentifiedMessage {
    config: {
      mode: "update",
      thing: string,
      channel: string,
      value: any
    }
  }
}