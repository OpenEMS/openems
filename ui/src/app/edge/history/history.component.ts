import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { addDays, format, getDate, getMonth, getYear, isSameDay, subDays } from 'date-fns';
import { IMyDate, IMyDateRange, IMyDateRangeModel, IMyDrpOptions } from 'mydaterangepicker';
import { Edge, Service } from '../../shared/shared';
import { environment } from '../../../environments';

type PeriodString = "today" | "yesterday" | "lastWeek" | "lastMonth" | "lastYear" | "otherPeriod";

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit {

  private readonly TODAY = new Date();
  private readonly YESTERDAY = subDays(new Date(), 1);
  private readonly TOMORROW = addDays(new Date(), 1);

  public activePeriodText: string = "";
  // sets the height for a chart. This is recalculated on every window resize.
  public socChartHeight: string = "250px";
  public energyChartHeight: string = "250px";
  public activePeriod: PeriodString = "today";
  public fromDate = this.TODAY;
  public toDate = this.TODAY;
  public env = environment;

  protected edge: Edge = null;
  protected dateRange: IMyDateRange;
  protected dateRangePickerOptions: IMyDrpOptions = {
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

  constructor(
    private route: ActivatedRoute,
    private translate: TranslateService,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
  }

  updateOnWindowResize() {
    let ref = /* fix proportions */ Math.min(window.innerHeight - 150,
      /* handle grid breakpoints */(window.innerWidth < 768 ? window.innerWidth - 150 : window.innerWidth - 400));
    this.socChartHeight =
      /* minimum size */ Math.max(150,
      /* maximium size */ Math.min(200, ref)
    ) + "px";
    this.energyChartHeight =
      /* minimum size */ Math.max(300,
      /* maximium size */ Math.min(600, ref)
    ) + "px";
  }

  clickOtherPeriod() {
    if (this.activePeriod === 'otherPeriod') {
      this.setPeriod("today");
    } else {
      this.setPeriod("otherPeriod", this.YESTERDAY, this.TODAY);
    }
  }

  onDateRangeChanged(event: IMyDateRangeModel) {
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
  setPeriod(period: PeriodString, fromDate?: Date, toDate?: Date) {
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
  private setDateRange(fromDate: Date, toDate: Date) {
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
  private toIMyDate(date: Date): IMyDate {
    return { year: getYear(date), month: getMonth(date) + 1, day: getDate(date) }
  }
}