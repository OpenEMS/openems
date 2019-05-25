import { Component } from '@angular/core';
import { Service } from '../shared';
import { PopoverController } from '@ionic/angular';
import { IMyDateRangeModel, IMyDate } from 'mydaterangepicker';
import { isSameDay, subDays, format, getYear, getMonth, getDate } from 'date-fns/esm';
import { TranslateService } from '@ngx-translate/core';
import { PickDatePopoverComponent } from './pickdate-modal/pickdate-popover.component';


type PeriodString = "today" | "yesterday" | "lastWeek" | "lastMonth" | "lastYear" | "otherPeriod";

@Component({
    selector: 'pickdate',
    templateUrl: './pickdate.component.html'
})


export class PickDateComponent {

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverController: PopoverController,
    ) { }


    ngOnInit() {
        this.service.setPeriod('today')
    }

    ngOnDestroy() { }


    public clickOtherPeriod() {
        if (this.service.activePeriod === 'otherPeriod') {
            this.setPeriod("today");
        } else {
            this.setPeriod("otherPeriod", this.service.YESTERDAY, this.service.TODAY);
        }
    }

    public onDateRangeChanged(event: IMyDateRangeModel) {
        console.log("ONDATERANGECHANGED")
        let fromDate = event.beginJsDate;
        let toDate = event.endJsDate;
        if (isSameDay(fromDate, toDate)) {
            // only one day selected
            if (isSameDay(this.service.TODAY, fromDate)) {
                this.setPeriod("today");
                return;
            } else if (isSameDay(this.service.YESTERDAY, fromDate)) {
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
        this.service.activePeriod = period;
        switch (period) {
            case "yesterday": {
                let yesterday = subDays(new Date(), 1);
                this.setDateRange(yesterday, yesterday);
                this.service.activePeriodText = this.translate.instant('Edge.History.Yesterday') + ", " + format(yesterday, this.translate.instant('General.DateFormat'));
                break;
            }
            case "otherPeriod":
                if (fromDate > toDate) {
                    toDate = fromDate;
                }
                this.setDateRange(fromDate, toDate);
                this.service.activePeriodText = this.translate.instant('General.PeriodFromTo', {
                    value1: format(fromDate, this.translate.instant('General.DateFormat')),
                    value2: format(toDate, this.translate.instant('General.DateFormat'))
                });
                break;
            case "today":
            default:
                let today = new Date();
                this.setDateRange(today, today);
                this.service.activePeriodText = this.translate.instant('Edge.History.Today') + ", " + format(today, this.translate.instant('General.DateFormat'));
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
        this.service.fromDate = fromDate;
        this.service.toDate = toDate;
        this.service.dateRange = {
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

    async presentPopover(ev: any) {
        const popover = await this.popoverController.create({
            component: PickDatePopoverComponent,
            event: ev,
            translucent: true,
            cssClass: 'position: relative;'
        });
        return await popover.present();
    }

    check() {
        console.log("passed")
    }
}
