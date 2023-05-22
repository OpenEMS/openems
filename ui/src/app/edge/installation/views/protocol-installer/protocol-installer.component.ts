import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { GetUserInformationRequest } from 'src/app/shared/jsonrpc/request/getUserInformationRequest';
import { GetUserInformationResponse } from 'src/app/shared/jsonrpc/response/getUserInformationResponse';
import { Service } from 'src/app/shared/shared';
import { COUNTRY_OPTIONS } from 'src/app/shared/type/country';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';

type UserInformation = {
  companyName: string;
  lastName: string;
  firstName: string;
  street: string;
  zip: string;
  city: string;
  country: string;
  email: string;
  phone: string;
};

@Component({
  selector: ProtocolInstallerComponent.SELECTOR,
  templateUrl: './protocol-installer.component.html',
})
export class ProtocolInstallerComponent implements OnInit {
  private static readonly SELECTOR = 'protocol-installer';

  @Input() public ibn: AbstractIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;
  public spinnerId: string;

  constructor(
    private service: Service,
    private translate: TranslateService
  ) { }

  public ngOnInit() {
    this.spinnerId = ProtocolInstallerComponent.SELECTOR + '-spinner';
    this.service.startSpinner(this.spinnerId);
    this.getUserInformation().then((userInformation) => {
      this.service.stopSpinner(this.spinnerId);
      this.form = new FormGroup({});
      this.fields = this.getFields();
      this.model = userInformation;
    });
  }

  public onPreviousClicked() {
    this.ibn.showViewCount = false;
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.form.invalid) {
      return;
    }

    this.ibn.installer = this.model;
    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): FormlyFieldConfig[] {
    return [
      {
        fieldGroup: [
          {
            key: 'lastName',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.LAST_NAME'),
              required: true,
            },
          },
          {
            key: 'firstName',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.FIRST_NAME'),
              required: true,
            },
          },
          {
            key: 'companyName',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COMPANY_NAME'),
              disabled: true,
            },
          },
          {
            key: 'street',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.STREET_ADDRESS'),
              disabled: true,
            },
          },
          {
            key: 'zip',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.ZIP'),
              disabled: true,
            },
          },
          {
            key: 'city',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.CITY'),
              disabled: true,
            },
          },
          {
            key: 'country',
            type: 'select',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COUNTRY'),
              options: COUNTRY_OPTIONS(this.translate),
              disabled: true,
            },
          },
          {
            key: 'email',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.EMAIL'),
              disabled: true,
            },
            validators: {
              validation: [Validators.email],
            },
          },
          {
            key: 'phone',
            type: 'input',
            templateOptions: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.PHONE'),
              disabled: true,
            },
          },
        ],
      },
    ];
  }

  public getUserInformation(): Promise<UserInformation> {
    return new Promise((resolve) => {
      this.service.websocket
        .sendRequest(new GetUserInformationRequest())
        .then((response: GetUserInformationResponse) => {
          const user = response.result.user;

          resolve({
            companyName: user.company.name,
            lastName: user.lastname,
            firstName: user.firstname,
            street: user.address.street,
            zip: user.address.zip,
            city: user.address.city,
            country: user.address.country,
            email: user.email,
            phone: user.phone,
          });
        })
        .catch(() => {
          resolve({
            companyName: '',
            lastName: '',
            firstName: '',
            street: '',
            zip: '',
            city: '',
            country: '',
            email: '',
            phone: '',
          });
        });
    });
  }
}
