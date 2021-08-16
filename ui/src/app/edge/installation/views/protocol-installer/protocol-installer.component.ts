import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { GetUserInformationRequest } from 'src/app/shared/jsonrpc/request/getUserInformationRequest';
import { SetUserInformationRequest } from 'src/app/shared/jsonrpc/request/setUserInformationRequest';
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

  public editModeEnabled: boolean;

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

    if (this.editModeEnabled) {
      this.service.toast("Speichern Sie zuerst die Daten, um zur nächsten Ansicht zu gelangen.", "warning");
      return;
    }

    this.installationData.installer = this.model;

    this.nextViewEvent.emit(this.installationData);

  }

  public getFields(): FormlyFieldConfig[] {

    return [{
      hooks: {
        onInit: ({ form }) => {
          // When the form is invalid, edit mode
          // gets enabled, to correct the data
          this.enableEditMode(form.invalid);
        }
      },
      fieldGroup: [
        {
          key: "companyName",
          type: "input",
          templateOptions: {
            label: "Firmenname",
            required: true
          }
        },
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
          key: "street",
          type: "input",
          templateOptions: {
            label: "Straße / Hausnummer",
            required: true

          }
        },
        {
          key: "zip",
          type: "input",
          templateOptions: {
            label: "PLZ",
            required: true
          },
          validators: {
            validation: ["zip"]
          }
        },
        {
          key: "city",
          type: "input",
          templateOptions: {
            label: "Ort",
            required: true
          }
        },
        {
          key: "country",
          type: "select",
          templateOptions: {
            label: "Land",
            required: true,
            options: COUNTRY_OPTIONS
          }
        },
        {
          key: "email",
          type: "input",
          templateOptions: {
            label: "E-Mail",
            required: true
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
            required: true
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

  public setUserInformation(): Promise<JsonrpcResponseSuccess> {

    let user = {
      firstname: this.model.firstName,
      lastname: this.model.lastName,
      email: this.model.email,
      phone: this.model.phone,
      address: {
        street: this.model.street,
        zip: this.model.zip,
        city: this.model.city,
        country: this.model.country
      },
      company: {
        name: this.model.companyName
      }
    }

    return this.service.websocket.sendRequest(new SetUserInformationRequest({ user }));

  }

  public enableEditMode(enable: boolean) {

    if (enable) {

      this.form.enable();
      this.editModeEnabled = true;

    } else {

      if (this.form.invalid) {
        this.service.toast("Geben Sie zuerst gültige Daten ein, um diese zu speichern.", "warning");
        return;
      }

      this.setUserInformation().then(() => {

        this.form.disable();
        this.editModeEnabled = false;

      }).catch(() => {

        // TODO find better solution

        console.warn("Data could not be set.");

        this.form.disable();
        this.editModeEnabled = false;

      });

    }

  }

  public onEditClicked() {

    this.enableEditMode(!this.editModeEnabled);

  }

  public onResetClicked() {

    this.getUserInformation().then((userInformation) => {

      this.model = userInformation;

    });

  }

}