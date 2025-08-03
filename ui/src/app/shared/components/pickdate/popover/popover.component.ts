// @ts-strict-ignore
import { Component, Input, OnInit, inject } from "@angular/core";
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
    templateUrl: "./popover.component.html",
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
    service = inject(Service);
    popoverCtrl = inject(PopoverController);
    translate = inject(TranslateService);


    @Input() public setDateRange: (period: DefaultTypes.HistoryPeriod) => void;
    @Input() public edge: Edge | null = null;
    @Input() public historyPeriods: DefaultTypes.PeriodStringValues[] = [];

    public locale: string = Language.DEFAULT.key;
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
        calendarAnimation: { in: CalAnimation.FlipDiagonal, out: CalAnimation.ScaleCenter },
        dateFormat: "dd.mm.yyyy",
        dateRange: true,
        disableSince: this.toIMyDate(this.TOMORROW),
        disableUntil: { day: 1, month: 1, year: 2013 }, // TODO start with date since the edge is available
        inline: true,
        selectorHeight: "14.063em",
        selectorWidth: "15.688em",
        showWeekNumbers: true,
    };
    protected readonly DefaultTypes = DefaultTypes;
    private readonly TODAY = new Date();

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    public onDateChanged(event: IMyDateRangeModel) {
        this.service.historyPeriod.next(new DefaultTypes.HistoryPeriod(event.beginJsDate, event.endJsDate));
        this.service.periodString = DefaultTypes.PeriodString.CUSTOM;
        this.popoverCtrl.dismiss();
    }

    ngOnInit() {

        this.locale = (Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT).key;
        // Restrict user to pick date before ibn-date
        this.myDpOptions.disableUntil = { day: Utils.subtractSafely(getDate(this.edge?.firstSetupProtocol), 1) ?? 1, month: Utils.addSafely(getMonth(this.edge?.firstSetupProtocol), 1) ?? 1, year: this.edge?.firstSetupProtocol?.getFullYear() ?? 2013 };

        // Filter out custom due to different on click event
        this.periods = EdgePermission.getAllowedHistoryPeriods(this.edge, this.historyPeriods).filter(period => period !== DefaultTypes.PeriodString.CUSTOM);
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
            case DefaultTypes.PeriodString.DAY: {
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.TODAY, this.TODAY));
                this.service.periodString = period;
                this.popoverCtrl.dismiss();
                break;
            }
            case DefaultTypes.PeriodString.WEEK: {
                this.setDateRange(new DefaultTypes.HistoryPeriod(startOfWeek(this.TODAY, { weekStartsOn: 1 }), endOfWeek(this.TODAY, { weekStartsOn: 1 })));
                this.service.periodString = period;
                this.popoverCtrl.dismiss();
                break;
            }
            case DefaultTypes.PeriodString.MONTH: {
                this.setDateRange(new DefaultTypes.HistoryPeriod(startOfMonth(this.TODAY), endOfMonth(this.TODAY)));
                this.service.periodString = period;
                this.popoverCtrl.dismiss();
                break;
            }
            case DefaultTypes.PeriodString.YEAR: {
                this.setDateRange(new DefaultTypes.HistoryPeriod(startOfYear(this.TODAY), endOfYear(this.TODAY)));
                this.service.periodString = period;
                this.popoverCtrl.dismiss();
                break;
            }
            case DefaultTypes.PeriodString.TOTAL: {
                this.setDateRange(new DefaultTypes.HistoryPeriod(this.edge?.firstSetupProtocol, endOfYear(this.TODAY)));
                this.service.periodString = period;
                this.popoverCtrl.dismiss();
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
