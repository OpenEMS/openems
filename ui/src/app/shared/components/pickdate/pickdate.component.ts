// @ts-strict-ignore
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { addDays, addMonths, addWeeks, addYears, differenceInDays, differenceInMilliseconds, endOfDay, endOfMonth, endOfWeek, endOfYear, isAfter, isBefore, isFuture, startOfDay, startOfMonth, startOfWeek, startOfYear, subDays, subMonths, subWeeks, subYears } from "date-fns";

import { DefaultTypes } from "../../service/defaulttypes";
import { Edge, Service } from "../../shared";
import { DateUtils } from "../../utils/date/dateutils";
import { PickDatePopoverComponent } from "./popover/popover.component";

@Component({
    selector: "pickdate",
    templateUrl: "./pickdate.component.html",
    standalone: false,
})
export class PickDateComponent implements OnInit, OnDestroy {

    @Input() public historyPeriods: DefaultTypes.PeriodStringValues[] = [];
    public disableArrow: boolean | null = null;
    protected isAllowedToSeeDay: boolean = true;

    protected isForwardArrowAllowed: boolean = false;
    protected isBackArrowAllowed: boolean = true;

    private changePeriodTimeout = null;
    private edge: Edge | null = null;


    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    /**
 * Checks if next time period is allowed to be queried
 *
 * @param service the service
 * @returns true, if requested toDate is not in the future
 */
    public static isNextPeriodAllowed(service: Service): boolean {

        switch (service.periodString) {
            case DefaultTypes.PeriodString.DAY:
                return isAfter(new Date(), startOfDay(addDays(service.historyPeriod.value.to, 1)));
            case DefaultTypes.PeriodString.WEEK:
                return isAfter(new Date(), startOfDay(startOfWeek(addWeeks(service.historyPeriod.value.to, 1), { weekStartsOn: 1 })));
            case DefaultTypes.PeriodString.MONTH:
                return isAfter(new Date(), startOfMonth(addMonths(service.historyPeriod.value.to, 1)));
            case DefaultTypes.PeriodString.YEAR:
                return isAfter(new Date(), startOfYear(addYears(service.historyPeriod.value.to, 1)));
            case DefaultTypes.PeriodString.TOTAL:
                return false;
            case DefaultTypes.PeriodString.CUSTOM: {
                const timeRange: number = differenceInDays(service.historyPeriod.value.to, service.historyPeriod.value.from);
                return isAfter(startOfDay(new Date()), addDays(service.historyPeriod.value.to, timeRange));
            }
        }
    }

    /**
 * Checks if previous time period is allowed to be queried
 *
 * @param service the service
 * @param firstSetupProtocol the date of setting up the edge
 * @returns true, if requested fromDate is not before firstSetupProtocolDate
 */
    public static isPreviousPeriodAllowed(service: Service, firstSetupProtocol: Date | null): boolean {

        if (!firstSetupProtocol) {
            return true;
        }

        switch (service.periodString) {
            case DefaultTypes.PeriodString.DAY:
                return isBefore(startOfDay(firstSetupProtocol), endOfDay(subDays(service.historyPeriod.value.from, 1)));
            case DefaultTypes.PeriodString.WEEK:
                return isBefore(firstSetupProtocol, endOfWeek(subWeeks(service.historyPeriod.value.from, 1)));
            case DefaultTypes.PeriodString.MONTH:
                return isBefore(firstSetupProtocol, endOfMonth(subWeeks(service.historyPeriod.value.from, 1)));
            case DefaultTypes.PeriodString.YEAR:
                return isBefore(firstSetupProtocol, endOfYear(subWeeks(service.historyPeriod.value.from, 1)));
            case DefaultTypes.PeriodString.TOTAL:
                return false;
            case DefaultTypes.PeriodString.CUSTOM: {
                const timeRange: number = differenceInDays(service.historyPeriod.value.to, service.historyPeriod.value.from);
                return isBefore(startOfDay(firstSetupProtocol), startOfDay(subDays(service.historyPeriod.value.from, timeRange)));
            }
        }
    }

    public ngOnInit() {
        this.checkArrowAutomaticForwarding();
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;

            this.isBackArrowAllowed = PickDateComponent.isPreviousPeriodAllowed(this.service, this.edge?.firstSetupProtocol);
            this.isForwardArrowAllowed = PickDateComponent.isNextPeriodAllowed(this.service);
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
        this.isBackArrowAllowed = PickDateComponent.isPreviousPeriodAllowed(this.service, this.edge?.firstSetupProtocol);
        this.isForwardArrowAllowed = PickDateComponent.isNextPeriodAllowed(this.service);

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
                break;
            }
            case DefaultTypes.PeriodString.TOTAL: {
                this.disableArrow = true;
                break;
            }

            case DefaultTypes.PeriodString.CUSTOM: {
                let dateDistance = Math.floor(Math.abs(this.service.historyPeriod.value.from.getTime() - this.service.historyPeriod.value.to.getTime()) / (1000 * 60 * 60 * 24));
                if (dateDistance == 0) {
                    dateDistance = 1;
                }
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
        this.isBackArrowAllowed = PickDateComponent.isPreviousPeriodAllowed(this.service, this.edge?.firstSetupProtocol);
        this.isForwardArrowAllowed = PickDateComponent.isNextPeriodAllowed(this.service);
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

            case DefaultTypes.PeriodString.TOTAL: {
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.edge?.firstSetupProtocol ?? DateUtils.stringToDate("03.11.2022 16:04:37"), endOfYear(addYears(this.service.historyPeriod.value.to, 1))));
                this.disableArrow = true;
                break;
            }
            case DefaultTypes.PeriodString.CUSTOM: {
                let dateDistance = Math.floor(Math.abs(this.service.historyPeriod.value.from.getTime() - this.service.historyPeriod.value.to.getTime()) / (1000 * 60 * 60 * 24));
                if (dateDistance == 0) {
                    dateDistance = 1;
                }
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

                this.setDateRange(new DefaultTypes.HistoryPeriod(subDays(this.service.historyPeriod.value.from, 1), subDays((endOfDay(this.service.historyPeriod.value.to)), 1)));
                break;
            }
            case DefaultTypes.PeriodString.WEEK: {
                //disables changing period to next week when next week is reached if current week is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.setDateRange(new DefaultTypes.HistoryPeriod(subWeeks(this.service.historyPeriod.value.from, 1), subWeeks(endOfWeek(this.service.historyPeriod.value.to, { weekStartsOn: 1 }), 1)));
                break;
            }
            case DefaultTypes.PeriodString.MONTH: {
                //disables changing period to next month when next month is reached if current month is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.setDateRange(new DefaultTypes.HistoryPeriod(subMonths(this.service.historyPeriod.value.from, 1), endOfMonth(subMonths(this.service.historyPeriod.value.to, 1))));
                break;
            }
            case DefaultTypes.PeriodString.YEAR: {
                //disables changing period to next year when next year is reached if current year is not selected
                if (this.changePeriodTimeout != null) {
                    clearTimeout(this.changePeriodTimeout);
                }
                this.setDateRange(new DefaultTypes.HistoryPeriod(subYears(this.service.historyPeriod.value.from, 1), endOfYear(subYears(this.service.historyPeriod.value.to, 1))));
                break;
            }
            case DefaultTypes.PeriodString.CUSTOM: {
                let dateDistance = Math.floor(Math.abs(this.service.historyPeriod.value.from.getTime() - this.service.historyPeriod.value.to.getTime()) / (1000 * 60 * 60 * 24));
                if (dateDistance == 0) {
                    dateDistance = 1;
                }
                this.setDateRange(new DefaultTypes.HistoryPeriod(subDays(this.service.historyPeriod.value.from, dateDistance), subDays(this.service.historyPeriod.value.to, dateDistance)));
                break;
            }
            default:
                break;

        }
    }

    async presentPopover(ev: any) {
        const popover = await this.popoverCtrl.create({
            component: PickDatePopoverComponent,
            event: ev,
            translucent: false,
            cssClass: "pickdate-popover",
            componentProps: {
                setDateRange: this.setDateRange,
                edge: this.edge,
                historyPeriods: this.historyPeriods,
            },
        });
        await popover.present();
        popover.onDidDismiss().then(() => {
            this.checkArrowAutomaticForwarding();
        });
    }

    /**
     * changes history period date and text when next day is reached
     */
    private forwardToNextDayWhenReached() {
        this.changePeriodTimeout = setTimeout(() => {
            this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), new Date()));
            this.service.historyPeriod.value?.getText(this.translate, this.service);
        }, this.millisecondsUntilnextPeriod());
    }

    /**
     * changes history period date and text when next week is reached
     */
    private forwardToNextWeekWhenReached() {
        this.changePeriodTimeout = setTimeout(() => {
            this.setDateRange(new DefaultTypes.HistoryPeriod(new Date(), endOfWeek(new Date(), { weekStartsOn: 1 })));
            this.service.historyPeriod.value?.getText(this.translate, this.service);
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
                this.service.historyPeriod.value?.getText(this.translate, this.service);
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
                this.service.historyPeriod.value?.getText(this.translate, this.service);
            }, this.millisecondsUntilnextPeriod());
        }
    }

    /**
     * calculates the milliseconds until next period (Day|Week) will occour
     * is used to change date period
     */
    private millisecondsUntilnextPeriod(): number | null {
        // + 1000 to reach the next day
        switch (this.service.periodString) {
            case DefaultTypes.PeriodString.DAY: {
                const currentDayTime = new Date();
                const endOfDayTime = endOfDay(currentDayTime);
                return differenceInMilliseconds(endOfDayTime, currentDayTime) + 1000;
            }
            case DefaultTypes.PeriodString.WEEK: {
                const currentDayTime = new Date();
                const endOfWeekTime = endOfWeek(currentDayTime, { weekStartsOn: 1 });
                return differenceInMilliseconds(endOfWeekTime, currentDayTime) + 1000;
            }
            case DefaultTypes.PeriodString.MONTH: {
                const currentDayTime = new Date();
                const endOfMonthTime = endOfMonth(currentDayTime);
                return differenceInMilliseconds(endOfMonthTime, currentDayTime) + 1000;
            }
            case DefaultTypes.PeriodString.YEAR: {
                const currentDayTime = new Date();
                const endOfYearTime = endOfYear(currentDayTime);
                return differenceInMilliseconds(endOfYearTime, currentDayTime) + 1000;
            }
            default:
                return null;
        }
    }

}
