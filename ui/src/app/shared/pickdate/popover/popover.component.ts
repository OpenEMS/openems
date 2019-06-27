import { Component } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { addDays, getDate, getMonth, getYear, format } from 'date-fns/esm';
import { IMyDrpOptions, IMyDate, IMyDateRangeModel } from 'mydaterangepicker';
import { Service } from '../../shared';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../service/defaulttypes';

@Component({
    selector: 'pickdatepopover',
    templateUrl: './popover.component.html'
})
export class PickDatePopoverComponent {

    public readonly TOMORROW = addDays(new Date(), 1);

    private selectedPeriod: DefaultTypes.HistoryPeriod;

    //DateRangePicker Options
    public dateRangePickerOptions: IMyDrpOptions = {
        inline: true,
        showClearBtn: false,
        showApplyBtn: false,
        dateFormat: 'dd.mm.yyyy',
        disableUntil: { day: 1, month: 1, year: 2013 }, // TODO start with date since the edge is available
        disableSince: this.toIMyDate(this.TOMORROW),
        showWeekNumbers: true,
        showClearDateRangeBtn: false,
        editableDateRangeField: false,
        openSelectorOnInputClick: true,
    };

    constructor(
        public service: Service,
        public popoverCtrl: PopoverController,
        private translate: TranslateService
    ) { }

    ngOnInit() {
        this.selectedPeriod = this.service.historyPeriod;
    }

    ngOnDestroy() { }

    dismiss() {
        this.popoverCtrl.dismiss(this.selectedPeriod);
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

    public onDateRangeChanged(event: IMyDateRangeModel) {
        this.selectedPeriod = new DefaultTypes.HistoryPeriod(event.beginJsDate, event.endJsDate);
        this.dismiss()
    }
}
