// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { RegisterUserRequest } from "src/app/shared/jsonrpc/request/registerUserRequest";
import { Service, Websocket } from "src/app/shared/shared";
import { COUNTRY_OPTIONS } from "src/app/shared/type/country";
import { environment } from "src/environments";

@Component({
  selector: "registration-modal",
  templateUrl: "./MODAL.COMPONENT.HTML",
  standalone: false,
})
export class RegistrationModalComponent implements OnInit {

  protected formGroup: FormGroup;
  protected activeSegment: string = "installer";
  protected readonly countries = COUNTRY_OPTIONS(THIS.TRANSLATE);
  protected docsLink: string | null = null;

  constructor(
    private formBuilder: FormBuilder,
    public modalCtrl: ModalController,
    private translate: TranslateService,
    private service: Service,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    THIS.FORM_GROUP = THIS.GET_FORM(THIS.ACTIVE_SEGMENT);
    THIS.DOCS_LINK = THIS.CREATE_EVCS_DOCS_LINK();
  }

  /**
   * Update the form depending on the thrown event (ionChange) value.
   *
   * @param event to get current value and change the form
   */
  updateRegistrationForm(event: CustomEvent) {
    THIS.FORM_GROUP = THIS.GET_FORM(EVENT.DETAIL.VALUE);
  }

  /**
   * Validate the current form and sends the registration request.
   */
  onSubmit() {
    if (!THIS.FORM_GROUP.VALID) {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("REGISTER.ERRORS.REQUIRED_FIELDS"), "danger");
      return;
    }

    const password = THIS.FORM_GROUP.VALUE.PASSWORD;
    const confirmPassword = THIS.FORM_GROUP.VALUE.CONFIRM_PASSWORD;

    if (password != confirmPassword) {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("REGISTER.ERRORS.PASSWORD_NOT_EQUAL"), "danger");
      return;
    }

    const email = THIS.FORM_GROUP.VALUE.EMAIL;
    const confirmEmail = THIS.FORM_GROUP.VALUE.CONFIRM_EMAIL;

    if (email != confirmEmail) {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("REGISTER.ERRORS.EMAIL_NOT_EQUAL"), "danger");
      return;
    }

    const request = new RegisterUserRequest({
      user: {
        firstname: THIS.FORM_GROUP.VALUE.FIRSTNAME,
        lastname: THIS.FORM_GROUP.VALUE.LASTNAME,
        phone: THIS.FORM_GROUP.VALUE.PHONE,
        email: THIS.FORM_GROUP.VALUE.EMAIL,
        password: password,
        confirmPassword: confirmPassword,
        address: {
          street: THIS.FORM_GROUP.VALUE.STREET,
          zip: THIS.FORM_GROUP.VALUE.ZIP,
          city: THIS.FORM_GROUP.VALUE.CITY,
          country: THIS.FORM_GROUP.VALUE.COUNTRY,
        },
        role: THIS.ACTIVE_SEGMENT,
      },
      oem: ENVIRONMENT.THEME,
    });

    const companyName = THIS.FORM_GROUP.VALUE.COMPANY_NAME;
    if (companyName) {
      REQUEST.PARAMS.USER.COMPANY = {
        name: companyName,
      };
    }

    THIS.WEBSOCKET.SEND_REQUEST(request)
      .then(() => {
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("REGISTER.SUCCESS"), "success");
        THIS.MODAL_CTRL.DISMISS();
      })
      .catch(reason => {
        THIS.SERVICE.TOAST(REASON.ERROR.MESSAGE, "danger");
      });
  }

  /**
   * Get from depending on given role.
   * If no role matches then the default (owner) from will be returnd.
   */
  private getForm(role: string): FormGroup {
    if (role === "installer") {
      return THIS.FORM_BUILDER.GROUP({
        companyName: new FormControl("", VALIDATORS.REQUIRED),
        firstname: new FormControl("", VALIDATORS.REQUIRED),
        lastname: new FormControl("", VALIDATORS.REQUIRED),
        street: new FormControl("", VALIDATORS.REQUIRED),
        zip: new FormControl("", VALIDATORS.REQUIRED),
        city: new FormControl("", VALIDATORS.REQUIRED),
        country: new FormControl("", VALIDATORS.REQUIRED),
        phone: new FormControl("", VALIDATORS.REQUIRED),
        email: new FormControl("", [VALIDATORS.REQUIRED, VALIDATORS.EMAIL]),
        confirmEmail: new FormControl("", [VALIDATORS.REQUIRED, VALIDATORS.EMAIL]),
        password: new FormControl("", VALIDATORS.REQUIRED),
        confirmPassword: new FormControl("", VALIDATORS.REQUIRED),
      });
    } else {
      return THIS.FORM_BUILDER.GROUP({
        firstname: new FormControl("", VALIDATORS.REQUIRED),
        lastname: new FormControl("", VALIDATORS.REQUIRED),
        street: new FormControl("", VALIDATORS.REQUIRED),
        zip: new FormControl("", VALIDATORS.REQUIRED),
        city: new FormControl("", VALIDATORS.REQUIRED),
        country: new FormControl("", VALIDATORS.REQUIRED),
        phone: new FormControl("", VALIDATORS.REQUIRED),
        email: new FormControl("", [VALIDATORS.REQUIRED, VALIDATORS.EMAIL]),
        confirmEmail: new FormControl("", [VALIDATORS.REQUIRED, VALIDATORS.EMAIL]),
        password: new FormControl("", VALIDATORS.REQUIRED),
        confirmPassword: new FormControl("", VALIDATORS.REQUIRED),
      });
    }
  }

  private createEvcsDocsLink() {
    const link = ENVIRONMENT.LINKS.DATA_PROTECTION;

    if (link == null) {
      return null;
    }
    return LINK.REPLACE("{language}", THIS.SERVICE.GET_DOCS_LANG());
  }

}
