import { Role } from '../type/role'

export module DefaultTypes {

  export type Backend = "OpenEMS Backend" | "OpenEMS Edge";

  export type ConnectionStatus = "online" | "connecting" | "waiting for authentication" | "failed";

  export interface ChannelAddresses {
    [componentId: string]: string[];
  }

  /**
   * CurrentData Summary
   * 
   * ratio is [-1,1]
   */
  export interface Summary {
    system: {
      // the balance sheet total power of all power that enters the the system (production, discharge, buy-from-grid), respectively leaves the system (consumption, charge, sell-to-grid)
      totalPower: number,
    }, storage: {
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
      powerRatio: number,
      maxApparentPower: number
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
      gridMode: number
    }, consumption: {
      powerRatio: number,
      activePower: number
    }
  }

  export type NotificationType = "success" | "error" | "warning" | "info";

  export interface Notification {
    type: NotificationType;
    message: string;
    code?: number,
    params?: string[]
  }

}