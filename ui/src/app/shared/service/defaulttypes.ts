import { Role } from '../type/role'

export module DefaultTypes {

  export type Backend = "OpenEMS Backend" | "OpenEMS Edge";

  export type ConnectionStatus = "online" | "connecting" | "waiting for authentication" | "failed";

  export interface ChannelAddresses {
    [thing: string]: string[];
  }

  export interface ThingConfig {
    id: string,
    class: string | string[],
    [channel: string]: any
  }

  export interface Config {
    things: {
      [id: string]: ThingConfig
    },
    meta: {
      [clazz: string]: {
        implements: string[],
        channels: {
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
      chargeActivePower: number,
      chargeActivePowerAC: number,
      chargeActivePowerDC: number,
      maxChargeActivePower: number,
      dischargeActivePower: number,
      dischargeActivePowerAC: number,
      dischargeActivePowerDC: number,
      maxDischargeActivePower: number
    }, production: {
      powerRatio: number,
      activePower: number, // sum of activePowerAC and activePowerDC
      activePowerAC: number,
      activePowerDC: number,
      maxActivePower: number
    }, grid: {
      powerRatio: number,
      buyActivePower: number,
      maxBuyActivePower: number,
      sellActivePower: number,
      maxSellActivePower: number
    }, consumption: {
      powerRatio: number,
      activePower: number
    }
  }

  export interface MessageMetadataDevice {
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

  export interface OutgoingMessage {
    id: string[]
  }

  export interface ConfigUpdate extends OutgoingMessage {
    config: {
      mode: "update",
      thing: string,
      channel: string,
      value: any
    }
  }
}