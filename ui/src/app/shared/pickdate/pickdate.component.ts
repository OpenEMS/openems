import { Component } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { endOfDay, getTime, differenceInMinutes, differenceInSeconds, differenceInMilliseconds, startOfWeek } from 'date-fns';
import { addDays, addWeeks, endOfWeek, isFuture, subDays, subWeeks } from 'date-fns/esm';
import { isUndefined } from 'util';
import { DefaultTypes, } from '../service/defaulttypes';
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

    private changePeriodTimeout = null;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    ngOnInit() {
        switch (this.service.periodString) {
            case 'day': {
                if (isFuture(addDays(this.service.historyPeriod.from, 1))) {
                    //waits until next day is reached to set next days period
                    this.forwardToNextDayWhenReached()
                    this.disableArrow = true;
                } else {
                    //disables changing period to next day when next day is reached if current day is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.disableArrow = false;
                }
                break;
            }
            case 'week': {
                if (isFuture(addWeeks(this.service.historyPeriod.from, 1))) {
                    //waits until next week is reached to set next weeks period
                    this.forwardToNextWeekWhenReached()
                    this.disableArrow = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.disableArrow = false;
                }
                break;
            }
            case 'custom': {
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
                dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
                if (isFuture(addDays(this.service.historyPeriod.from, dateDistance * 2))) {
                    this.disableArrow = true;
                } else {
                    this.disableArrow = false;
                }
                break;
            }
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
                    //waits until next day is reached to set next days period
                    console.log("yo")
                    this.forwardToNextDayWhenReached()
                    this.disableArrow = true;
                }
                if (!isFuture(addDays(this.service.historyPeriod.from, 1))) {
                    //disables changing period to next day when next day is reached if current day is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.service.historyPeriod.from = addDays(this.service.historyPeriod.from, 1);
                    this.service.historyPeriod.to = endOfDay(this.service.historyPeriod.from);
                    this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                }
                break;
            }
            case 'week': {
                if (isFuture(addWeeks(this.service.historyPeriod.from, 2))) {
                    //waits until next week is reached to set next weeks period
                    this.forwardToNextWeekWhenReached()
                    this.disableArrow = true;
                }
                if (!isFuture(addWeeks(this.service.historyPeriod.from, 1))) {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.service.historyPeriod.from = addWeeks(this.service.historyPeriod.from, 1);
                    this.service.historyPeriod.to = endOfWeek(this.service.historyPeriod.from, { weekStartsOn: 1 });
                    this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                }
                break;
            }
            case 'custom': {
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
                dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
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
                //disables changing period to next day when next day is reached if current day is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.disableArrow = false;
                this.service.historyPeriod.from = subDays(this.service.historyPeriod.from, 1);
                this.service.historyPeriod.to = endOfDay(this.service.historyPeriod.from);
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                break;
            }
            case 'week': {
                //disables changing period to next week when next week is reached if current week is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.disableArrow = false;
                this.service.historyPeriod.from = subWeeks(this.service.historyPeriod.from, 1);
                this.service.historyPeriod.to = endOfWeek(this.service.historyPeriod.from, { weekStartsOn: 1 });
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                break;
            }
            case 'custom': {
                this.disableArrow = false;
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
                dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
                this.service.historyPeriod.from = subDays(this.service.historyPeriod.from, dateDistance);
                this.service.historyPeriod.to = subDays(this.service.historyPeriod.to, dateDistance);
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.service.historyPeriod.from, this.service.historyPeriod.to));
                break;
            }
        }
    }

    /**
     * changes history period date and text when next day is reached
     */
    forwardToNextDayWhenReached() {
        this.changePeriodTimeout = setTimeout(() => {
            this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), new Date()));
            this.service.historyPeriod.getText(this.translate);
        }, this.millisecondsUntilnextPeriod());
    }

    /**
     * changes history period date and text when next week is reached
     */
    forwardToNextWeekWhenReached() {
        this.changePeriodTimeout = setTimeout(() => {
            console.log("period1", this.service.historyPeriod)
            // this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfWeek(new Date(), { weekStartsOn: 1 })));
            this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfWeek(new Date(), { weekStartsOn: 1 })));
            console.log("period2", this.service.historyPeriod)
            this.service.historyPeriod.getText(this.translate);
        }, this.millisecondsUntilnextPeriod());
    }

    /**
     * calculates the milliseconds until next period (Day|Week) will occour
     * is used to change date period
     */
    millisecondsUntilnextPeriod(): number {
        // + 1000 to reach the next day
        switch (this.service.periodString) {
            case 'day': {
                let currentDayTime = new Date();
                let endOfDayTime = endOfDay(currentDayTime);
                return differenceInMilliseconds(endOfDayTime, currentDayTime) + 1000;
            }
            case 'week': {
                let currentDayTime = new Date();
                let endOfWeekTime = endOfWeek(currentDayTime, { weekStartsOn: 1 })
                return differenceInMilliseconds(endOfWeekTime, currentDayTime) + 1000;
            }
        }
    }
}
