import { TranslateService } from '@ngx-translate/core';
import { format, getDay, isSameDay, subDays } from 'date-fns';
import { EdgeConfig } from '../shared';

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
      // autarchy in percent
      autarchy: number,
      // self consumption in percent
      selfConsumption: number,
      // state 0: Ok, 1: Info, 2: Warning, 3: Fault
      state: number
    }, storage: {
      soc: number,
      activePowerL1: number,
      activePowerL2: number,
      activePowerL3: number,
      effectiveActivePowerL1: number,
      effectiveActivePowerL2: number,
      effectiveActivePowerL3: number,
      chargeActivePower: number,
      chargeActivePowerAc: number,
      chargeActivePowerDc: number,
      maxChargeActivePower?: number,
      dischargeActivePower: number,
      dischargeActivePowerAc: number,
      dischargeActivePowerDc: number,
      maxDischargeActivePower?: number,
      powerRatio: number,
      maxApparentPower: number,
      effectivePower: number,
      effectiveChargePower: number,
      effectiveDischargePower: number,
      capacity: number,
    }, production: {
      powerRatio: number,
      hasDC: boolean,
      activePower: number, // sum of activePowerAC and activePowerDC
      activePowerAc: number,
      activePowerAcL1: number,
      activePowerAcL2: number,
      activePowerAcL3: number,
      activePowerDc: number,
      maxActivePower: number
    }, grid: {
      powerRatio: number,
      activePowerL1: number,
      activePowerL2: number,
      activePowerL3: number,
      buyActivePower: number,
      maxBuyActivePower: number,
      sellActivePower: number,
      sellActivePowerL1: number,
      sellActivePowerL2: number,
      sellActivePowerL3: number,
      maxSellActivePower: number,
      gridMode: number
    }, consumption: {
      powerRatio: number,
      activePower: number,
      activePowerL1: number,
      activePowerL2: number,
      activePowerL3: number
    }
  }

  export type NotificationType = "success" | "error" | "warning" | "info";

  export interface Notification {
    type: NotificationType;
    message: string;
    code?: number,
    params?: string[]
  }

  export type PeriodString = 'day' | 'week' | 'custom';

  export class HistoryPeriod {

    constructor(
      public from: Date = new Date(),
      public to: Date = new Date(),
    ) { }

    public getText(translate: TranslateService): string {
      if (isSameDay(this.from, this.to) && isSameDay(this.from, new Date())) {
        return translate.instant('Edge.History.today') + ", " + format(new Date(), translate.instant('General.dateFormat'));
      }
      else if (isSameDay(this.from, this.to) && !isSameDay(this.from, subDays(new Date(), 1))) {
        switch (getDay(this.from)) {
          case 0: {
            return translate.instant('General.Week.sunday') + ", " + translate.instant('Edge.History.selectedDay', {
              value: format(this.from, translate.instant('General.dateFormat'))
            })
          }
          case 1: {
            return translate.instant('General.Week.monday') + ", " + translate.instant('Edge.History.selectedDay', {
              value: format(this.from, translate.instant('General.dateFormat'))
            })
          }
          case 2: {
            return translate.instant('General.Week.tuesday') + ", " + translate.instant('Edge.History.selectedDay', {
              value: format(this.from, translate.instant('General.dateFormat'))
            })
          }
          case 3: {
            return translate.instant('General.Week.wednesday') + ", " + translate.instant('Edge.History.selectedDay', {
              value: format(this.from, translate.instant('General.dateFormat'))
            })
          }
          case 4: {
            return translate.instant('General.Week.thursday') + ", " + translate.instant('Edge.History.selectedDay', {
              value: format(this.from, translate.instant('General.dateFormat'))
            })
          }
          case 5: {
            return translate.instant('General.Week.friday') + ", " + translate.instant('Edge.History.selectedDay', {
              value: format(this.from, translate.instant('General.dateFormat'))
            })
          }
          case 6: {
            return translate.instant('General.Week.saturday') + ", " + translate.instant('Edge.History.selectedDay', {
              value: format(this.from, translate.instant('General.dateFormat'))
            })
          }
        }
      }
      else if (isSameDay(this.from, this.to) && isSameDay(this.from, subDays(new Date(), 1))) {
        return translate.instant('Edge.History.yesterday') + ", " + format(this.from, translate.instant('General.dateFormat'));
      } else {
        return translate.instant(
          'General.periodFromTo', {
          value1: format(this.from, translate.instant('General.dateFormatShort')),
          value2: format(this.to, translate.instant('General.dateFormat'))
        })
      }
    }
  }
}
