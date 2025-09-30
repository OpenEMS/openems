// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { CalAnimation, IAngularMyDpOptions, IMyDate, IMyDateRangeModel } from "@nodro7/angular-mydatepicker";
import { addDays, endOfMonth, endOfWeek, endOfYear, getDate, getMonth, getYear, startOfMonth, startOfWeek, startOfYear } from "date-fns";
import { EdgePermission, Service, Utils } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { Language } from "src/app/shared/type/language";
import { Edge } from "../../edge/edge";

@Component({
    selector: "pickdatepopover",
    templateUrl: "./POPOVER.COMPONENT.HTML",
    standalone: false,
    styles: [
        `
        :host{
             --width: fit-content !important;
        }
        `,
    ],
})
export class PickDatePopoverComponent implements OnInit {

    @Input() public setDateRange: (period: DEFAULT_TYPES.HISTORY_PERIOD) => void;
    @Input() public edge: Edge | null = null;
    @Input() public historyPeriods: DEFAULT_TYPES.PERIOD_STRING_VALUES[] = [];

    public locale: string = LANGUAGE.DEFAULT.KEY;
    public showCustomDate: boolean = false;

    protected periods: string[] = [];
    protected readonly TOMORROW = addDays(new Date(), 1);
    protected myDpOptions: IAngularMyDpOptions = {

        stylesData: {
            selector: "dp1",
            styles: `
            .myDpSelector{
                background-color: var(--ion-color-background);
                color: var(--color);
                background: var(--ion-color-background);
                width: inherit !important;
            }
            .dp1 {
                overflow-x: hidden;
            }
            .dp1 .myDpIconLeftArrow, 
            .dp1 .myDpIconRightArrow,
            .dp1 .myDpHeaderBtn {
                color: var(--ion-color-primary);
            }
            .dp1 .myDpHeaderBtn:focus,
            .dp1 .myDpMonthLabel:focus,
            .dp1 .myDpYearLabel:focus {
                color: var(--ion-color-primary);
             }
            .dp1 .myDpDaycell:focus,
            .dp1 .myDpMonthcell:focus,
            .dp1 .myDpYearcell:focus {
                box-shadow: inset 0 0 0 0.063em var(--ion-color-primary-contrast);
            }
            .dp1 .myDpSelectedDay,
            .dp1 .myDpSelectedMonth,
            .dp1 .myDpSelectedYear {
                background-color: var(--ion-color-secondary-shade);
                color: var(--ion-color-primary-contrast);
                }
            .dp1 .myDpTableSingleDay:hover, 
            .dp1 .myDpTableSingleMonth:hover, 
            .dp1 .myDpTableSingleYear:hover {
                background-color: var(--ion-color-secondary-shade);
                color: white !important;
                }
            .dp1 .myDpMarkCurrDay,
            .dp1 .myDpMarkCurrMonth,
            .dp1 .myDpMarkCurrYear {
                color: var(--ion-color-text-percentage-bar) !important;
                border-bottom: 0.125em solid var(--ion-color-text-percentage-bar) !important;
                }
            .dp1 .myDpRangeColor {
                background-color: var(--ion-color-primary);
                }
            .ng-mydp * {
                background-color: var(--ion-color-background);
                color: var(--color);
                border: 0;
            }
            .myDpDisabled {
               color: var(--ion-color-secondary-tint);
               background: repeating-linear-gradient(-45deg, rgba(88, 87, 87, 0.45) 0.438em, rgba(192, 192, 192, 0.3) 0.5em, transparent 0.438em, transparent 0.875em ) !important;
            }
             `,
        },
        calendarAnimation: { in: CAL_ANIMATION.FLIP_DIAGONAL, out: CAL_ANIMATION.SCALE_CENTER },
        dateFormat: "DD.MM.YYYY",
        dateRange: true,
        disableSince: THIS.TO_IMY_DATE(THIS.TOMORROW),
        disableUntil: { day: 1, month: 1, year: 2013 }, // TODO start with date since the edge is available
        inline: true,
        selectorHeight: "14.063em",
        selectorWidth: "15.688em",
        showWeekNumbers: true,
    };
    protected readonly DefaultTypes = DefaultTypes;
    private readonly TODAY = new Date();

    constructor(
        public service: Service,
        public popoverCtrl: PopoverController,
        public translate: TranslateService,
    ) { }

    public onDateChanged(event: IMyDateRangeModel) {
        THIS.SERVICE.HISTORY_PERIOD.NEXT(new DEFAULT_TYPES.HISTORY_PERIOD(EVENT.BEGIN_JS_DATE, EVENT.END_JS_DATE));
        THIS.SERVICE.PERIOD_STRING = DEFAULT_TYPES.PERIOD_STRING.CUSTOM;
        THIS.POPOVER_CTRL.DISMISS();
    }

    ngOnInit() {

        THIS.LOCALE = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).key;
        // Restrict user to pick date before ibn-date
        THIS.MY_DP_OPTIONS.DISABLE_UNTIL = { day: UTILS.SUBTRACT_SAFELY(getDate(THIS.EDGE?.firstSetupProtocol), 1) ?? 1, month: UTILS.ADD_SAFELY(getMonth(THIS.EDGE?.firstSetupProtocol), 1) ?? 1, year: THIS.EDGE?.firstSetupProtocol?.getFullYear() ?? 2013 };

        // Filter out custom due to different on click event
        THIS.PERIODS = EDGE_PERMISSION.GET_ALLOWED_HISTORY_PERIODS(THIS.EDGE, THIS.HISTORY_PERIODS).filter(period => period !== DEFAULT_TYPES.PERIOD_STRING.CUSTOM);
    }

    /**
     * This is called by the input button on the UI.
     *
     * @param period
     * @param from
     * @param to
     */
    public setPeriod(period: DEFAULT_TYPES.PERIOD_STRING) {
        switch (period) {
            case DEFAULT_TYPES.PERIOD_STRING.DAY: {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(THIS.TODAY, THIS.TODAY));
                THIS.SERVICE.PERIOD_STRING = period;
                THIS.POPOVER_CTRL.DISMISS();
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.WEEK: {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(startOfWeek(THIS.TODAY, { weekStartsOn: 1 }), endOfWeek(THIS.TODAY, { weekStartsOn: 1 })));
                THIS.SERVICE.PERIOD_STRING = period;
                THIS.POPOVER_CTRL.DISMISS();
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.MONTH: {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(startOfMonth(THIS.TODAY), endOfMonth(THIS.TODAY)));
                THIS.SERVICE.PERIOD_STRING = period;
                THIS.POPOVER_CTRL.DISMISS();
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.YEAR: {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(startOfYear(THIS.TODAY), endOfYear(THIS.TODAY)));
                THIS.SERVICE.PERIOD_STRING = period;
                THIS.POPOVER_CTRL.DISMISS();
                break;
            }
            case DEFAULT_TYPES.PERIOD_STRING.TOTAL: {
                THIS.SET_DATE_RANGE(new DEFAULT_TYPES.HISTORY_PERIOD(THIS.EDGE?.firstSetupProtocol, endOfYear(THIS.TODAY)));
                THIS.SERVICE.PERIOD_STRING = period;
                THIS.POPOVER_CTRL.DISMISS();
                break;
            }
            default:
                break;
        }
    }

    /**
     * Converts a 'Date' to 'IMyDate' format.
     *
     * @param date the 'Date'
     * @returns the 'IMyDate'
     */
    private toIMyDate(date: Date): IMyDate {
        return { year: getYear(date), month: getMonth(date) + 1, day: getDate(date) };
    }

}
