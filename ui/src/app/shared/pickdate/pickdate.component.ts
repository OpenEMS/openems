import { addDays, addWeeks, endOfWeek, isFuture, subDays, subWeeks } from 'date-fns/esm';
import { Component } from '@angular/core';
import { DefaultTypes, } from '../service/defaulttypes';
import { endOfDay, differenceInMilliseconds } from 'date-fns';
import { PickDatePopoverComponent } from './popover/popover.component';
import { PopoverController } from '@ionic/angular';
import { Service } from '../shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'pickdate',
    templateUrl: './pickdate.component.html'
})
export class PickDateComponent {

    public disableArrow: boolean = null;

    private changePeriodTimeout = null;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    ngOnInit() {
        this.checkArrowAutomaticForwarding();
    }

    /**
     * checks if arrow has to be disabled/enabled and if automatic forwarding is needed dependend on the date
     */
    public checkArrowAutomaticForwarding() {
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
            case 'month': {

            }
            case 'year': {

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

    public goForward() {
        switch (this.service.periodString) {
            case 'day': {
                if (isFuture(addDays(this.service.historyPeriod.from, 2))) {
                    //waits until next day is reached to set next days period
                    this.forwardToNextDayWhenReached();
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addDays(this.service.historyPeriod.from, 1), addDays(endOfDay(this.service.historyPeriod.to), 1)));
                    this.disableArrow = true;
                } else {
                    //disables changing period to next day when next day is reached if current day is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addDays(this.service.historyPeriod.from, 1), addDays(endOfDay(this.service.historyPeriod.to), 1)));
                }
                break;
            }
            case 'week': {
                if (isFuture(addWeeks(this.service.historyPeriod.from, 2))) {
                    //waits until next week is reached to set next weeks period
                    this.forwardToNextWeekWhenReached();
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addWeeks(this.service.historyPeriod.from, 1), addWeeks(endOfWeek(this.service.historyPeriod.to, { weekStartsOn: 1 }), 1)));
                    this.disableArrow = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addWeeks(this.service.historyPeriod.from, 1), addWeeks(endOfWeek(this.service.historyPeriod.to, { weekStartsOn: 1 }), 1)));
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
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addDays(this.service.historyPeriod.from, dateDistance), addDays(this.service.historyPeriod.to, dateDistance)));
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
                this.setDateRange(new DefaultTypes.HistoryPeriod(subDays(this.service.historyPeriod.from, 1), subDays((endOfDay(this.service.historyPeriod.to)), 1)));
                break;
            }
            case 'week': {
                //disables changing period to next week when next week is reached if current week is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.disableArrow = false;
                this.setDateRange(new DefaultTypes.HistoryPeriod(subWeeks(this.service.historyPeriod.from, 1), subWeeks(endOfWeek(this.service.historyPeriod.to, { weekStartsOn: 1 }), 1)));
                break;
            }
            case 'custom': {
                this.disableArrow = false;
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.from - <any>this.service.historyPeriod.to) / (1000 * 60 * 60 * 24));
                dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
                this.setDateRange(new DefaultTypes.HistoryPeriod(subDays(this.service.historyPeriod.from, dateDistance), subDays(this.service.historyPeriod.to, dateDistance)));
                break;
            }
        }
    }

    /**
     * changes history period date and text when next day is reached
     */
    private forwardToNextDayWhenReached() {
        this.changePeriodTimeout = setTimeout(() => {
            this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), new Date()));
            this.service.historyPeriod.getText(this.translate);
        }, this.millisecondsUntilnextPeriod());
    }

    /**
     * changes history period date and text when next week is reached
     */
    private forwardToNextWeekWhenReached() {
        this.changePeriodTimeout = setTimeout(() => {
            // this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfWeek(new Date(), { weekStartsOn: 1 })));
            this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfWeek(new Date(), { weekStartsOn: 1 })));
            this.service.historyPeriod.getText(this.translate);
        }, this.millisecondsUntilnextPeriod());
    }

    /**
     * calculates the milliseconds until next period (Day|Week) will occour
     * is used to change date period
     */
    private millisecondsUntilnextPeriod(): number {
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

    async presentPopover(ev: any) {
        const popover = await this.popoverCtrl.create({
            component: PickDatePopoverComponent,
            event: ev,
            translucent: false,
            cssClass: 'pickdate-popover',
            componentProps: {
                setDateRange: this.setDateRange,
            }
        });
        await popover.present();
        popover.onDidDismiss().then(() => {
            this.checkArrowAutomaticForwarding();
        });
    }
}