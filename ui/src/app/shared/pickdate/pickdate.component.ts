import { Component, EventEmitter, Output } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { addDays, format, getDate, getMonth, getYear, isSameDay, subDays } from 'date-fns/esm';
import { IMyDate, IMyDateRange, IMyDateRangeModel } from 'mydaterangepicker';
import { Service } from '../shared';
import { PickDatePopoverComponent } from './popover/popover.component';
import { DefaultTypes } from '../service/defaulttypes';

type PeriodString = 'today' | 'yesterday' | 'otherPeriod';

@Component({
    selector: 'pickdate',
    templateUrl: './pickdate.component.html'
})
export class PickDateComponent {

    public readonly TODAY = new Date();
    public readonly YESTERDAY = subDays(new Date(), 1);
    public readonly TOMORROW = addDays(new Date(), 1);

    public activePeriod: PeriodString = 'today';
    public dateRange: IMyDateRange;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    ngOnInit() {
        this.updateActivePeriod();
    }

    ngOnDestroy() { }

    private updateActivePeriod(): PeriodString {
        let period = this.service.historyPeriod;
        if (isSameDay(period.from, this.TODAY) && isSameDay(period.to, this.TODAY)) {
            this.activePeriod = 'today';
        } else if (isSameDay(period.from, this.YESTERDAY) && isSameDay(period.to, this.YESTERDAY)) {
            this.activePeriod = 'yesterday';
        } else {
            this.activePeriod = 'otherPeriod';
        }
        return this.activePeriod;
    }

    /**
     * This is called by the input button on the UI.
     * 
     * @param period
     * @param from
     * @param to
     */
    public setPeriod(period: PeriodString) {
        switch (period) {
            case "yesterday": {
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.YESTERDAY, this.YESTERDAY));
                break;
            }
            case "today":
            default:
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.TODAY, this.TODAY));
                break;
        }
    }

    /**
     * Sets the current time period.
     * 
     * @param fromDate the starting date
     * @param toDate   the end date
     */
    public setDateRange(period: DefaultTypes.HistoryPeriod) {
        this.service.historyPeriod = period;
        this.updateActivePeriod();
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
        const popover = await this.popoverCtrl.create({
            component: PickDatePopoverComponent,
            event: ev,
            translucent: false,
            cssClass: 'pickdate-popover'
        });
        await popover.present();
        const { data } = await popover.onDidDismiss();
        this.setDateRange(data);
        return;
    }
}
