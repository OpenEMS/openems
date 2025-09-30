// @ts-strict-ignore
import { KeyValue } from "@angular/common";
import { Component, effect, OnInit, untracked } from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { environment, Theme as SystemTheme } from "../../environments";
import { Changelog } from "../changelog/view/component/CHANGELOG.CONSTANTS";
import { Theme as UserTheme } from "../edge/history/shared";
import { NavigationService } from "../shared/components/navigation/service/NAVIGATION.SERVICE";
import { GetUserInformationRequest } from "../shared/jsonrpc/request/getUserInformationRequest";
import { SetUserInformationRequest } from "../shared/jsonrpc/request/setUserInformationRequest";
import { UpdateUserLanguageRequest } from "../shared/jsonrpc/request/updateUserLanguageRequest";
import { GetUserInformationResponse } from "../shared/jsonrpc/response/getUserInformationResponse";
import { UserService } from "../shared/service/USER.SERVICE";
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
  templateUrl: "./USER.COMPONENT.HTML",
  standalone: false,
})
export class UserComponent implements OnInit {

  private static readonly DEFAULT_THEME: UserTheme = USER_THEME.LIGHT; // Theme as of "Light","Dark" or "System" Themes.
  protected userTheme: UserTheme; // Theme as of "Light","Dark" or "System" Themes.
  protected systemTheme: SystemTheme; // SystemTheme as of "OpenEMS" or other OEM Themes.

  protected readonly themes: KeyValue<string, string>[] = [
    { key: "Light", value: "light" },
    { key: "Dark", value: "dark" },
    { key: "System", value: "system" },
  ];
  protected readonly environment = environment;
  protected readonly uiVersion = Changelog.UI_VERSION;
  protected readonly languages: Language[] = LANGUAGE.ALL;
  protected currentLanguage: Language;
  protected isEditModeDisabled: boolean = true;
  protected form: { formGroup: FormGroup, model: UserInformation | CompanyUserInformation };
  protected showInformation: boolean = false;
  protected userInformationFields: FormlyFieldConfig[] = [{
    key: "firstname",
    type: "input",
    props: {
      label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.FIRSTNAME"),
      disabled: true,
    },
  },
  {
    key: "lastname",
    type: "input",
    props: {
      label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.LASTNAME"),
      disabled: true,
    },
  }];
  protected companyInformationFields: FormlyFieldConfig[] = [];

  protected isAtLeastAdmin: boolean = false;
  protected isAllowedToSeeUserDetails: boolean = true;
  protected useNewUi: boolean | null = null;
  protected newNavigationForced: boolean = false;

  constructor(
    public translate: TranslateService,
    public service: Service,
    private websocket: Websocket,
    private userService: UserService,
    private navigationService: NavigationService,
  ) {
    effect(async () => {
      const user = THIS.USER_SERVICE.CURRENT_USER();

      if (user && THIS.FORM == null) {
        THIS.IS_AT_LEAST_ADMIN = ROLE.IS_AT_LEAST(USER.GLOBAL_ROLE, ROLE.ADMIN);
        await THIS.UPDATE_USER_INFORMATION();

        THIS.IS_ALLOWED_TO_SEE_USER_DETAILS = THIS.IS_USER_ALLOWED_TO_SEE_CONTACT_DETAILS(USER.ID);
        THIS.SHOW_INFORMATION = THIS.FORM != null;
        THIS.USER_THEME = USER.GET_THEME_FROM_SETTINGS() ?? UserComponent.DEFAULT_THEME;
        THIS.USE_NEW_UI = USER.GET_USE_NEW_UIFROM_SETTINGS();
        THIS.NEW_NAVIGATION_FORCED = NAVIGATION_SERVICE.FORCE_NEW_NAVIGATION(untracked(() => THIS.SERVICE.CURRENT_EDGE()));
      }
    });
  }

  ngOnInit() {
    THIS.CURRENT_LANGUAGE = LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT;
    THIS.SYSTEM_THEME = ENVIRONMENT.THEME as SystemTheme;
  }

  public setTheme(theme: UserTheme): void {
    THIS.USER_SERVICE.SELECT_THEME(theme);
  }

  public applyChanges() {
    const params: SetUserInformationRequest["params"] = {
      user: {
        lastname: THIS.FORM.MODEL.LASTNAME,
        firstname: THIS.FORM.MODEL.FIRSTNAME,
        email: THIS.FORM.MODEL.EMAIL,
        phone: THIS.FORM.MODEL.PHONE,
        address: {
          street: THIS.FORM.MODEL.STREET,
          zip: THIS.FORM.MODEL.ZIP,
          city: THIS.FORM.MODEL.CITY,
          country: THIS.FORM.MODEL.COUNTRY,
        },
      },
    };

    THIS.SERVICE.WEBSOCKET.SEND_REQUEST(new SetUserInformationRequest(params)).then(() => {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
    }).catch((reason) => {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
    });
    THIS.ENABLE_AND_DISABLE_FORM_FIELDS();
    THIS.FORM.FORM_GROUP.MARK_AS_PRISTINE();
  }

  public enableAndDisableEditMode(): void {
    if (!THIS.IS_EDIT_MODE_DISABLED) {
      THIS.UPDATE_USER_INFORMATION();
    }
    THIS.ENABLE_AND_DISABLE_FORM_FIELDS();
  }

  public enableAndDisableFormFields(): boolean {
    THIS.USER_INFORMATION_FIELDS = THIS.USER_INFORMATION_FIELDS.MAP(field => {
      FIELD.PROPS.DISABLED = !FIELD.PROPS.DISABLED;
      return field;
    });
    return THIS.IS_EDIT_MODE_DISABLED = !THIS.IS_EDIT_MODE_DISABLED;
  }

  public getUserInformation(): Promise<UserInformation | CompanyUserInformation> {
    return new Promise(resolve => {
      const interval = setInterval(() => {
        if (THIS.WEBSOCKET.STATUS === "online") {
          THIS.SERVICE.WEBSOCKET.SEND_REQUEST(new GetUserInformationRequest()).then((response: GetUserInformationResponse) => {
            const user = RESPONSE.RESULT.USER;
            resolve({
              lastname: USER.LASTNAME,
              firstname: USER.FIRSTNAME,
              email: USER.EMAIL,
              phone: USER.PHONE,
              street: USER.ADDRESS.STREET,
              zip: USER.ADDRESS.ZIP,
              city: USER.ADDRESS.CITY,
              country: USER.ADDRESS.COUNTRY,
              ...(USER.COMPANY?.name ? { companyName: USER.COMPANY.NAME } : {}),
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
    THIS.USER_SERVICE.CURRENT_USER.SET(null);
    THIS.WEBSOCKET.LOGOUT();
  }

  public toggleDebugMode(event: Event) {
    LOCAL_STORAGE.SET_ITEM("DEBUGMODE", (event as CustomEvent).detail["checked"]);
    THIS.ENVIRONMENT.DEBUG_MODE = (event as CustomEvent).detail["checked"];
  }

  public async toggleNewUI(event: Event) {
    const isToggleOn = (event as CustomEvent).detail["checked"];
    THIS.SERVICE.START_SPINNER("user");
    await THIS.USER_SERVICE.UPDATE_USER_SETTINGS_WITH_PROPERTY("useNewUI", isToggleOn);
    THIS.SERVICE.STOP_SPINNER("user");
  }

  public setLanguage(language: Language): void {
    // Get Key of LanguageTag Enum
    LOCAL_STORAGE.LANGUAGE = LANGUAGE.KEY;

    THIS.SERVICE.SET_LANG(language);
    THIS.WEBSOCKET.SEND_REQUEST(new UpdateUserLanguageRequest({ language: LANGUAGE.KEY })).then(() => {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
    }).catch((reason) => {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
    });

    THIS.CURRENT_LANGUAGE = language;
    THIS.TRANSLATE.USE(LANGUAGE.KEY);
  }

  private updateUserInformation(): Promise<void> {
    return THIS.GET_USER_INFORMATION().then((userInformation) => {
      THIS.FORM = {
        formGroup: new FormGroup({}),
        model: userInformation,
      };

      const baseInformationFields: FormlyFieldConfig[] = [{
        key: "street",
        type: "input",
        props: {
          label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.STREET"),
          disabled: true,
        },
      },
      {
        key: "zip",
        type: "input",
        props: {
          label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.ZIP"),
          disabled: true,
        },
      },
      {
        key: "city",
        type: "input",
        props: {
          label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.CITY"),
          disabled: true,
        },
      },
      {
        key: "country",
        type: "select",
        props: {
          label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.COUNTRY"),
          options: COUNTRY_OPTIONS(THIS.TRANSLATE),
          disabled: true,
        },
      },
      {
        key: "email",
        type: "input",
        props: {
          label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.EMAIL"),
          disabled: true,
        },
        validators: {
          validation: [VALIDATORS.EMAIL],
        },
      },
      {
        key: "phone",
        type: "input",
        props: {
          label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.PHONE"),
          disabled: true,
        },

      }];

      if (OBJECT.PROTOTYPE.HAS_OWN_PROPERTY.CALL(userInformation, "companyName")) {
        THIS.COMPANY_INFORMATION_FIELDS = [{
          key: "companyName",
          type: "input",
          props: {
            label: THIS.TRANSLATE.INSTANT("REGISTER.FORM.COMPANY_NAME"),
            disabled: true,
          },
        },
        ...baseInformationFields,
        ];
      } else {
        THIS.USER_INFORMATION_FIELDS = baseInformationFields;
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
      case "demo@FENECON.DE":
      case "pv@schachinger-GAERTEN.DE":
      case "pv@studentenpark1-STRAUBING.DE":
        return false;
      default:
        return true;
    }
  }
}

