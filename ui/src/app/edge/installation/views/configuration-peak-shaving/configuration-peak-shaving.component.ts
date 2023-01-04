import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Category } from '../../shared/category';

@Component({
    selector: ConfigurationPeakShavingComponent.SELECTOR,
    templateUrl: './configuration-peak-shaving.component.html'
})
export class ConfigurationPeakShavingComponent implements OnInit {

    private static readonly SELECTOR = 'configuration-peak-shaving';

    @Input() public ibn: AbstractIbn;
    @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
    @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

    protected form: FormGroup;
    protected fields: FormlyFieldConfig[];
    protected model;
    protected header: string;
    protected showDescription: boolean;

    constructor(private translate: TranslateService) { }

    ngOnInit(): void {
        this.form = new FormGroup({});
        this.fields = this.getFields();
        this.header = Category.toTranslatedString(this.ibn.getPeakShavingHeader(), this.translate);
        this.showDescription = this.header == Category.toTranslatedString(Category.PEAK_SHAVING_ASYMMETRIC_HEADER, this.translate) ? true : false
        this.model = {};
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }

    public onNextClicked() {

        if (this.form.invalid) {
            return;
        }

        this.ibn.setCommercialfeature(this.model);
        this.nextViewEvent.emit(this.ibn);
    }

    public getFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];

        fields.push({
            key: 'entladungÜber',
            type: 'input',
            className: 'overflow-wrapper',
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.DISCHARGE_ABOVE_VALUE'),
                type: 'number',
                description: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.DISCHARGE_ABOVE_DESCRIPTION'),
                required: true,
            },
        });

        fields.push({
            key: 'beladungUnter',
            type: 'input',
            className: 'line-break',
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.CHARGE_BELOW_VALUE'),
                type: 'number',
                description: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.CHARGE_BELOW_DESCRIPTION'),
                required: true,
            },
            expressionProperties: {
                // "beladungUnter" value cannot be greater than "entladungÜber" value
                'templateOptions.max': 'model.entladungÜber'
            }
        });

        return fields;
    }
}
