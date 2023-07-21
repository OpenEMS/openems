import { Component, OnDestroy, OnInit } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { addMonths, addYears, differenceInMilliseconds, endOfDay, endOfMonth, endOfYear, subMonths, subYears } from 'date-fns';
import { addDays, addWeeks, endOfWeek, isFuture, subDays, subWeeks } from 'date-fns/esm';
import { DefaultTypes } from '../service/defaulttypes';
import { Edge, Service } from '../shared';
import { PickDatePopoverComponent } from './popover/popover.component';

@Component({
    selector: 'pickdate',
    templateUrl: './pickdate.component.html'
})
export class PickDateComponent implements OnInit, OnDestroy {

    public disableArrow: boolean | null = null;

    private changePeriodTimeout = null;
    private edge: Edge | null = null;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController
    ) { }

    public ngOnInit() {
        this.checkArrowAutomaticForwarding();
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
        });
    }

    public ngOnDestroy() {
        if (this.changePeriodTimeout != null) {
            clearTimeout(this.changePeriodTimeout);
        }
    }

    /**
     * checks if arrow has to be disabled/enabled and if automatic forwarding is needed dependend on the date
     */
    public checkArrowAutomaticForwarding() {
        switch (this.service.periodString) {
            case DefaultTypes.PeriodString.DAY: {
                if (isFuture(addDays(this.service.historyPeriod.value.from, 1))) {
                    //waits until next day is reached to set next days period
                    this.forwardToNextDayWhenReached();
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
            case DefaultTypes.PeriodString.WEEK: {
                if (isFuture(addWeeks(this.service.historyPeriod.value.from, 1))) {
                    //waits until next week is reached to set next weeks period
                    this.forwardToNextWeekWhenReached();
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
            case DefaultTypes.PeriodString.MONTH: {
                if (isFuture(addMonths(this.service.historyPeriod.value.from, 1))) {
                    //waits until next month is reached to set next months period
                    this.forwardToNextMonthWhenReached();
                    this.disableArrow = true;
                } else {
                    //disables changing period to next month when next month is reached if current month is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.disableArrow = false;
                }
                break;
            }
            case DefaultTypes.PeriodString.YEAR: {
                if (isFuture(addYears(this.service.historyPeriod.value.from, 1))) {
                    //waits until next week is reached to set next weeks period
                    // this.forwardToNextYearWhenReached()
                    this.disableArrow = true;
                } else {
                    //disables changing period to next year when next year is reached if current year is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.disableArrow = false;
                }
            }
            case DefaultTypes.PeriodString.CUSTOM: {
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.value.from - <any>this.service.historyPeriod.value.to) / (1000 * 60 * 60 * 24));
                dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
                if (isFuture(addDays(this.service.historyPeriod.value.from, dateDistance * 2))) {
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
        this.service.historyPeriod.next(period);
    }

    public goForward() {
        switch (this.service.periodString) {
            case DefaultTypes.PeriodString.DAY: {
                if (isFuture(addDays(this.service.historyPeriod.value.from, 2))) {
                    //waits until next day is reached to set next days period
                    this.forwardToNextDayWhenReached();
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addDays(this.service.historyPeriod.value.from, 1), addDays(endOfDay(this.service.historyPeriod.value.to), 1)));
                    this.disableArrow = true;
                } else {
                    //disables changing period to next day when next day is reached if current day is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addDays(this.service.historyPeriod.value.from, 1), addDays(endOfDay(this.service.historyPeriod.value.to), 1)));
                }
                break;
            }
            case DefaultTypes.PeriodString.WEEK: {
                if (isFuture(addWeeks(this.service.historyPeriod.value.from, 2))) {
                    //waits until next week is reached to set next weeks period
                    this.forwardToNextWeekWhenReached();
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addWeeks(this.service.historyPeriod.value.from, 1), addWeeks(endOfWeek(this.service.historyPeriod.value.to, { weekStartsOn: 1 }), 1)));
                    this.disableArrow = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addWeeks(this.service.historyPeriod.value.from, 1), addWeeks(endOfWeek(this.service.historyPeriod.value.to, { weekStartsOn: 1 }), 1)));
                }
                break;
            }
            case DefaultTypes.PeriodString.MONTH: {
                if (isFuture(addMonths(this.service.historyPeriod.value.from, 2))) {
                    //waits until next month is reached to set next months period
                    this.forwardToNextMonthWhenReached();
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addMonths(this.service.historyPeriod.value.from, 1), endOfMonth(addMonths(this.service.historyPeriod.value.to, 1))));
                    this.disableArrow = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addMonths(this.service.historyPeriod.value.from, 1), endOfMonth(addMonths(this.service.historyPeriod.value.to, 1))));
                }
                break;
            }
            case DefaultTypes.PeriodString.YEAR: {
                if (isFuture(addYears(this.service.historyPeriod.value.from, 2))) {
                    //waits until next week is reached to set next weeks period
                    this.forwardToNextYearWhenReached();
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addYears(this.service.historyPeriod.value.from, 1), endOfYear(addYears(this.service.historyPeriod.value.to, 1))));
                    this.disableArrow = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (this.changePeriodTimeout != null) {
                        clearTimeout(this.changePeriodTimeout);
                    }
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addYears(this.service.historyPeriod.value.from, 1), endOfYear(addYears(this.service.historyPeriod.value.to, 1))));
                }
                break;
            }
            case DefaultTypes.PeriodString.CUSTOM: {
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.value.from - <any>this.service.historyPeriod.value.to) / (1000 * 60 * 60 * 24));
                dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
                if (isFuture(addDays(this.service.historyPeriod.value.to, dateDistance * 2))) {
                    this.disableArrow = true;
                }
                if (!isFuture(addDays(this.service.historyPeriod.value.to, dateDistance))) {
                    this.setDateRange(new DefaultTypes.HistoryPeriod(addDays(this.service.historyPeriod.value.from, dateDistance), addDays(this.service.historyPeriod.value.to, dateDistance)));
                }
                break;
            }
        }
    }

    public goBackward() {
        switch (this.service.periodString) {
            case DefaultTypes.PeriodString.DAY: {
                //disables changing period to next day when next day is reached if current day is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.disableArrow = false;
                this.setDateRange(new DefaultTypes.HistoryPeriod(subDays(this.service.historyPeriod.value.from, 1), subDays((endOfDay(this.service.historyPeriod.value.to)), 1)));
                break;
            }
            case DefaultTypes.PeriodString.WEEK: {
                //disables changing period to next week when next week is reached if current week is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.disableArrow = false;
                this.setDateRange(new DefaultTypes.HistoryPeriod(subWeeks(this.service.historyPeriod.value.from, 1), subWeeks(endOfWeek(this.service.historyPeriod.value.to, { weekStartsOn: 1 }), 1)));
                break;
            }
            case DefaultTypes.PeriodString.MONTH: {
                //disables changing period to next month when next month is reached if current month is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.disableArrow = false;
                this.setDateRange(new DefaultTypes.HistoryPeriod(subMonths(this.service.historyPeriod.value.from, 1), endOfMonth(subMonths(this.service.historyPeriod.value.to, 1))));
                break;
            }
            case DefaultTypes.PeriodString.YEAR: {
                //disables changing period to next year when next year is reached if current year is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.disableArrow = false;
                this.setDateRange(new DefaultTypes.HistoryPeriod(subYears(this.service.historyPeriod.value.from, 1), endOfYear(subYears(this.service.historyPeriod.value.to, 1))));
                break;
            }
            case DefaultTypes.PeriodString.CUSTOM: {
                this.disableArrow = false;
                let dateDistance = Math.floor(Math.abs(<any>this.service.historyPeriod.value.from - <any>this.service.historyPeriod.value.to) / (1000 * 60 * 60 * 24));
                dateDistance == 0 ? dateDistance = 1 : dateDistance = dateDistance;
                this.setDateRange(new DefaultTypes.HistoryPeriod(subDays(this.service.historyPeriod.value.from, dateDistance), subDays(this.service.historyPeriod.value.to, dateDistance)));
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
            this.service.historyPeriod.value?.getText(this.translate);
        }, this.millisecondsUntilnextPeriod());
    }

    /**
     * changes history period date and text when next week is reached
     */
    private forwardToNextWeekWhenReached() {
        this.changePeriodTimeout = setTimeout(() => {
            this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfWeek(new Date(), { weekStartsOn: 1 })));
            this.service.historyPeriod.value?.getText(this.translate);
        }, this.millisecondsUntilnextPeriod());
    }

    /**
     * changes history period date and text when next week is reached
     */
    private forwardToNextMonthWhenReached() {
        // 2147483647 (32 bit int) is setTimeout max value
        if (this.millisecondsUntilnextPeriod() < 2147483647) {
            this.changePeriodTimeout = setTimeout(() => {
                this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfMonth(new Date())));
                this.service.historyPeriod.value?.getText(this.translate);
            }, this.millisecondsUntilnextPeriod());
        }
    }
    /**
    * changes history period date and text when next week is reached
    */
    private forwardToNextYearWhenReached() {
        // 2147483647 (32 bit int) is setTimeout max value
        if (this.millisecondsUntilnextPeriod() < 2147483647) {
            this.changePeriodTimeout = setTimeout(() => {
                this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfYear(new Date())));
                this.service.historyPeriod.value?.getText(this.translate);
            }, this.millisecondsUntilnextPeriod());
        }
    }

    /**
     * calculates the milliseconds until next period (Day|Week) will occour
     * is used to change date period
     */
    private millisecondsUntilnextPeriod(): number {
        // + 1000 to reach the next day
        switch (this.service.periodString) {
            case DefaultTypes.PeriodString.DAY: {
                let currentDayTime = new Date();
                let endOfDayTime = endOfDay(currentDayTime);
                return differenceInMilliseconds(endOfDayTime, currentDayTime) + 1000;
            }
            case DefaultTypes.PeriodString.WEEK: {
                let currentDayTime = new Date();
                let endOfWeekTime = endOfWeek(currentDayTime, { weekStartsOn: 1 });
                return differenceInMilliseconds(endOfWeekTime, currentDayTime) + 1000;
            }
            case DefaultTypes.PeriodString.MONTH: {
                let currentDayTime = new Date();
                let endOfMonthTime = endOfMonth(currentDayTime);
                return differenceInMilliseconds(endOfMonthTime, currentDayTime) + 1000;
            }
            case DefaultTypes.PeriodString.YEAR: {
                let currentDayTime = new Date();
                let endOfYearTime = endOfYear(currentDayTime);
                return differenceInMilliseconds(endOfYearTime, currentDayTime) + 1000;
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
                edge: this.edge
            }
        });
        await popover.present();
        popover.onDidDismiss().then(() => {
            this.checkArrowAutomaticForwarding();
        });
    }
}