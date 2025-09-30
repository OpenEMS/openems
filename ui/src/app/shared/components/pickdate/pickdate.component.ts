// @ts-strict-ignore
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { addDays, addMonths, addWeeks, addYears, differenceInDays, differenceInMilliseconds, endOfDay, endOfMonth, endOfWeek, endOfYear, isAfter, isBefore, isFuture, startOfDay, startOfMonth, startOfWeek, startOfYear, subDays, subMonths, subWeeks, subYears } from "date-fns";

import { Edge, Service } from "../../shared";
import { DefaultTypes } from "../../type/defaulttypes";
import { DateUtils } from "../../utils/date/dateutils";
import { PickDatePopoverComponent } from "./popover/POPOVER.COMPONENT";

@Component({
    selector: "pickdate",
    templateUrl: "./PICKDATE.COMPONENT.HTML",
    standalone: false,
    styles: [`
        ion-BUTTON.PICKDATE-styles {
            background: transparent !important;
            box-shadow: none !important;
        }

        ion-BUTTON.PICKDATE-styles::part(native) {
            background: var(--ion-color-toolbar-primary);
            color: var(--ion-menu-color);
            box-shadow: 0em 0.3em 0.3em var(--ion-color-primary-rgba);
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            height: 100%;
            text-transform: uppercase;
            border-radius: 0.5em;
        }

        ion-BUTTON.PICKDATE-styles:hover::part(native) {
            transition: background-color 0.1s ease-in-out;
            box-shadow: none;
        }

        ion-BUTTON.PICKDATE-styles {
            :is(active::part(native)) {
                transform: scale(0.98);
                box-shadow: inset 0em 0.125em 0.25em rgba(0, 0, 0, 0.4);
                opacity: 0.8;
            }}
        `],
})
export class PickDateComponent implements OnInit, OnDestroy {

    @Input() public historyPeriods: DEFAULT_TYPES.PERIOD_STRING_VALUES[] = [];
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

        switch (SERVICE.PERIOD_STRING) {
            case DEFAULT_TYPES.PERIOD_STRING.DAY:
                return isAfter(new Date(), startOfDay(addDays(SERVICE.HISTORY_PERIOD.VALUE.TO, 1)));
            case DEFAULT_TYPES.PERIOD_STRING.WEEK:
                return isAfter(new Date(), startOfDay(startOfWeek(addWeeks(SERVICE.HISTORY_PERIOD.VALUE.TO, 1), { weekStartsOn: 1 })));
            case DEFAULT_TYPES.PERIOD_STRING.MONTH:
                return isAfter(new Date(), startOfMonth(addMonths(SERVICE.HISTORY_PERIOD.VALUE.TO, 1)));
            case DEFAULT_TYPES.PERIOD_STRING.YEAR:
                return isAfter(new Date(), startOfYear(addYears(SERVICE.HISTORY_PERIOD.VALUE.TO, 1)));
            case DEFAULT_TYPES.PERIOD_STRING.TOTAL:
                return false;
            case DEFAULT_TYPES.PERIOD_STRING.CUSTOM: {
                const timeRange: number = differenceInDays(SERVICE.HISTORY_PERIOD.VALUE.TO, SERVICE.HISTORY_PERIOD.VALUE.FROM);
                return isAfter(startOfDay(new Date()), addDays(SERVICE.HISTORY_PERIOD.VALUE.TO, timeRange));
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

        switch (SERVICE.PERIOD_STRING) {
            case DEFAULT_TYPES.PERIOD_STRING.DAY:
                return isBefore(startOfDay(firstSetupProtocol), endOfDay(subDays(SERVICE.HISTORY_PERIOD.VALUE.FROM, 1)));
            case DEFAULT_TYPES.PERIOD_STRING.WEEK:
                return isBefore(firstSetupProtocol, endOfWeek(subWeeks(SERVICE.HISTORY_PERIOD.VALUE.FROM, 1)));
            case DEFAULT_TYPES.PERIOD_STRING.MONTH:
                return isBefore(firstSetupProtocol, endOfMonth(subWeeks(SERVICE.HISTORY_PERIOD.VALUE.FROM, 1)));
            case DEFAULT_TYPES.PERIOD_STRING.YEAR:
                return isBefore(firstSetupProtocol, endOfYear(subWeeks(SERVICE.HISTORY_PERIOD.VALUE.FROM, 1)));
            case DEFAULT_TYPES.PERIOD_STRING.TOTAL:
                return false;
            case DEFAULT_TYPES.PERIOD_STRING.CUSTOM: {
                const timeRange: number = differenceInDays(SERVICE.HISTORY_PERIOD.VALUE.TO, SERVICE.HISTORY_PERIOD.VALUE.FROM);
                return isBefore(startOfDay(firstSetupProtocol), startOfDay(subDays(SERVICE.HISTORY_PERIOD.VALUE.FROM, timeRange)));
            }
        }
    }

    public ngOnInit() {
        THIS.CHECK_ARROW_AUTOMATIC_FORWARDING();
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;

            THIS.IS_BACK_ARROW_ALLOWED = PICK_DATE_COMPONENT.IS_PREVIOUS_PERIOD_ALLOWED(THIS.SERVICE, THIS.EDGE?.firstSetupProtocol);
            THIS.IS_FORWARD_ARROW_ALLOWED = PICK_DATE_COMPONENT.IS_NEXT_PERIOD_ALLOWED(THIS.SERVICE);
        });
    }

    public ngOnDestroy() {
        if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
            clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
        }
    }

    /**
     * checks if arrow has to be disabled/enabled and if automatic forwarding is needed dependend on the date
     */
    public checkArrowAutomaticForwarding() {
        THIS.IS_BACK_ARROW_ALLOWED = PICK_DATE_COMPONENT.IS_PREVIOUS_PERIOD_ALLOWED(THIS.SERVICE, THIS.EDGE?.firstSetupProtocol);
        THIS.IS_FORWARD_ARROW_ALLOWED = PICK_DATE_COMPONENT.IS_NEXT_PERIOD_ALLOWED(THIS.SERVICE);

        switch (THIS.SERVICE.PERIOD_STRING) {
            case DEFAULT_TYPES.PERIOD_STRING.DAY: {
                if (isFuture(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1))) {
                    //waits until next day is reached to set next days period
                    THIS.FORWARD_TO_NEXT_DAY_WHEN_REACHED();
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next day when next day is reached if current day is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.DISABLE_ARROW = false;
                }
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.WEEK: {
                if (isFuture(addWeeks(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1))) {
                    //waits until next week is reached to set next weeks period
                    THIS.FORWARD_TO_NEXT_WEEK_WHEN_REACHED();
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.DISABLE_ARROW = false;
                }
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.MONTH: {
                if (isFuture(addMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1))) {
                    //waits until next month is reached to set next months period
                    THIS.FORWARD_TO_NEXT_MONTH_WHEN_REACHED();
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next month when next month is reached if current month is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.DISABLE_ARROW = false;
                }
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.YEAR: {
                if (isFuture(addYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1))) {
                    //waits until next week is reached to set next weeks period
                    // THIS.FORWARD_TO_NEXT_YEAR_WHEN_REACHED()
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next year when next year is reached if current year is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.DISABLE_ARROW = false;
                }
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.TOTAL: {
                THIS.DISABLE_ARROW = true;
                break;
            }

            case DEFAULT_TYPES.PERIOD_STRING.CUSTOM: {
                let dateDistance = MATH.FLOOR(MATH.ABS(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM.GET_TIME() - THIS.SERVICE.HISTORY_PERIOD.VALUE.TO.GET_TIME()) / (1000 * 60 * 60 * 24));
                if (dateDistance == 0) {
                    dateDistance = 1;
                }
                if (isFuture(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, dateDistance * 2))) {
                    THIS.DISABLE_ARROW = true;
                } else {
                    THIS.DISABLE_ARROW = false;
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
    public setDateRange(period: DEFAULT_TYPES.HISTORY_PERIOD) {
        THIS.SERVICE.HISTORY_PERIOD.NEXT(period);
        THIS.IS_BACK_ARROW_ALLOWED = PICK_DATE_COMPONENT.IS_PREVIOUS_PERIOD_ALLOWED(THIS.SERVICE, THIS.EDGE?.firstSetupProtocol);
        THIS.IS_FORWARD_ARROW_ALLOWED = PICK_DATE_COMPONENT.IS_NEXT_PERIOD_ALLOWED(THIS.SERVICE);
    }

    public goForward() {
        switch (THIS.SERVICE.PERIOD_STRING) {
            case DEFAULT_TYPES.PERIOD_STRING.DAY: {
                if (isFuture(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 2))) {
                    //waits until next day is reached to set next days period
                    THIS.FORWARD_TO_NEXT_DAY_WHEN_REACHED();
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), addDays(endOfDay(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO), 1)));
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next day when next day is reached if current day is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), addDays(endOfDay(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO), 1)));
                }
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.WEEK: {
                if (isFuture(addWeeks(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 2))) {
                    //waits until next week is reached to set next weeks period
                    THIS.FORWARD_TO_NEXT_WEEK_WHEN_REACHED();
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addWeeks(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), addWeeks(endOfWeek(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, { weekStartsOn: 1 }), 1)));
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addWeeks(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), addWeeks(endOfWeek(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, { weekStartsOn: 1 }), 1)));
                }
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.MONTH: {
                if (isFuture(addMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 2))) {
                    //waits until next month is reached to set next months period
                    THIS.FORWARD_TO_NEXT_MONTH_WHEN_REACHED();
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), endOfMonth(addMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, 1))));
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), endOfMonth(addMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, 1))));
                }
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.YEAR: {
                if (isFuture(addYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 2))) {
                    //waits until next week is reached to set next weeks period
                    THIS.FORWARD_TO_NEXT_YEAR_WHEN_REACHED();
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), endOfYear(addYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, 1))));
                    THIS.DISABLE_ARROW = true;
                } else {
                    //disables changing period to next week when next week is reached if current week is not selected
                    if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                        clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                    }
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), endOfYear(addYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, 1))));
                }
                break;
            }

            case DEFAULT_TYPES.PERIOD_STRING.TOTAL: {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(THIS.EDGE?.firstSetupProtocol ?? DATE_UTILS.STRING_TO_DATE("03.11.2022 16:04:37"), endOfYear(addYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, 1))));
                THIS.DISABLE_ARROW = true;
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.CUSTOM: {
                let dateDistance = MATH.FLOOR(MATH.ABS(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM.GET_TIME() - THIS.SERVICE.HISTORY_PERIOD.VALUE.TO.GET_TIME()) / (1000 * 60 * 60 * 24));
                if (dateDistance == 0) {
                    dateDistance = 1;
                }
                if (isFuture(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, dateDistance * 2))) {
                    THIS.DISABLE_ARROW = true;
                }
                if (!isFuture(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, dateDistance))) {
                    THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, dateDistance), addDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, dateDistance)));
                }
                break;
            }
        }
    }

    public goBackward() {
        switch (THIS.SERVICE.PERIOD_STRING) {
            case DEFAULT_TYPES.PERIOD_STRING.DAY: {
                //disables changing period to next day when next day is reached if current day is not selected
                if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                    clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                }

                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(subDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), subDays((endOfDay(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO)), 1)));
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.WEEK: {
                //disables changing period to next week when next week is reached if current week is not selected
                if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                    clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                }
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(subWeeks(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), subWeeks(endOfWeek(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, { weekStartsOn: 1 }), 1)));
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.MONTH: {
                //disables changing period to next month when next month is reached if current month is not selected
                if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                    clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                }
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(subMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), endOfMonth(subMonths(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, 1))));
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.YEAR: {
                //disables changing period to next year when next year is reached if current year is not selected
                if (THIS.CHANGE_PERIOD_TIMEOUT != null) {
                    clearTimeout(THIS.CHANGE_PERIOD_TIMEOUT);
                }
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(subYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, 1), endOfYear(subYears(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, 1))));
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.CUSTOM: {
                let dateDistance = MATH.FLOOR(MATH.ABS(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM.GET_TIME() - THIS.SERVICE.HISTORY_PERIOD.VALUE.TO.GET_TIME()) / (1000 * 60 * 60 * 24));
                if (dateDistance == 0) {
                    dateDistance = 1;
                }
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(subDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, dateDistance), subDays(THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, dateDistance)));
                break;
            }
            default:
                break;

        }
    }

    async presentPopover(ev: any) {
        const popover = await THIS.POPOVER_CTRL.CREATE({
            component: PickDatePopoverComponent,
            event: ev,
            translucent: false,
            cssClass: "pickdate-popover",
            componentProps: {
                setDateRange: THIS.SET_DATE_RANGE,
                edge: THIS.EDGE,
                historyPeriods: THIS.HISTORY_PERIODS,
            },
        });
        await POPOVER.PRESENT();
        POPOVER.ON_DID_DISMISS().then(() => {
            THIS.CHECK_ARROW_AUTOMATIC_FORWARDING();
        });
    }

    /**
     * changes history period date and text when next day is reached
     */
    private forwardToNextDayWhenReached() {
        THIS.CHANGE_PERIOD_TIMEOUT = setTimeout(() => {
            THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(new Date(), new Date()));
            THIS.SERVICE.HISTORY_PERIOD.VALUE?.getText(THIS.TRANSLATE, THIS.SERVICE);
        }, THIS.MILLISECONDS_UNTILNEXT_PERIOD());
    }

    /**
     * changes history period date and text when next week is reached
     */
    private forwardToNextWeekWhenReached() {
        THIS.CHANGE_PERIOD_TIMEOUT = setTimeout(() => {
            THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(new Date(), endOfWeek(new Date(), { weekStartsOn: 1 })));
            THIS.SERVICE.HISTORY_PERIOD.VALUE?.getText(THIS.TRANSLATE, THIS.SERVICE);
        }, THIS.MILLISECONDS_UNTILNEXT_PERIOD());
    }

    /**
     * changes history period date and text when next week is reached
     */
    private forwardToNextMonthWhenReached() {
        // 2147483647 (32 bit int) is setTimeout max value
        if (THIS.MILLISECONDS_UNTILNEXT_PERIOD() < 2147483647) {
            THIS.CHANGE_PERIOD_TIMEOUT = setTimeout(() => {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(new Date(), endOfMonth(new Date())));
                THIS.SERVICE.HISTORY_PERIOD.VALUE?.getText(THIS.TRANSLATE, THIS.SERVICE);
            }, THIS.MILLISECONDS_UNTILNEXT_PERIOD());
        }
    }
    /**
    * changes history period date and text when next week is reached
    */
    private forwardToNextYearWhenReached() {
        // 2147483647 (32 bit int) is setTimeout max value
        if (THIS.MILLISECONDS_UNTILNEXT_PERIOD() < 2147483647) {
            THIS.CHANGE_PERIOD_TIMEOUT = setTimeout(() => {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(new Date(), endOfYear(new Date())));
                THIS.SERVICE.HISTORY_PERIOD.VALUE?.getText(THIS.TRANSLATE, THIS.SERVICE);
            }, THIS.MILLISECONDS_UNTILNEXT_PERIOD());
        }
    }

    /**
     * calculates the milliseconds until next period (Day|Week) will occour
     * is used to change date period
     */
    private millisecondsUntilnextPeriod(): number | null {
        // + 1000 to reach the next day
        switch (THIS.SERVICE.PERIOD_STRING) {
            case DEFAULT_TYPES.PERIOD_STRING.DAY: {
                const currentDayTime = new Date();
                const endOfDayTime = endOfDay(currentDayTime);
                return differenceInMilliseconds(endOfDayTime, currentDayTime) + 1000;
            }
            case DEFAULT_TYPES.PERIOD_STRING.WEEK: {
                const currentDayTime = new Date();
                const endOfWeekTime = endOfWeek(currentDayTime, { weekStartsOn: 1 });
                return differenceInMilliseconds(endOfWeekTime, currentDayTime) + 1000;
            }
            case DEFAULT_TYPES.PERIOD_STRING.MONTH: {
                const currentDayTime = new Date();
                const endOfMonthTime = endOfMonth(currentDayTime);
                return differenceInMilliseconds(endOfMonthTime, currentDayTime) + 1000;
            }
            case DEFAULT_TYPES.PERIOD_STRING.YEAR: {
                const currentDayTime = new Date();
                const endOfYearTime = endOfYear(currentDayTime);
                return differenceInMilliseconds(endOfYearTime, currentDayTime) + 1000;
            }
            default:
                return null;
        }
    }

}
