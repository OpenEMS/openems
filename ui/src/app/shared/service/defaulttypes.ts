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
      chargeActivePower: number,
      chargeActivePowerAC: number,
      chargeActivePowerDC: number,
      maxChargeActivePower?: number,
      dischargeActivePower: number,
      dischargeActivePowerAC: number,
      dischargeActivePowerDC: number,
      maxDischargeActivePower?: number,
      powerRatio: number,
      maxApparentPower: number,
      effectiveChargePower: number,
      effectiveDischargePower: number,
    }, production: {
      powerRatio: number,
      hasDC: boolean,
      activePower: number, // sum of activePowerAC and activePowerDC
      activePowerAC: number,
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