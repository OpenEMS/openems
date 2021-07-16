import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { RegisterUserRequest } from 'src/app/shared/jsonrpc/request/registerUserRequest';
import { Service, Websocket } from 'src/app/shared/shared';

@Component({
  selector: 'registration-modal',
  templateUrl: './modal.component.html'
})
export class RegistrationModalComponent implements OnInit {

  public formGroup: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    public modalCtrl: ModalController,
    private translate: TranslateService,
    private service: Service,
    private websocket: Websocket
  ) { }

  ngOnInit() {
    this.formGroup = this.formBuilder.group({
      companyName: new FormControl("", Validators.required),
      firstname: new FormControl("", Validators.required),
      lastname: new FormControl("", Validators.required),
      street: new FormControl("", Validators.required),
      zip: new FormControl("", Validators.required),
      city: new FormControl("", Validators.required),
      country: new FormControl("", Validators.required),
      phone: new FormControl("", Validators.required),
      email: new FormControl("", Validators.email),
      password: new FormControl("", Validators.required),
      confirmPassword: new FormControl("", Validators.required),
      isElectrician: new FormControl(false, Validators.requiredTrue),
      acceptPrivacyPolicy: new FormControl(false, Validators.requiredTrue),
      acceptAgb: new FormControl(false, Validators.requiredTrue),
      subscribeNewsletter: new FormControl()
    });
  }

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
          country: this.formGroup.value.country
        },
        company: {
          name: this.formGroup.value.companyName
        },
        subscribeNewsletter: this.formGroup.value.subscribeNewsletter
      }
    });

    this.websocket.sendRequest(request)
      .then(res => {
        this.service.toast(this.translate.instant("Register.success"), 'success');
        this.modalCtrl.dismiss();
      })
      .catch(reason => {
        this.service.toast(reason.error.message, 'danger');
      });
  }

}
