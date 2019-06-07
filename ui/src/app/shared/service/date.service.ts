import { Injectable } from '@angular/core';
import { addDays, format, getDate, getMonth, getYear, isSameDay, subDays } from 'date-fns';
import { IMyDate, IMyDateRange, IMyDateRangeModel, IMyDrpOptions } from 'mydaterangepicker';
import { TranslateService } from '@ngx-translate/core';

type PeriodString = "today" | "yesterday" | "lastWeek" | "lastMonth" | "lastYear" | "otherPeriod";

@Injectable({
  providedIn: 'root'
})
export class DateService {

  constructor(public translate: TranslateService) { }

  //DateRangePicker Variables
  public readonly TODAY = new Date();
  public readonly YESTERDAY = subDays(new Date(), 1);
  public readonly TOMORROW = addDays(new Date(), 1);

  public activePeriod: PeriodString = "today";
  public fromDate = this.TODAY;
  public toDate = this.TODAY;
  public dateRange: IMyDateRange;
  public activePeriodText: string = "";

  //DateRangePicker Options
  public dateRangePickerOptions: IMyDrpOptions = {
    inline: true,
    showClearBtn: false,
    showApplyBtn: false,
    dateFormat: 'dd.mm.yyyy',
    disableUntil: { day: 1, month: 1, year: 2013 }, // TODO start with date since the edge is available
    disableSince: this.toIMyDate(this.TOMORROW),
    showWeekNumbers: true,
    showClearDateRangeBtn: false,
    editableDateRangeField: false,
    openSelectorOnInputClick: true,
  };

  public clickOtherPeriod() {
    if (this.activePeriod === 'otherPeriod') {
      this.setPeriod("today");
    } else {
      this.setPeriod("otherPeriod", this.YESTERDAY, this.TODAY);
    }
  }

  public onDateRangeChanged(event: IMyDateRangeModel) {
    let fromDate = event.beginJsDate;
    let toDate = event.endJsDate;
    if (isSameDay(fromDate, toDate)) {
      // only one day selected
      if (isSameDay(this.TODAY, fromDate)) {
        this.setPeriod("today");
        return;
      } else if (isSameDay(this.YESTERDAY, fromDate)) {
        this.setPeriod("yesterday");
        return;
      }
    }
    this.setPeriod("otherPeriod", fromDate, toDate);
  }

  /**
   * This is called by the input button on the UI.
   * 
   * @param period
   * @param from
   * @param to
   */
  public setPeriod(period: PeriodString, fromDate?: Date, toDate?: Date) {
    this.activePeriod = period;
    switch (period) {
      case "yesterday": {
        let yesterday = subDays(new Date(), 1);
        this.setDateRange(yesterday, yesterday);
        this.activePeriodText = this.translate.instant('Edge.History.Yesterday') + ", " + format(yesterday, this.translate.instant('General.DateFormat'));
        break;
      }
      case "otherPeriod":
        if (fromDate > toDate) {
          toDate = fromDate;
        }
        this.setDateRange(fromDate, toDate);
        this.activePeriodText = this.translate.instant('General.PeriodFromTo', {
          value1: format(fromDate, this.translate.instant('General.DateFormat')),
          value2: format(toDate, this.translate.instant('General.DateFormat'))
        });
        break;
      case "today":
      default:
        let today = new Date();
        this.setDateRange(today, today);
        this.activePeriodText = this.translate.instant('Edge.History.Today') + ", " + format(today, this.translate.instant('General.DateFormat'));
        break;
    }
  }

  /**
   * Sets the current time period.
   * 
   * @param fromDate the starting date
   * @param toDate   the end date
   */
  public setDateRange(fromDate: Date, toDate: Date) {
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.dateRange = {
      beginDate: this.toIMyDate(fromDate),
      endDate: this.toIMyDate(toDate)
    }
  }

  /**
   * Converts a 'Date' to 'IMyDate' format.
   * 
   * @param date the 'Date'
   * @returns the 'IMyDate'
   */
  public toIMyDate(date: Date): IMyDate {
    return { year: getYear(date), month: getMonth(date) + 1, day: getDate(date) }
  }
}
