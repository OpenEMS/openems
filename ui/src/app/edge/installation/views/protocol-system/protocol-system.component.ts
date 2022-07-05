import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';

import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { COUNTRY_OPTIONS } from '../../installation.component';

@Component({
  selector: ProtocolSystemComponent.SELECTOR,
  templateUrl: './protocol-system.component.html'
})
export class ProtocolSystemComponent implements OnInit {

  private static readonly SELECTOR = "protocol-system";

  @Input() public ibn: AbstractIbn;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor() { }

  public ngOnInit() {

    this.form = new FormGroup({});
    this.fields = this.generateFields();
    this.model = this.ibn.location ?? {
      isEqualToCustomerData: false,
      isCorporateClient: false
    };

  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    if (this.form.invalid) {
      return;
    }
    this.ibn.location = this.model;
    this.nextViewEvent.emit(this.ibn);
  }

  public generateFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "isEqualToCustomerData",
      type: "checkbox",
      templateOptions: {
        label: "Entspricht der Speicherstandort der Kundenadresse?",
      }
    });

    fields.push({
      key: "isCorporateClient",
      type: "checkbox",
      templateOptions: {
        label: "Firmenkunde?",
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      key: "companyName",
      type: "input",
      templateOptions: {
        label: "Firmenname",
        required: true
      },
      hideExpression: model => model.isEqualToCustomerData || !model.isCorporateClient
    });

    fields.push({
      key: "lastName",
      type: "input",
      templateOptions: {
        label: "Nachname Kontaktperson",
        required: true
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      key: "firstName",
      type: "input",
      templateOptions: {
        label: "Vorname Kontaktperson",
        required: true
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      key: "street",
      type: "input",
      templateOptions: {
        label: "Straße und Hausnummer",
        required: true
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      key: "zip",
      type: "input",
      templateOptions: {
        label: "PLZ",
        required: true
      },
      validators: {
        validation: ["onlyPositiveInteger"]
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      key: "city",
      type: "input",
      templateOptions: {
        label: "Ort",
        required: true
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      key: "country",
      type: "select",
      templateOptions: {
        label: "Land",
        required: true,
        options: COUNTRY_OPTIONS
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      fieldGroup: [
        {
          key: "email",
          type: "input",
          templateOptions: {
            type: "email",
            label: "E-Mail-Adresse",
            required: true
          },
          validators: {
            validation: [Validators.email]
          }
        },
        {
          key: "emailConfirm",
          type: "input",
          templateOptions: {
            type: "email",
            label: "E-Mail-Adresse",
            description: "Wiederholen",
            required: true
          }
        }
      ],
      validators: {
        validation: [
          { name: "emailMatch", options: { errorPath: "emailConfirm" } },
        ],
      },
      hideExpression: model => model.isEqualToCustomerData
    });

    fields.push({
      key: "phone",
      type: "input",
      templateOptions: {
        label: "Telefonnummer",
        required: true
      },
      hideExpression: model => model.isEqualToCustomerData
    });
    return fields;
  }
}
