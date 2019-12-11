import { Component, Input } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { addDays, getDate, getMonth, getYear, subDays, startOfWeek, startOfMonth, startOfYear, endOfWeek, endOfMonth, endOfYear } from 'date-fns/esm';
import { Service } from '../../shared';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../service/defaulttypes';
import { IMyDate, IMyDateRangeModel, IMyDrpOptions, IMyDayLabels, IMyMonthLabels } from 'mydaterangepicker';
import { isFuture } from 'date-fns';


@Component({
    selector: 'pickdatepopover',
    templateUrl: './popover.component.html'
})
export class PickDatePopoverComponent {

    @Input() public disableArrow: boolean;

    public readonly TODAY = new Date();
    public readonly YESTERDAY = subDays(new Date(), 1);
    public readonly TOMORROW = addDays(new Date(), 1);


    public activePeriod: DefaultTypes.PeriodString = this.service.periodString;
    public showCustomDate: boolean = false;

    public transDayLables: IMyDayLabels = {
        su: this.translate.instant('Edge.History.Sun'),
        mo: this.translate.instant('Edge.History.Mon'),
        tu: this.translate.instant('Edge.History.Tue'),
        we: this.translate.instant('Edge.History.Wed'),
        th: this.translate.instant('Edge.History.Thu'),
        fr: this.translate.instant('Edge.History.Fri'),
        sa: this.translate.instant('Edge.History.Sat')
    };

    public transMonthLabels: IMyMonthLabels = {
        1: this.translate.instant('Edge.History.Jan'),
        2: this.translate.instant('Edge.History.Feb'),
        3: this.translate.instant('Edge.History.Mar'),
        4: this.translate.instant('Edge.History.Apr'),
        5: this.translate.instant('Edge.History.May'),
        6: this.translate.instant('Edge.History.Jun'),
        7: this.translate.instant('Edge.History.Jul'),
        8: this.translate.instant('Edge.History.Aug'),
        9: this.translate.instant('Edge.History.Sep'),
        10: this.translate.instant('Edge.History.Oct'),
        11: this.translate.instant('Edge.History.Nov'),
        12: this.translate.instant('Edge.History.Dec')
    };

    //DateRangePicker Options
    public dateRangePickerOptions: IMyDrpOptions = {
        selectorHeight: '225px',
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
        selectBeginDateTxt: this.translate.instant('Edge.History.BeginDate'),
        selectEndDateTxt: this.translate.instant('Edge.History.EndDate'),
        dayLabels: this.transDayLables,
        monthLabels: this.transMonthLabels
    };

    constructor(
        public service: Service,
        public popoverCtrl: PopoverController,
        public translate: TranslateService,
    ) { }


    /**
     * Sets the current time period.
     * 
     * @param fromDate the starting date
     * @param toDate   the end date
     */
    public setDateRange(period: DefaultTypes.HistoryPeriod) {
        this.service.historyPeriod = period;
        // this.updateActivePeriod();
    }

    /**
     * This is called by the input button on the UI.
     * 
     * @param period
     * @param from
     * @param to
     */
    public setPeriod(period: DefaultTypes.PeriodString) {
        switch (period) {
            case 'day': {
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.TODAY, this.TODAY));
                this.service.periodString = period;
                this.disableArrow = true;
                this.popoverCtrl.dismiss(this.disableArrow);
                break;
            }
            case 'week': {
                this.setDateRange(new DefaultTypes.HistoryPeriod(startOfWeek(this.TODAY, { weekStartsOn: 1 }), endOfWeek(this.TODAY, { weekStartsOn: 1 })));
                this.service.periodString = period;
                this.disableArrow = true;
                this.popoverCtrl.dismiss(this.disableArrow);
                break;
            }
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

    public onDateRangeChanged(event: IMyDateRangeModel) {
        this.service.historyPeriod = new DefaultTypes.HistoryPeriod(event.beginJsDate, event.endJsDate);
        this.service.periodString = 'custom';
        let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
        dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
        if (isFuture(addDays(this.service.historyPeriod.to, dateDistance * 2))) {
            this.disableArrow = true;
        } else {
            this.disableArrow = false;
        }
        this.popoverCtrl.dismiss(this.disableArrow);
    }
}
