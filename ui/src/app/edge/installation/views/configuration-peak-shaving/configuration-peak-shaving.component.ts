import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';

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

    constructor() { }

    ngOnInit(): void {
        this.form = new FormGroup({});
        this.fields = this.getFields();
        this.header = this.ibn.getPeakShavingHeader();
        this.showDescription = this.header == 'Einstellungen Phasengenaue Lastspitzenkappung' ? true : false
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
                label: 'Entladung über - Wert [W]:',
                type: 'number',
                description: 'liegt die Netzbezugsleistung oberhalb von diesem Wert, wird die Batterie entladen.',
                required: true,
            },
        });

        fields.push({
            key: 'beladungUnter',
            type: 'input',
            className: 'line-break',
            templateOptions: {
                label: 'Beladung unter - Wert [W]:',
                type: 'number',
                description: `liegt die Netzbezugsleistung unterhalb von diesem Wert, wird die Batterie wieder beladen.
                Dieser Wert darf max. dem Wert "Entladung über" entsprechen.`,
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
