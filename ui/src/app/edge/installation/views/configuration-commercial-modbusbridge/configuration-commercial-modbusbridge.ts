import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { ModbusBridgeType } from "../../shared/enums";
import { AbstractCommercialIbn } from "../../installation-systems/commercial/abstract-commercial";

@Component({
    selector: ConfigurationCommercialModbuBridgeComponent.SELECTOR,
    templateUrl: './configuration-commercial-modbusbridge.html',
})
export class ConfigurationCommercialModbuBridgeComponent implements OnInit {

    private static readonly SELECTOR = 'configuration-commercial-modbusbridge';

    @Input() public ibn: AbstractCommercialIbn;
    @Output() public nextViewEvent = new EventEmitter();
    @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

    public form: FormGroup;
    public fields: FormlyFieldConfig[];
    public model;

    constructor(private translate: TranslateService) { }

    public ngOnInit() {
        this.form = new FormGroup({});
        this.fields = this.getFields();
        this.model = { ...(this.ibn.modbusBridgeType && { "modbus": this.ibn.modbusBridgeType }) };
    }

    public onNextClicked() {
        if (this.form.invalid) {
            return;
        }

        // set the modbus bridge type in ibn
        this.ibn.modbusBridgeType = this.model['modbus'];

        // Sets the ibn object.
        this.nextViewEvent.emit(this.ibn);
    }

    public getFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];
        const modbusBridgeLabel = ([
            {
                value: ModbusBridgeType.TCP_IP,
                label: this.translate.instant('INSTALLATION.CONFIGURATION_COMMERCIAL_MODBUS_COMPONENT.TCP_IP_LABEL'),
                url: 'assets/img/TCP-IP_6_Ports.png',
            },
            {
                value: ModbusBridgeType.RS485,
                label: this.translate.instant('INSTALLATION.CONFIGURATION_COMMERCIAL_MODBUS_COMPONENT.RS485_LABEL'),
                url: 'assets/img/RTU-4-Ports.png',
            },
        ]);

        fields.push({
            key: "modbus",
            type: "radio",
            props: {
                required: true,
                options: modbusBridgeLabel,
            },
            defaultValue: ModbusBridgeType.TCP_IP,
            wrappers: ['formly-field-radio-with-image'],
        });
        return fields;
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }
}
