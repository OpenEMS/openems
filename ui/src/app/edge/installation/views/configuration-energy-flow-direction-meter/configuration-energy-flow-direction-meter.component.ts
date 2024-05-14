// @ts-strict-ignore
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { Edge } from "src/app/shared/shared";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Meter } from "../../shared/meter";
import { FormGroup } from "@angular/forms";
import { AbstractHomeIbn } from "../../installation-systems/home/abstract-home";

@Component({
    selector: ConfigurationEnergyFlowMeterComponent.SELECTOR,
    templateUrl: './configuration-energy-flow-direction-meter.component.html',
})
export class ConfigurationEnergyFlowMeterComponent implements OnInit {

    private static readonly SELECTOR = 'configuration-energy-flow-direction-meter';

    constructor(private translate: TranslateService) { }

    @Input() public ibn: AbstractHomeIbn;
    @Input() public edge: Edge;
    @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
    @Output() public nextViewEvent: EventEmitter<any> = new EventEmitter();

    public form: FormGroup;
    public fields: FormlyFieldConfig[];
    protected model;

    public ngOnInit(): void {
        this.form = new FormGroup({});
        this.fields = this.initializeFields();
        this.model = this.ibn.energyFlowMeter ?? {};
    }

    public onNextClicked() {
        if (this.form.invalid) {
            return;
        }

        this.ibn.energyFlowMeter = this.model;
        this.nextViewEvent.emit(this.ibn);
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }

    private initializeFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];
        const options = [
            {
                value: Meter.GridMeterCategory.SMART_METER, //
                label: Meter.toGridMeterCategoryLabelString(Meter.GridMeterCategory.SMART_METER, this.translate),
            },
            {
                value: Meter.GridMeterCategory.COMMERCIAL_METER, //
                label: Meter.toGridMeterCategoryLabelString(Meter.GridMeterCategory.COMMERCIAL_METER, this.translate),
            }];

        fields.push({
            key: 'meter',
            type: 'select',
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.LABEL'),
                options: options,
                required: true,
            },
            defaultValue: Meter.GridMeterCategory.SMART_METER,
        });

        fields.push({
            key: 'value',
            type: 'input',
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.CONVERTER_RATIO'),
                type: 'number',
                min: 200,
                max: 5000,
                required: true,
            },
            expressions: {
                hide: (fields) => fields.model.meter === Meter.GridMeterCategory.SMART_METER,
            },
            validators: {
                validation: ["onlyPositiveInteger"],
            },
        });

        return fields;
    }
}
