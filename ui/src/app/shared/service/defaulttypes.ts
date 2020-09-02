import { TranslateService } from '@ngx-translate/core';
import { format, getDay, isSameDay, subDays } from 'date-fns';

export module DefaultTypes {

  export type Backend = "OpenEMS Backend" | "OpenEMS Edge";

  export type ConnectionStatus = "online" | "connecting" | "waiting for authentication" | "failed";

  export type UpdateComponentObject = {
    name: string,
    value: string
  };

  export interface FormlyModel {
    property_id?: string,
    value?: string
  }

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
      totalPower: number | null,
      // autarchy in percent
      autarchy: number | null,
      // self consumption in percent
      selfConsumption: number | null,
      // state 0: Ok, 1: Info, 2: Warning, 3: Fault
      state: number | null
    }, storage: {
      soc: number | null,
      activePowerL1: number | null,
      activePowerL2: number | null,
      activePowerL3: number | null,
      effectiveActivePowerL1: number | null,
      effectiveActivePowerL2: number | null,
      effectiveActivePowerL3: number | null,
      chargeActivePower: number | null,
      chargeActivePowerAc: number | null,
      chargeActivePowerDc: number | null,
      maxChargeActivePower?: number | null,
      dischargeActivePower: number | null,
      dischargeActivePowerAc: number | null,
      dischargeActivePowerDc: number | null,
      maxDischargeActivePower?: number | null,
      powerRatio: number | null,
      maxApparentPower: number | null,
      effectivePower: number | null,
      effectiveChargePower: number | null,
      effectiveDischargePower: number | null,
      capacity: number | null,
    }, production: {
      powerRatio: number | null,
      hasDC: boolean | null,
      activePower: number | null, // sum of activePowerAC and activePowerDC
      activePowerAc: number | null,
      activePowerAcL1: number | null,
      activePowerAcL2: number | null,
      activePowerAcL3: number | null,
      activePowerDc: number | null,
      maxActivePower: number | null
    }, grid: {
      powerRatio: number | null,
      activePowerL1: number | null,
      activePowerL2: number | null,
      activePowerL3: number | null,
      buyActivePower: number | null,
      maxBuyActivePower: number | null,
      sellActivePower: number | null,
      sellActivePowerL1: number | null,
      sellActivePowerL2: number | null,
      sellActivePowerL3: number | null,
      maxSellActivePower: number | null,
      gridMode: number | null
    }, consumption: {
      powerRatio: number | null,
      activePower: number | null,
      activePowerL1: number | null,
      activePowerL2: number | null,
      activePowerL3: number | null
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
