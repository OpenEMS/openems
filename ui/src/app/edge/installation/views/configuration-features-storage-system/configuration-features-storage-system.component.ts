import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractCommercial50Ibn } from '../../installation-systems/commercial/commercial-50/abstract-commercial-50';
import { Commercial50EigenverbrauchsOptimierung } from '../../installation-systems/commercial/commercial-50/commercial50-eigenverbrauchsoptimierung';
import { Commercial50Lastspitzenkappung } from '../../installation-systems/commercial/commercial-50/commercial50-lastspitzenkappung';
import { Category } from '../../shared/category';

@Component({
    selector: ConfigurationFeaturesStorageSystemComponent.SELECTOR,
    templateUrl: './configuration-features-storage-system.component.html',
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
            { value: Category.BALANCING, label: Category.toTranslatedString(Category.BALANCING, this.translate) },
            { value: Category.PEAK_SHAVING_SYMMETRIC, label: Category.toTranslatedString(Category.PEAK_SHAVING_SYMMETRIC, this.translate) },
            { value: Category.PEAK_SHAVING_ASYMMETRIC, label: Category.toTranslatedString(Category.PEAK_SHAVING_ASYMMETRIC, this.translate) },
        ];

        fields.push({
            key: 'feature',
            type: 'radio',
            templateOptions: {
                options: label,
                description: this.translate.instant('INSTALLATION.CONFIGURATION_FEATURES_STORAGE_SYSTEM.FEATURE_DESCRIPTION'),
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
            case Category.BALANCING:
                this.ibn = new Commercial50EigenverbrauchsOptimierung(this.translate);
                this.ibn.commercial50Feature.feature.type = Category.BALANCING;
                break;
            case Category.PEAK_SHAVING_SYMMETRIC:
                this.ibn = new Commercial50Lastspitzenkappung(this.translate);
                this.ibn.commercial50Feature.feature.type = Category.PEAK_SHAVING_SYMMETRIC;
                break;
            case Category.PEAK_SHAVING_ASYMMETRIC:
                this.ibn = new Commercial50Lastspitzenkappung(this.translate);
                this.ibn.commercial50Feature.feature.type = Category.PEAK_SHAVING_ASYMMETRIC;
                break;
        }
    }
}
