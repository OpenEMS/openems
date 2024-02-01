import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { RegisterUserRequest } from 'src/app/shared/jsonrpc/request/registerUserRequest';
import { Service, Websocket } from 'src/app/shared/shared';
import { COUNTRY_OPTIONS } from 'src/app/shared/type/country';
import { environment } from 'src/environments';

@Component({
  selector: 'registration-modal',
  templateUrl: './modal.component.html',
})
export class RegistrationModalComponent implements OnInit {

  protected formGroup: FormGroup;
  protected activeSegment: string = "installer";
  protected readonly countries = COUNTRY_OPTIONS(this.translate);

  constructor(
    private formBuilder: FormBuilder,
    public modalCtrl: ModalController,
    private translate: TranslateService,
    private service: Service,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    this.formGroup = this.getForm(this.activeSegment);
  }

  /**
   * Update the form depending on the thrown event (ionChange) value.
   *
   * @param event to get current value and change the form
   */
  updateRegistrationForm(event: CustomEvent) {
    this.formGroup = this.getForm(event.detail.value);
  }

  /**
   * Validate the current form and sends the registration request.
   */
  onSubmit() {
    if (!this.formGroup.valid) {
      this.service.toast(this.translate.instant("Register.errors.requiredFields"), 'danger');
      return;
    }

    let password = this.formGroup.value.password;
    let confirmPassword = this.formGroup.value.confirmPassword;

    if (password != confirmPassword) {
      this.service.toast(this.translate.instant("Register.errors.passwordNotEqual"), 'danger');
      return;
    }

    let email = this.formGroup.value.email;
    let confirmEmail = this.formGroup.value.confirmEmail;

    if (email != confirmEmail) {
      this.service.toast(this.translate.instant("Register.errors.emailNotEqual"), 'danger');
      return;
    }

    let request = new RegisterUserRequest({
      user: {
        firstname: this.formGroup.value.firstname,
        lastname: this.formGroup.value.lastname,
        phone: this.formGroup.value.phone,
        email: this.formGroup.value.email,
        password: password,
        confirmPassword: confirmPassword,
        address: {
          street: this.formGroup.value.street,
          zip: this.formGroup.value.zip,
          city: this.formGroup.value.city,
          country: this.formGroup.value.country,
        },
        role: this.activeSegment,
      },
      oem: environment.theme,
    });

    let companyName = this.formGroup.value.companyName;
    if (companyName) {
      request.params.user.company = {
        name: companyName,
      };
    }

    this.websocket.sendRequest(request)
      .then(() => {
        this.service.toast(this.translate.instant("Register.success"), 'success');
        this.modalCtrl.dismiss();
      })
      .catch(reason => {
        this.service.toast(reason.error.message, 'danger');
      });
  }

  /**
   * Get from depending on given role.
   * If no role matches then the default (owner) from will be returnd.
   */
  private getForm(role: string): FormGroup {
    if (role === 'installer') {
      return this.formBuilder.group({
        companyName: new FormControl("", Validators.required),
        firstname: new FormControl("", Validators.required),
        lastname: new FormControl("", Validators.required),
        street: new FormControl("", Validators.required),
        zip: new FormControl("", Validators.required),
        city: new FormControl("", Validators.required),
        country: new FormControl("", Validators.required),
        phone: new FormControl("", Validators.required),
        email: new FormControl("", [Validators.required, Validators.email]),
        confirmEmail: new FormControl("", [Validators.required, Validators.email]),
        password: new FormControl("", Validators.required),
        confirmPassword: new FormControl("", Validators.required),
      });
    } else {
      return this.formBuilder.group({
        firstname: new FormControl("", Validators.required),
        lastname: new FormControl("", Validators.required),
        street: new FormControl("", Validators.required),
        zip: new FormControl("", Validators.required),
        city: new FormControl("", Validators.required),
        country: new FormControl("", Validators.required),
        phone: new FormControl("", Validators.required),
        email: new FormControl("", [Validators.required, Validators.email]),
        confirmEmail: new FormControl("", [Validators.required, Validators.email]),
        password: new FormControl("", Validators.required),
        confirmPassword: new FormControl("", Validators.required),
      });
    }
  }

}
