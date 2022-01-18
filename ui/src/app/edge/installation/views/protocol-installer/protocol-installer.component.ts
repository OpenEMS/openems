import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { GetUserInformationRequest } from 'src/app/shared/jsonrpc/request/getUserInformationRequest';
import { GetUserInformationResponse } from 'src/app/shared/jsonrpc/response/getUserInformationResponse';
import { Service } from 'src/app/shared/shared';
import { COUNTRY_OPTIONS, InstallationData } from '../../installation.component';

type UserInformation = {
  companyName: string,
  lastName: string,
  firstName: string,
  street: string,
  zip: string,
  city: string,
  country: string,
  email: string,
  phone: string
}

@Component({
  selector: ProtocolInstallerComponent.SELECTOR,
  templateUrl: './protocol-installer.component.html'
})
export class ProtocolInstallerComponent implements OnInit {

  private static readonly SELECTOR = "protocol-installer";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  public spinnerId: string;

  constructor(private service: Service) { }

  public ngOnInit() {

    this.spinnerId = ProtocolInstallerComponent.SELECTOR + "-spinner";

    this.service.startSpinner(this.spinnerId);

    this.getUserInformation().then((userInformation) => {

      this.service.stopSpinner(this.spinnerId);

      this.form = new FormGroup({});
      this.fields = this.getFields();
      this.model = userInformation;
    });
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.form.invalid) {
      return;
    }

    this.installationData.installer = this.model;

    this.nextViewEvent.emit(this.installationData);
  }

  public getFields(): FormlyFieldConfig[] {

    return [{
      fieldGroup: [
        {
          key: "lastName",
          type: "input",
          templateOptions: {
            label: "Nachname",
            required: true
          }
        },
        {
          key: "firstName",
          type: "input",
          templateOptions: {
            label: "Vorname",
            required: true
          }
        },
        {
          key: "companyName",
          type: "input",
          templateOptions: {
            label: "Firmenname",
            disabled: true
          }
        },
        {
          key: "street",
          type: "input",
          templateOptions: {
            label: "Straße / Hausnummer",
            disabled: true
          }
        },
        {
          key: "zip",
          type: "input",
          templateOptions: {
            label: "PLZ",
            disabled: true
          }
        },
        {
          key: "city",
          type: "input",
          templateOptions: {
            label: "Ort",
            disabled: true
          }
        },
        {
          key: "country",
          type: "select",
          templateOptions: {
            label: "Land",
            options: COUNTRY_OPTIONS,
            disabled: true
          }
        },
        {
          key: "email",
          type: "input",
          templateOptions: {
            label: "E-Mail",
            disabled: true
          },
          validators: {
            validation: [Validators.email]
          }
        },
        {
          key: "phone",
          type: "input",
          templateOptions: {
            label: "Telefonnummer",
            disabled: true
          }
        }
      ]
    }];

  }

  public getUserInformation(): Promise<UserInformation> {

    return new Promise((resolve) => {

      this.service.websocket.sendRequest(new GetUserInformationRequest()).then((response: GetUserInformationResponse) => {

        let user = response.result.user;

        resolve({
          companyName: user.company.name,
          lastName: user.lastname,
          firstName: user.firstname,
          street: user.address.street,
          zip: user.address.zip,
          city: user.address.city,
          country: user.address.country,
          email: user.email,
          phone: user.phone
        });

      }).catch(() => {

        resolve({
          companyName: "",
          lastName: "",
          firstName: "",
          street: "",
          zip: "",
          city: "",
          country: "",
          email: "",
          phone: ""
        });

      });

    });

  }

}