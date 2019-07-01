import { TranslateService } from '@ngx-translate/core';
import { format, isSameDay, subDays, getDay } from 'date-fns';

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
      if (isSameDay(this.from, this.to) && isSameDay(this.from, new Date())) {
        return translate.instant('Edge.History.Today') + ", " + format(new Date(), translate.instant('General.DateFormat'));
      }
      else if (isSameDay(this.from, this.to) && !isSameDay(this.from, subDays(new Date(), 1))) {
        switch (getDay(this.from)) {
          case 0: {
            return translate.instant('General.Week.Sunday') + ", " + translate.instant('Edge.History.SelectedDay', {
              value: format(this.from, translate.instant('General.DateFormat'))
            })
          }
          case 1: {
            return translate.instant('General.Week.Monday') + ", " + translate.instant('Edge.History.SelectedDay', {
              value: format(this.from, translate.instant('General.DateFormat'))
            })
          }
          case 2: {
            return translate.instant('General.Week.Tuesday') + ", " + translate.instant('Edge.History.SelectedDay', {
              value: format(this.from, translate.instant('General.DateFormat'))
            })
          }
          case 3: {
            return translate.instant('General.Week.Wednesday') + ", " + translate.instant('Edge.History.SelectedDay', {
              value: format(this.from, translate.instant('General.DateFormat'))
            })
          }
          case 4: {
            return translate.instant('General.Week.Thursday') + ", " + translate.instant('Edge.History.SelectedDay', {
              value: format(this.from, translate.instant('General.DateFormat'))
            })
          }
          case 5: {
            return translate.instant('General.Week.Friday') + ", " + translate.instant('Edge.History.SelectedDay', {
              value: format(this.from, translate.instant('General.DateFormat'))
            })
          }
          case 6: {
            return translate.instant('General.Week.Saturday') + ", " + translate.instant('Edge.History.SelectedDay', {
              value: format(this.from, translate.instant('General.DateFormat'))
            })
          }
        }
      }
      else if (isSameDay(this.from, this.to) && isSameDay(this.from, subDays(new Date(), 1))) {
        return translate.instant('Edge.History.Yesterday') + ", " + format(this.from, translate.instant('General.DateFormat'));
      }
      else {
        {
          return translate.instant(
            'General.PeriodFromTo', {
              value1: format(this.from, translate.instant('General.DateFormat')),
              value2: format(this.to, translate.instant('General.DateFormat'))
            })
        }
      }
    }
  }
}