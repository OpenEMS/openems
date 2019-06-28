import { TranslateService } from '@ngx-translate/core';
import { format, isSameDay } from 'date-fns';

export module DefaultTypes {

  export type Backend = "OpenEMS Backend" | "OpenEMS Edge" | "App";

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

  export class HistoryPeriod {

    constructor(
      public from: Date = new Date(),
      public to: Date = new Date(),
    ) { }

    public getText(translate: TranslateService): string {
      if (!isSameDay(this.from, this.to)) {
        return translate.instant(
          'General.PeriodFromTo', {
            value1: format(this.from, translate.instant('General.DateFormat')),
            value2: format(this.to, translate.instant('General.DateFormat'))
          })
      }
      else if (!isSameDay(this.from, new Date())) {
        return translate.instant('Edge.History.Yesterday') + ", " + format(this.from, translate.instant('General.DateFormat'));
      }
      else {
        return translate.instant('Edge.History.Today') + ", " + format(new Date(), translate.instant('General.DateFormat'));
      }
    }
  }
}