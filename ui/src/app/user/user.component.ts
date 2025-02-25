// @ts-strict-ignore
import { KeyValue } from "@angular/common";
import { Component, effect, OnInit } from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { environment, Theme as SystemTheme } from "../../environments";
import { Changelog } from "../changelog/view/component/changelog.constants";
import { Theme as UserTheme } from "../edge/history/shared";
import { GetUserInformationRequest } from "../shared/jsonrpc/request/getUserInformationRequest";
import { SetUserInformationRequest } from "../shared/jsonrpc/request/setUserInformationRequest";
import { UpdateUserLanguageRequest } from "../shared/jsonrpc/request/updateUserLanguageRequest";
import { GetUserInformationResponse } from "../shared/jsonrpc/response/getUserInformationResponse";
import { UserService } from "../shared/service/user.service";
import { Service, Websocket } from "../shared/shared";
import { COUNTRY_OPTIONS } from "../shared/type/country";
import { Language } from "../shared/type/language";
import { Role } from "../shared/type/role";

type CompanyUserInformation = UserInformation & { companyName: string };

type UserInformation = {
  firstname: string,
  lastname: string,
  email: string,
  phone: string,
  street: string,
  zip: string,
  city: string,
  country: string
};

@Component({
  templateUrl: "./user.component.html",
  standalone: false,
})
export class UserComponent implements OnInit {

  private static readonly DEFAULT_THEME: UserTheme = UserTheme.LIGHT; // Theme as of "Light","Dark" or "System" Themes.
  protected userTheme: UserTheme; // Theme as of "Light","Dark" or "System" Themes.
  protected systemTheme: SystemTheme; // SystemTheme as of "FENECON","Heckert" or "OpenEMS" Themes.

  protected readonly themes: KeyValue<string, string>[] = [
    { key: "Light", value: "light" },
    { key: "Dark", value: "dark" },
    { key: "System", value: "system" },
  ];
  protected readonly environment = environment;
  protected readonly uiVersion = Changelog.UI_VERSION;
  protected readonly languages: Language[] = Language.ALL;
  protected currentLanguage: Language;
  protected isEditModeDisabled: boolean = true;
  protected form: { formGroup: FormGroup, model: UserInformation | CompanyUserInformation };
  protected showInformation: boolean = false;
  protected userInformationFields: FormlyFieldConfig[] = [{
    key: "firstname",
    type: "input",
    props: {
      label: this.translate.instant("Register.Form.firstname"),
      disabled: true,
    },
  },
  {
    key: "lastname",
    type: "input",
    props: {
      label: this.translate.instant("Register.Form.lastname"),
      disabled: true,
    },
  }];
  protected companyInformationFields: FormlyFieldConfig[] = [];

  protected isAtLeastAdmin: boolean = false;
  protected isAllowedToSeeUserDetails: boolean = true;

  constructor(
    public translate: TranslateService,
    public service: Service,
    private websocket: Websocket,
    private userService: UserService,
  ) {
    effect(async () => {
      const user = this.userService.currentUser();

      if (user && this.form == null) {
        this.isAtLeastAdmin = Role.isAtLeast(user.globalRole, Role.ADMIN);
        await this.updateUserInformation();

        this.isAllowedToSeeUserDetails = this.isUserAllowedToSeeContactDetails(user.id);
        this.showInformation = this.form != null;
        this.userTheme = user.getThemeFromSettings() ?? UserComponent.DEFAULT_THEME;
      }
    });
  }

  ngOnInit() {
    this.currentLanguage = Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT;
    this.systemTheme = environment.theme as SystemTheme;
  }

  public setTheme(theme: UserTheme): void {
    this.userService.selectTheme(theme);
  }

  public applyChanges() {

    const params: SetUserInformationRequest["params"] = {
      user: {
        lastname: this.form.model.lastname,
        firstname: this.form.model.firstname,
        email: this.form.model.email,
        phone: this.form.model.phone,
        address: {
          street: this.form.model.street,
          zip: this.form.model.zip,
          city: this.form.model.city,
          country: this.form.model.country,
        },
      },
    };

    this.service.websocket.sendRequest(new SetUserInformationRequest(params)).then(() => {
      this.service.toast(this.translate.instant("General.changeAccepted"), "success");
    }).catch((reason) => {
      this.service.toast(this.translate.instant("General.changeFailed") + "\n" + reason.error.message, "danger");
    });
    this.enableAndDisableFormFields();
    this.form.formGroup.markAsPristine();
  }

  public enableAndDisableEditMode(): void {
    if (this.isEditModeDisabled == false) {
      this.getUserInformation().then((userInformation) => {
        this.form = {
          formGroup: new FormGroup({}),
          model: userInformation,
        };
      });
    }

    this.enableAndDisableFormFields();
  }

  public enableAndDisableFormFields(): boolean {

    this.userInformationFields = this.userInformationFields.map(field => {
      field.props.disabled = !field.props.disabled;
      return field;
    });

    return this.isEditModeDisabled = !this.isEditModeDisabled;
  }

  public getUserInformation(): Promise<UserInformation | CompanyUserInformation> {

    return new Promise(resolve => {
      const interval = setInterval(() => {
        if (this.websocket.status == "online") {
          this.service.websocket.sendRequest(new GetUserInformationRequest()).then((response: GetUserInformationResponse) => {
            const user = response.result.user;

            resolve({
              lastname: user.lastname,
              firstname: user.firstname,

              // Show company if available
              email: user.email,
              phone: user.phone,
              street: user.address.street,
              zip: user.address.zip,
              city: user.address.city,
              country: user.address.country,
              ...(user.company?.name ? { companyName: user.company.name } : {}),
            });
          }).catch(() => {
            resolve({
              lastname: "",
              firstname: "",
              email: "",
              phone: "",
              street: "",
              zip: "",
              city: "",
              country: "",
            });
          });
          clearInterval(interval);
        }
      }, 1000);
    });
  }

  /**
   * Logout from OpenEMS Edge or Backend.
   */
  public doLogout() {
    this.userService.currentUser.set(null);
    this.websocket.logout();
  }

  public toggleDebugMode(event: CustomEvent) {
    localStorage.setItem("DEBUGMODE", event.detail["checked"]);
    this.environment.debugMode = event.detail["checked"];
  }

  public setLanguage(language: Language): void {
    // Get Key of LanguageTag Enum
    localStorage.LANGUAGE = language.key;

    this.service.setLang(language);
    this.websocket.sendRequest(new UpdateUserLanguageRequest({ language: language.key })).then(() => {
      this.service.toast(this.translate.instant("General.changeAccepted"), "success");
    }).catch((reason) => {
      this.service.toast(this.translate.instant("General.changeFailed") + "\n" + reason.error.message, "danger");
    });

    this.currentLanguage = language;
    this.translate.use(language.key);
  }

  private updateUserInformation(): Promise<void> {
    return this.getUserInformation().then((userInformation) => {
      this.form = {
        formGroup: new FormGroup({}),
        model: userInformation,
      };

      const baseInformationFields: FormlyFieldConfig[] = [{
        key: "street",
        type: "input",
        props: {
          label: this.translate.instant("Register.Form.street"),
          disabled: true,

        },
      },
      {
        key: "zip",
        type: "input",
        props: {
          label: this.translate.instant("Register.Form.zip"),
          disabled: true,
        },
      },
      {
        key: "city",
        type: "input",
        props: {
          label: this.translate.instant("Register.Form.city"),
          disabled: true,
        },
      },
      {
        key: "country",
        type: "select",
        props: {
          label: this.translate.instant("Register.Form.country"),
          options: COUNTRY_OPTIONS(this.translate),
          disabled: true,
        },
      },
      {
        key: "email",
        type: "input",
        props: {
          label: this.translate.instant("Register.Form.email"),
          disabled: true,
        },
        validators: {
          validation: [Validators.email],
        },
      },
      {
        key: "phone",
        type: "input",
        props: {
          label: this.translate.instant("Register.Form.phone"),
          disabled: true,
        },

      }];

      if (Object.prototype.hasOwnProperty.call(userInformation, "companyName")) {
        this.companyInformationFields = [{
          key: "companyName",
          type: "input",
          props: {
            label: this.translate.instant("Register.Form.companyName"),
            disabled: true,
          },
        },
        ...baseInformationFields,
        ];
      } else {
        this.userInformationFields = baseInformationFields;
      }
    });
  }

  /**
   * Checks if user is allowed to see contact details
   *
   * @param id the user id
   * @returns true, if user is allowed to see contact details
   */
  private isUserAllowedToSeeContactDetails(id: string): boolean {
    switch (id) {
      case "demo@fenecon.de":
      case "pv@schachinger-gaerten.de":
      case "pv@studentenpark1-straubing.de":
        return false;
      default:
        return true;
    }
  }
}

