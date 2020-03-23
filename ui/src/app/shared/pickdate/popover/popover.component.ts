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
        su: this.translate.instant('Edge.History.sun'),
        mo: this.translate.instant('Edge.History.mon'),
        tu: this.translate.instant('Edge.History.tue'),
        we: this.translate.instant('Edge.History.wed'),
        th: this.translate.instant('Edge.History.thu'),
        fr: this.translate.instant('Edge.History.fri'),
        sa: this.translate.instant('Edge.History.sat')
    };

    public transMonthLabels: IMyMonthLabels = {
        1: this.translate.instant('Edge.History.jan'),
        2: this.translate.instant('Edge.History.feb'),
        3: this.translate.instant('Edge.History.mar'),
        4: this.translate.instant('Edge.History.apr'),
        5: this.translate.instant('Edge.History.may'),
        6: this.translate.instant('Edge.History.jun'),
        7: this.translate.instant('Edge.History.jul'),
        8: this.translate.instant('Edge.History.aug'),
        9: this.translate.instant('Edge.History.sep'),
        10: this.translate.instant('Edge.History.oct'),
        11: this.translate.instant('Edge.History.nov'),
        12: this.translate.instant('Edge.History.dec')
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
        selectBeginDateTxt: this.translate.instant('Edge.History.beginDate'),
        selectEndDateTxt: this.translate.instant('Edge.History.endDate'),
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
