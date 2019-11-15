import { Component } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { endOfDay } from 'date-fns';
import { addDays, addWeeks, endOfWeek, getDate, getMonth, getYear, isFuture, subDays, subWeeks } from 'date-fns/esm';
import { IMyDate } from 'mydaterangepicker';
import { isUndefined } from 'util';
import { DefaultTypes } from '../service/defaulttypes';
import { Service } from '../shared';
import { PickDatePopoverComponent } from './popover/popover.component';


@Component({
    selector: 'pickdate',
    templateUrl: './pickdate.component.html'
})
export class PickDateComponent {

    public readonly TODAY = new Date();
    public readonly YESTERDAY = subDays(new Date(), 1);
    public readonly TOMORROW = addDays(new Date(), 1);

    public disableArrow: boolean = null;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    ngOnInit() {
        switch (this.service.periodString) {
            case 'day': {
                if (isFuture(addDays(this.service.historyPeriod.from, 1))) {
                    this.disableArrow = true;
                } else {
                    this.disableArrow = false;
                }
                break;
            }
            case 'week': {
                if (isFuture(addWeeks(this.service.historyPeriod.from, 1))) {
                    this.disableArrow = true;
                } else {
                    this.disableArrow = false;
                }
                break;
            }
            case 'custom': {
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
                if (isFuture(addDays(this.service.historyPeriod.from, dateDistance * 2))) {
                    this.disableArrow = true;
                } else {
                    this.disableArrow = false;
                }
                break;
            }
        }
    }

    ngOnDestroy() { }


    /**
     * Sets the current time period.
     * 
     * @param fromDate the starting date
     * @param toDate   the end date
     */
    public setDateRange(period: DefaultTypes.HistoryPeriod) {
        this.service.historyPeriod = period;
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
            cssClass: 'pickdate-popover',
            componentProps: {
                disableArrow: this.disableArrow,
            }
        });
        await popover.present();
        popover.onDidDismiss().then((data) => {
            if (!isUndefined(data['data'])) {
                this.disableArrow = data['data'];
            }
        });
    }

    public goForward() {
        switch (this.service.periodString) {
            case 'day': {
                if (isFuture(addDays(this.service.historyPeriod.from, 2))) {
                    this.disableArrow = true;
                }
                if (!isFuture(addDays(this.service.historyPeriod.from, 1))) {
                    this.service.historyPeriod.from = addDays(this.service.historyPeriod.from, 1);
                    this.service.historyPeriod.to = endOfDay(this.service.historyPeriod.from);
                    this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                }
                break;
            }
            case 'week': {
                if (isFuture(addWeeks(this.service.historyPeriod.from, 2))) {
                    this.disableArrow = true;
                }
                if (!isFuture(addWeeks(this.service.historyPeriod.from, 1))) {
                    this.service.historyPeriod.from = addWeeks(this.service.historyPeriod.from, 1);
                    this.service.historyPeriod.to = endOfWeek(this.service.historyPeriod.from, { weekStartsOn: 1 });
                    this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                }
                break;
            }
            case 'custom': {
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
                if (isFuture(addDays(this.service.historyPeriod.to, dateDistance * 2))) {
                    this.disableArrow = true;
                }
                if (!isFuture(addDays(this.service.historyPeriod.to, dateDistance))) {
                    this.service.historyPeriod.from = addDays(this.service.historyPeriod.from, dateDistance);
                    this.service.historyPeriod.to = addDays(this.service.historyPeriod.to, dateDistance);
                    this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                }
                break;
            }
        }
    }

    public goBackward() {
        switch (this.service.periodString) {
            case 'day': {
                this.disableArrow = false;
                this.service.historyPeriod.from = subDays(this.service.historyPeriod.from, 1);
                this.service.historyPeriod.to = endOfDay(this.service.historyPeriod.from);
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                break;
            }
            case 'week': {
                this.disableArrow = false;
                this.service.historyPeriod.from = subWeeks(this.service.historyPeriod.from, 1);
                this.service.historyPeriod.to = endOfWeek(this.service.historyPeriod.from, { weekStartsOn: 1 });
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                break;
            }
            case 'custom': {
                this.disableArrow = false;
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
                this.service.historyPeriod.from = subDays(this.service.historyPeriod.from, dateDistance);
                this.service.historyPeriod.to = subDays(this.service.historyPeriod.to, dateDistance);
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                break;
            }
        }
    }
}