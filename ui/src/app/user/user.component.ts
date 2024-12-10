// @ts-strict-ignore
import { KeyValue } from "@angular/common";
import { Component, OnInit, effect } from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Changelog } from "src/app/changelog/view/component/changelog.constants";
import { environment } from "../../environments";
import { Theme } from "../edge/history/shared";
import { GetUserInformationRequest } from "../shared/jsonrpc/request/getUserInformationRequest";
import { SetUserInformationRequest } from "../shared/jsonrpc/request/setUserInformationRequest";
import { UpdateUserLanguageRequest } from "../shared/jsonrpc/request/updateUserLanguageRequest";
import { UpdateUserSettingsRequest } from "../shared/jsonrpc/request/updateUserSettingsRequest";
import { GetUserInformationResponse } from "../shared/jsonrpc/response/getUserInformationResponse";
import { User } from "../shared/jsonrpc/shared";
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
})
export class UserComponent implements OnInit {

  private static readonly DEFAULT_THEME: Theme = Theme.LIGHT;
  public currentTheme: string;
  public readonly themes: KeyValue<string, string>[] = [
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

  constructor(
    public translate: TranslateService,
    public service: Service,
    private route: ActivatedRoute,
    private websocket: Websocket,
  ) {
    effect(() => {
      const user = this.service.currentUser();

      if (user) {
        this.isAtLeastAdmin = Role.isAtLeast(user.globalRole, Role.ADMIN);
        this.updateUserInformation();
      }
    });
  }

  public static applyUserSettings(user: User): void {
    const theme = UserComponent.getCurrentTheme(user);
    let attr: Exclude<`${Theme}`, Theme.SYSTEM> = theme;
    localStorage.setItem("THEME", theme);
    if (theme === Theme.SYSTEM) {
      attr = window.matchMedia("(prefers-color-scheme: dark)").matches ? Theme.DARK : Theme.LIGHT;
    }
    document.documentElement.setAttribute("data-theme", attr);
  }

  public static getPreferedColorSchemeFromTheme(theme: Theme) {
    return theme === Theme.SYSTEM ? (window.matchMedia("(prefers-color-scheme: dark)").matches ? Theme.DARK : Theme.LIGHT) : theme;
  }

  public static getCurrentTheme(user: User): Theme {
    return user?.settings["theme"] ?? localStorage.getItem("THEME") ?? UserComponent.DEFAULT_THEME;
  }

  ngOnInit() {
    // Set currentLanguage to
    this.currentLanguage = Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT;

    this.updateUserInformation().then(() => {
      this.service.metadata.subscribe(entry => {
        this.showInformation = true;
      });
    });
  }

  public setTheme(theme: Theme): void {
    this.service.websocket.sendSafeRequest(new UpdateUserSettingsRequest({
      settings: { theme: theme },
    })).then(() => {
      const currentUser = this.service.currentUser();
      if (currentUser) {
        currentUser.settings["theme"] = theme;
        UserComponent.applyUserSettings(currentUser);
      }
    });
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
}
