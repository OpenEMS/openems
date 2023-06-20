import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractIbn } from "../../installation-systems/abstract-ibn";
import { Commercial30AnschlussIbn } from "../../installation-systems/commercial/commercial-30/commercial30-anschluss";
import { Commercial30NetztrennIbn } from "../../installation-systems/commercial/commercial-30/commercial30-netztrenn";

@Component({
    selector: ConfigurationCommercialComponent.SELECTOR,
    templateUrl: './configuration-commercial.component.html'
})
export class ConfigurationCommercialComponent implements OnInit {

    private static readonly SELECTOR = 'configuration-commercial';

    @Input() public ibn: AbstractIbn;
    @Output() public nextViewEvent = new EventEmitter();
    @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

    public form: FormGroup;
    public fields: FormlyFieldConfig[];
    public model;

    constructor(private translate: TranslateService) { }

    public ngOnInit() {
        this.form = new FormGroup({});
        this.fields = this.getFields();
    }

    public onNextClicked() {
        if (this.form.invalid) {
            return;
        }
        // Sets the ibn object.
        this.setIbn();
        this.ibn.showViewCount = true;
        this.nextViewEvent.emit(this.ibn);
    }

    public getFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];
        const componentLabel = ([
            {
                value: "Anschluss", label: `FEMS Anschlussbox
                (Commercial 30 ohne Notstromversorgung)`
            },
            {
                value: "Netzstelle", label: `Netztrennstelle 
            (Commercial 30 mit Notstromversorgung)` }
        ]);

        fields.push({
            key: "component",
            type: "radio",
            className: 'line-break',
            templateOptions: {
                options: componentLabel,
                required: true
            }
        });
        return fields;
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }

    /**
     * Loads the appropriate Ibn object.
     */
    private setIbn() {
        const component = this.form.controls['component'].value;

        if (component === 'Anschluss') {
            this.ibn = new Commercial30AnschlussIbn(this.translate);
        } else {
            this.ibn = new Commercial30NetztrennIbn(this.translate);
        }
    }
}
