import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { COUNTRY_OPTIONS, InstallationData } from '../../installation.component';

@Component({
  selector: ProtocolSystemComponent.SELECTOR,
  templateUrl: './protocol-system.component.html'
})
export class ProtocolSystemComponent implements OnInit {

  private static readonly SELECTOR = "protocol-system";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor() { }

  public ngOnInit() {

    this.form = new FormGroup({});
    this.fields = this.generateFields();
    this.model = this.installationData.location ?? {};

  }

  public onPreviousClicked() {

    this.previousViewEvent.emit();

  }

  public onNextClicked() {

    if (this.form.invalid) {
      return;
    }

    this.installationData.location = this.model;

    this.nextViewEvent.emit(this.installationData);

  }

  public generateFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "isEqualToCustomerData",
      type: "checkbox",
      templateOptions: {
        label: "Entspricht der Speicherstandort der Kundenadresse?",
        required: true
      }
    });

    fields.push({
      key: "isCorporateClient",
      type: "checkbox",
      templateOptions: {
        label: "Firmenkunde?",
        required: true
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
        validation: ["zip"]
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