import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { COUNTRY_OPTIONS } from 'src/app/shared/type/country';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';

@Component({
  selector: ProtocolSystemComponent.SELECTOR,
  templateUrl: './protocol-system.component.html',
})
export class ProtocolSystemComponent implements OnInit {

  private static readonly SELECTOR = "protocol-system";

  @Input() public ibn: AbstractIbn;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor(private translate: TranslateService) { }

  public ngOnInit() {

    this.form = new FormGroup({});
    this.fields = this.generateFields();
    this.model = this.ibn.location ?? {
      isEqualToCustomerData: false,
      isCorporateClient: false,
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_SYSTEM.LOCATION_SAME_AS_ADDRESS'),
      },
    });

    fields.push({
      key: "isCorporateClient",
      type: "checkbox",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COMPANY_CUSTOMER'),
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      key: "companyName",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COMPANY_NAME'),
        required: true,
      },
      hideExpression: model => model.isEqualToCustomerData || !model.isCorporateClient,
    });

    fields.push({
      key: "lastName",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_SYSTEM.LAST_NAME'),
        required: true,
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      key: "firstName",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_SYSTEM.FIRSTNAME'),
        required: true,
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      key: "street",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.STREET_ADDRESS'),
        required: true,
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      key: "zip",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.ZIP'),
        required: true,
      },
      validators: {
        validation: ["onlyPositiveInteger"],
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      key: "city",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.CITY'),
        required: true,
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      key: "country",
      type: "select",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COUNTRY'),
        required: true,
        options: COUNTRY_OPTIONS(this.translate),
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      fieldGroup: [
        {
          key: "email",
          type: "input",
          templateOptions: {
            type: "email",
            label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL'),
            required: true,
          },
          validators: {
            validation: [Validators.email],
          },
        },
        {
          key: "emailConfirm",
          type: "input",
          templateOptions: {
            type: "email",
            label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL'),
            description: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL_CONFIRMATION'),
            required: true,
          },
        },
      ],
      validators: {
        validation: [
          { name: "emailMatch", options: { errorPath: "emailConfirm" } },
        ],
      },
      hideExpression: model => model.isEqualToCustomerData,
    });

    fields.push({
      key: "phone",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.PHONE'),
        required: true,
      },
      hideExpression: model => model.isEqualToCustomerData,
    });
    return fields;
  }
}
