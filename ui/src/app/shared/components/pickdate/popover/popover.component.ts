// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { CalAnimation, IAngularMyDpOptions, IMyDate, IMyDateRangeModel } from "@nodro7/angular-mydatepicker";
import { endOfMonth, startOfMonth } from "date-fns";
import { addDays, endOfWeek, endOfYear, getDate, getMonth, getYear, startOfWeek, startOfYear } from "date-fns/esm";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { EdgePermission, Service, Utils } from "src/app/shared/shared";

import { Edge } from "../../edge/edge";

@Component({
    selector: "pickdatepopover",
    templateUrl: "./popover.component.html",
})
export class PickDatePopoverComponent implements OnInit {

    @Input() public setDateRange: (period: DefaultTypes.HistoryPeriod) => void;
    @Input() public edge: Edge | null = null;
    @Input() public historyPeriods: DefaultTypes.PeriodStringValues[] = [];

    public locale: string = "de";
    public showCustomDate: boolean = false;

    protected periods: string[] = [];
    protected readonly TOMORROW = addDays(new Date(), 1);
    protected myDpOptions: IAngularMyDpOptions = {
        stylesData: {
            selector: "dp1",
            styles: `
            .dp1 .myDpIconLeftArrow, 
            .dp1 .myDpIconRightArrow,
            .dp1 .myDpHeaderBtn {
                color: #004d8c;
            }
            .dp1 .myDpHeaderBtn:focus,
            .dp1 .myDpMonthLabel:focus,
            .dp1 .myDpYearLabel:focus {
                color: #2d8fab;
             }
            .dp1 .myDpDaycell:focus,
            .dp1 .myDpMonthcell:focus,
            .dp1 .myDpYearcell:focus {
                box-shadow: inset 0 0 0 1px #66afe9;
            }
            .dp1 .myDpSelectedDay,
            .dp1 .myDpSelectedMonth,
            .dp1 .myDpSelectedYear {
                background-color: #93c47d;
                }
            .dp1 .myDpTableSingleDay:hover, 
            .dp1 .myDpTableSingleMonth:hover, 
            .dp1 .myDpTableSingleYear:hover {
                background-color: #add8e6;
                color: #3855c1;
                }
            .dp1 .myDpMarkCurrDay, 
            .dp1 .myDpMarkCurrMonth, 
            .dp1 .myDpMarkCurrYear {
                border-bottom: 2px solid #2d8fab;
                color: #2d8fab;
             }
            .dp1 .myDpRangeColor {
            background-color: #dbeaff;
            }

             `,
        },
        calendarAnimation: { in: CalAnimation.FlipDiagonal, out: CalAnimation.ScaleCenter },
        dateFormat: "dd.mm.yyyy",
        dateRange: true,
        disableSince: this.toIMyDate(this.TOMORROW),
        disableUntil: { day: 1, month: 1, year: 2013 }, // TODO start with date since the edge is available
        inline: true,
        selectorHeight: "225px",
        selectorWidth: "251px",
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
        this.service.historyPeriod.next(new DefaultTypes.HistoryPeriod(event.beginJsDate, event.endJsDate));
        this.service.periodString = DefaultTypes.PeriodString.CUSTOM;
        this.popoverCtrl.dismiss();
    }

    ngOnInit() {
        // Restrict user to pick date before ibn-date
        this.myDpOptions.disableUntil = { day: Utils.subtractSafely(getDate(this.edge?.firstSetupProtocol), 1) ?? 1, month: Utils.addSafely(getMonth(this.edge?.firstSetupProtocol), 1) ?? 1, year: this.edge?.firstSetupProtocol?.getFullYear() ?? 2013 },
            this.locale = this.translate.getBrowserLang();

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
