// @ts-strict-ignore
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Edge } from "src/app/shared/shared";
import { Commercial50Gen3Ibn } from "../../installation-systems/commercial/commercial-50/commercial-50-gen3";

@Component({
    selector: ConfigurationStsBoxComponent.SELECTOR,
    templateUrl: "./configuration-sts-box.component.html",
    standalone: false,
})
export class ConfigurationStsBoxComponent implements OnInit {
    private static readonly SELECTOR = "configuration-sts-box";

    @Input() public ibn: Commercial50Gen3Ibn;
    @Input() public edge: Edge;
    @Output() public nextViewEvent = new EventEmitter();
    @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

    protected form: FormGroup;
    protected fields: FormlyFieldConfig[];
    protected model;

    constructor(private translate: TranslateService) { }

    ngOnInit(): void {
        this.form = new FormGroup({});
        this.fields = this.getStsBoxFields();
        this.model = this.ibn.emergencyReserve;
    }

    public onNextClicked() {
        if (this.form.invalid) {
            return;
        }

        if (this.model.isStsBoxAvailable && !this.model.stsBoxSerialNumber) {
            return;
        }

        this.ibn.emergencyReserve = this.model;
        this.nextViewEvent.emit(this.ibn);
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }

    public getStsBoxFields() {
        const fields: FormlyFieldConfig[] = [];

        fields.push({
            key: "isStsBoxAvailable",
            type: "checkbox",
            props: {
                label: this.translate.instant("INSTALLATION.CONFIGURATION_STS_BOX.STS_BOX_AVAILABLE"),
                url: "assets/img/commercial/sts-box.png",
                imagePositionToLeft: true,

                // Configuration for the nested serial number input
                serialNumberField: {
                    key: "stsBoxSerialNumber",
                    label: this.translate.instant("INSTALLATION.PROTOCOL_AVU_BOX.SERIAL_NUMBER"),
                    placeholder: "xxxxxxxxxx",
                    required: true,
                    helpText: this.translate.instant("INSTALLATION.CONFIGURATION_STS_BOX.HELP_TEXT"),
                },
            },
            wrappers: ["formly-field-checkbox-with-image"],
            validators: {
                validation: ["serialNumberRequiredWhenChecked"],
            },
        });

        fields.push(...this.ibn.getEmergencyReserveFields());
        return fields;
    }

}
