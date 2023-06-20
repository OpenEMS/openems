import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Service } from 'src/app/shared/shared';
import { COUNTRY_OPTIONS } from 'src/app/shared/type/country';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';

@Component({
  selector: ProtocolCustomerComponent.SELECTOR,
  templateUrl: './protocol-customer.component.html'
})
export class ProtocolCustomerComponent implements OnInit {

  private static readonly SELECTOR = 'protocol-customer';

  @Input() public ibn: AbstractIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor(
    private translate: TranslateService,
    private service: Service) { }


  public ngOnInit() {
    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = this.ibn.customer ?? {
      isCorporateClient: false
    };
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.form.invalid) {
      this.service.toast(this.translate.instant('Edge.Network.mandatoryFields'), 'danger');
      return;
    }
    this.ibn.customer = this.model;
    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): FormlyFieldConfig[] {

    const fields: FormlyFieldConfig[] = [];
    fields.push({
      key: 'isCorporateClient',
      type: 'checkbox',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COMPANY_CUSTOMER')
      }
    });

    fields.push({
      key: 'companyName',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COMPANY_NAME'),
        required: true
      },
      hideExpression: model => !model.isCorporateClient
    });

    fields.push({
      key: 'lastName',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.LAST_NAME'),
        required: true
      }
    });

    fields.push({
      key: 'firstName',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.FIRST_NAME'),
        required: true
      }
    });

    fields.push({
      key: 'street',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.STREET_ADDRESS'),
        required: true
      }
    });

    fields.push({
      key: 'zip',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.ZIP'),
        required: true,
        pattern: '^[0-9]*$'
      }
    });

    fields.push({
      key: 'city',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.CITY'),
        required: true
      }
    });

    fields.push({
      key: 'country',
      type: 'select',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COUNTRY'),
        required: true,
        options: COUNTRY_OPTIONS(this.translate)
      }
    });

    fields.push({
      fieldGroup: [
        {
          key: 'email',
          type: 'input',
          templateOptions: {
            type: 'email',
            label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL'),
            description: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL_DESCRIPTION'),
            required: true
          },
          validators: {
            validation: [Validators.email]
          }
        },
        {
          key: 'emailConfirm',
          type: 'input',
          templateOptions: {
            type: 'email',
            label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL'),
            description: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL_CONFIRMATION'),
            required: true
          }
        }
      ],
      validators: {
        validation: [
          { name: 'emailMatch', options: { errorPath: 'emailConfirm' } }
        ]
      }
    });

    fields.push({
      key: 'phone',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.PHONE'),
        required: true
      }
    });
    return fields;
  }
}
