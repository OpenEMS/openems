import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Commercial50Lastspitzenkappung } from '../../installation-systems/commercial/commercial-50/commercial50-lastspitzenkappung';
import { Category } from '../../shared/category';

@Component({
    selector: ConfigurationPeakShavingComponent.SELECTOR,
    templateUrl: './configuration-peak-shaving.component.html'
})
export class ConfigurationPeakShavingComponent implements OnInit {

    private static readonly SELECTOR = 'configuration-peak-shaving';

    @Input() public ibn: Commercial50Lastspitzenkappung;
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
        this.header = this.getHeader();
        this.showDescription = this.isAsymmetricPeakShaving();
        this.model = {};
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }

    public onNextClicked() {

        if (this.form.invalid) {
            return;
        }

        this.ibn.setNonAbstractFields(this.model);
        this.nextViewEvent.emit(this.ibn);
    }

    public getFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];

        fields.push({
            key: 'dischargeAbove',
            type: 'input',
            className: 'overflow-wrapper',
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.DISCHARGE_ABOVE_VALUE'),
                type: 'number',
                description: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.DISCHARGE_ABOVE_DESCRIPTION'),
                required: true
            }
        });

        fields.push({
            key: 'chargeBelow',
            type: 'input',
            className: 'line-break',
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.CHARGE_BELOW_VALUE'),
                type: 'number',
                description: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.CHARGE_BELOW_DESCRIPTION'),
                required: true
            },
            expressionProperties: {
                // "chargeBelow" value cannot be greater than "dischargeAbove" value
                'templateOptions.max': `parseInt(model.dischargeAbove)`
            }
        });

        return fields;
    }

    private getHeader(): string {
        if (this.ibn.commercial50Feature.feature.type === Category.PEAK_SHAVING_SYMMETRIC) {
            return Category.toTranslatedString(Category.PEAK_SHAVING_SYMMETRIC_HEADER, this.translate);
        } else {
            return Category.toTranslatedString(Category.PEAK_SHAVING_ASYMMETRIC_HEADER, this.translate);
        }
    }

    // Returns true if Asymmetric peakshaving (Phasengenaue Lastspitzenkappung) is selected in commercial features view.
    private isAsymmetricPeakShaving(): boolean {
        return this.ibn.commercial50Feature.feature.type === Category.PEAK_SHAVING_ASYMMETRIC ? true : false;
    }
}
