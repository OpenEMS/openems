import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractCommercial50Ibn } from '../../installation-systems/commercial/commercial-50/abstract-commercial-50';
import { Commercial50EigenverbrauchsOptimierung } from '../../installation-systems/commercial/commercial-50/commercial50-eigenverbrauchsoptimierung';
import { Commercial50Lastspitzenkappung } from '../../installation-systems/commercial/commercial-50/commercial50-lastspitzenkappung';

@Component({
    selector: ConfigurationFeaturesStorageSystemComponent.SELECTOR,
    templateUrl: './configuration-features-storage-system.component.html'
})
export class ConfigurationFeaturesStorageSystemComponent implements OnInit {

    private static readonly SELECTOR = 'configuration-features-storage-system';

    @Input() public ibn: AbstractCommercial50Ibn;
    @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
    @Output() public nextViewEvent = new EventEmitter<AbstractCommercial50Ibn>();

    protected form: FormGroup;
    protected fields: FormlyFieldConfig[];
    protected model;

    constructor(private translate: TranslateService) { }

    public ngOnInit(): void {
        this.form = new FormGroup({});
        this.fields = this.getFields();
        this.model = {};
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }

    public onNextClicked() {

        if (this.form.invalid) {
            return;
        }
        this.setIbn();
        this.ibn.showViewCount = true;
        this.nextViewEvent.emit(this.ibn);
    }

    public getFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];

        const label = [
            { value: 'Eigenverbrauchsoptimierung', label: 'Eigenverbrauchsoptimierung' },
            { value: 'Lastspitzenkappung', label: 'Lastspitzenkappung' },
            { value: 'PhasengenaueLastspitzenkappung', label: 'Phasengenaue Lastspitzenkappung' }
        ];

        fields.push({
            key: 'feature',
            type: 'radio',
            templateOptions: {
                options: label,
                description: 'Die entsprechende App muss vorhanden sein',
                required: true,
            },
        });
        return fields;
    }

    /**
     * Loads the appropriate Ibn object.
     */
    private setIbn() {
        const system = this.form.controls['feature'].value;

        switch (system) {
            case 'Eigenverbrauchsoptimierung':
                this.ibn = new Commercial50EigenverbrauchsOptimierung(this.translate);
                this.ibn.commercial50Feature.feature.type = 'Eigenverbrauchsoptimierung';
                break;
            case 'Lastspitzenkappung':
                this.ibn = new Commercial50Lastspitzenkappung(this.translate);
                this.ibn.commercial50Feature.feature.type = 'Lastspitzenkappung';
                break;
            case 'PhasengenaueLastspitzenkappung':
                this.ibn = new Commercial50Lastspitzenkappung(this.translate);
                this.ibn.commercial50Feature.feature.type = 'PhasengenaueLastspitzenkappung';
                break;
        }
    }
}
