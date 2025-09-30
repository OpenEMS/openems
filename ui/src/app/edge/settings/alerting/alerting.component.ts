// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component, LOCALE_ID, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, FormsModule, Validators } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyFieldConfig, FormlyFormOptions, FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { Subscription } from "rxjs";
import tr from "src/app/edge/settings/alerting/shared/TRANSLATION.JSON";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { GetUserAlertingConfigsRequest } from "src/app/shared/jsonrpc/request/getUserAlertingConfigsRequest";
import { SetUserAlertingConfigsRequest, UserSettingRequest } from "src/app/shared/jsonrpc/request/setUserAlertingConfigsRequest";
import { AlertingSettingResponse, GetUserAlertingConfigsResponse } from "src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse";
import { User } from "src/app/shared/jsonrpc/shared";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { Icon } from "src/app/shared/type/widget";
import { ArrayUtils } from "src/app/shared/utils/array/ARRAY.UTILS";
import { FormUtils } from "src/app/shared/utils/form/FORM.UTILS";
import { currentUserRows, otherUserRows } from "./formly/formly-alerting-configs";

export enum AlertingType {
  OFFLINE,
  FAULT,
  WARNING,
}

export type DefaultValues = { [K in AlertingType]: Delay[]; };
export type Delay = { value: number, label: string };

type AlertingSetting = AlertingSettingResponse;
type DetailedAlertingSetting = AlertingSetting & { isOfflineActive: boolean, isFaultActive: boolean, isWarningActive: boolean };

@Component({
  selector: ALERTING_COMPONENT.SELECTOR,
  templateUrl: "./ALERTING.COMPONENT.HTML",
  standalone: true,
  imports: [
    CommonModule,
    NgxSpinnerModule,
    FormlyModule,
    TranslateModule,
    IonicModule,
    HelpButtonComponent,
    FormsModule,
  ],
  providers: [
    { provide: LOCALE_ID, useFactory: () => (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.GET_BY_BROWSER_LANG(NAVIGATOR.LANGUAGE) ?? LANGUAGE.DEFAULT).key },
  ],
})
export class AlertingComponent implements OnInit, OnDestroy {
  protected static readonly SELECTOR = "alerting";
  private static readonly NO_ALERTING: number = 0;
  public readonly spinnerId: string = ALERTING_COMPONENT.SELECTOR;
  protected AlertingType = AlertingType;

  protected readonly defaultValues: DefaultValues = {
    [ALERTING_TYPE.OFFLINE]: THIS.AS_DELAY_OPTIONS([15, 60, 1440]),
    [ALERTING_TYPE.FAULT]: THIS.AS_DELAY_OPTIONS([15, 60, 1440]),
    [ALERTING_TYPE.WARNING]: THIS.AS_DELAY_OPTIONS([15, 60, 1440]),
  };

  protected edge: Edge;
  protected user: User;
  protected error: Error;

  protected currentUserInformation: DetailedAlertingSetting;
  protected currentUserForm: { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: FormlyFormOptions };
  protected otherUserForm: { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: FormlyFormOptions };

  protected otherUserInformation: AlertingSetting[];

  private subscriptions: Subscription = new Subscription();

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
    public formBuilder: FormBuilder,
  ) {
    LANGUAGE.SET_ADDITIONAL_TRANSLATION_FILE(tr, translate).then(({ lang, translations, shouldMerge }) => {
      TRANSLATE.SET_TRANSLATION(lang, translations, shouldMerge);
    });
  }

  /**
   * Checks if form is valid
   *
   * @param formGroup the formGroup
   * @returns true, if controls are valid, else false
   */
  public static isFormValid(formGroup: FormGroup): boolean {
    const isFaultAlerting = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<boolean>(formGroup, "fault-toggle");
    const isOfflineAlerting = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<boolean>(formGroup, "offline-toggle");

    const isOfflineCheckboxChecked = FORM_UTILS.FIND_FORM_CONTROL_SAFELY(formGroup, "offline-checkbox");
    IS_OFFLINE_CHECKBOX_CHECKED.SET_VALIDATORS(VALIDATORS.REQUIRED_TRUE);
    IS_OFFLINE_CHECKBOX_CHECKED.UPDATE_VALUE_AND_VALIDITY();

    const isFaultCheckboxChecked = FORM_UTILS.FIND_FORM_CONTROL_SAFELY(formGroup, "fault-checkbox");
    IS_FAULT_CHECKBOX_CHECKED.SET_VALIDATORS(VALIDATORS.REQUIRED_TRUE);
    IS_FAULT_CHECKBOX_CHECKED.UPDATE_VALUE_AND_VALIDITY();

    const faultInvalid = isFaultAlerting && IS_FAULT_CHECKBOX_CHECKED.INVALID;
    const offlineInvalid = isOfflineAlerting && IS_OFFLINE_CHECKBOX_CHECKED.INVALID;

    return !(faultInvalid || offlineInvalid);
  }

  public ngOnInit(): void {
    THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;

      THIS.SERVICE.METADATA.SUBSCRIBE(metadata => {
        THIS.USER = METADATA.USER;
      });

      const request = new GetUserAlertingConfigsRequest({ edgeId: THIS.EDGE.ID });

      THIS.SEND_REQUEST(request).then(response => {
        const result = RESPONSE.RESULT;

        THIS.SETUP_CURRENT_USER(RESULT.CURRENT_USER_SETTINGS);
        THIS.SETUP_OTHER_USERS(RESULT.OTHER_USERS_SETTINGS);
        THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
      }).catch(error => {
        THIS.ERROR = ERROR.ERROR;
      });
    });
  }

  ngOnDestroy() {
    THIS.SUBSCRIPTIONS.UNSUBSCRIBE();
  }

  /**
   * get if given delay is valid
 */
  protected isValidDelay(type: AlertingType, delay: number): boolean {
    if (delay <= 0) {
      return false;
    }
    return THIS.DEFAULT_VALUES[type].some(e => E.VALUE === delay);
  }

  /**
 * get the label matching the given delay, with translated timeunits and
 * attention to writing differences and singular and plural.
 *
 * @param delay to generate label for
 * @returns label as string
 */
  protected getLabelToDelay(delay: number): string {
    if (delay <= 0) {
      return THIS.TRANSLATE.INSTANT("EDGE.CONFIG.ALERTING.DEACTIVATED");
    }
    if (delay >= 1440) {
      delay = delay / 1440;
      return delay + " " + (delay == 1
        ? THIS.TRANSLATE.INSTANT("GENERAL.TIME.DAY")
        : THIS.TRANSLATE.INSTANT("GENERAL.TIME.DAYS"));
    } else if (delay >= 60) {
      delay = delay / 60;
      return delay + " " + (delay == 1
        ? THIS.TRANSLATE.INSTANT("GENERAL.TIME.HOUR")
        : THIS.TRANSLATE.INSTANT("GENERAL.TIME.HOURS"));
    } else {
      return delay + " " + (delay == 1
        ? THIS.TRANSLATE.INSTANT("GENERAL.TIME.MINUTE")
        : THIS.TRANSLATE.INSTANT("GENERAL.TIME.MINUTES"));
    }
  }

  protected setUsersAlertingConfig() {
    const edgeId: string = THIS.EDGE.ID;

    const dirtyformGroups: FormGroup<any>[] = [];
    const changedUserSettings: UserSettingRequest[] = [];

    // current user form
    if (THIS.CURRENT_USER_FORM.FORM_GROUP.DIRTY) {
      const formGroup = THIS.CURRENT_USER_FORM.FORM_GROUP;
      DIRTYFORM_GROUPS.PUSH(formGroup);
      const isFormValid = ALERTING_COMPONENT.IS_FORM_VALID(formGroup);

      if (!isFormValid) {
        FORM_GROUP.MARK_ALL_AS_TOUCHED();
        THIS.SERVICE.TOAST("Please check the mail option", "warning");
        return;
      }

      CHANGED_USER_SETTINGS.PUSH({
        userLogin: THIS.CURRENT_USER_INFORMATION.USER_LOGIN,
        ...THIS.GET_DELAYS(formGroup),
      });
    }

    // other users form
    const userOptions: AlertingSetting[] = [];
    if (THIS.OTHER_USER_INFORMATION) {
      if (THIS.OTHER_USER_FORM.FORM_GROUP.DIRTY) {
        DIRTYFORM_GROUPS.PUSH(THIS.OTHER_USER_FORM.FORM_GROUP);

        for (const user of THIS.OTHER_USER_INFORMATION) {
          const formGroup = THIS.OTHER_USER_FORM.FORM_GROUP.CONTROLS[USER.USER_LOGIN] as FormGroup;

          if (FORM_GROUP.PRISTINE) {
            continue;
          }

          const isFormValid = ALERTING_COMPONENT.IS_FORM_VALID(formGroup);
          if (!isFormValid) {
            FORM_GROUP.MARK_ALL_AS_TOUCHED();
            THIS.SERVICE.TOAST("Please check all required fields", "warning");
            return;
          }

          CHANGED_USER_SETTINGS.PUSH({
            userLogin: USER.USER_LOGIN,
            ...THIS.GET_DELAYS(formGroup),
          });
          USER_OPTIONS.PUSH(user);
        }
      }
    }

    const request = new SetUserAlertingConfigsRequest({ edgeId: edgeId, userSettings: changedUserSettings });
    THIS.SEND_REQUEST_AND_UPDATE(request, dirtyformGroups);
  }

  /**
 * get if any userSettings has changed/is dirty.
 * @returns true if any settings are changed, else false
 */
  protected isDirty(): boolean {
    if (THIS.ERROR || !THIS.CURRENT_USER_FORM) {
      return false;
    }
    return THIS.CURRENT_USER_FORM?.formGroup?.dirty || THIS.OTHER_USER_FORM.FORM_GROUP?.dirty;
  }

  private setupCurrentUser(response: AlertingSettingResponse) {
    THIS.CURRENT_USER_INFORMATION = THIS.AS_DETAILED_SETTINGS(response);
    THIS.CURRENT_USER_FORM = THIS.GENERATE_FORM(THIS.CURRENT_USER_INFORMATION, THIS.EDGE.ROLE);
  }

  private generateForm(settings: DetailedAlertingSetting, edgeRole: Role): { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: any, } {
    const delays: Delay[] = THIS.DEFAULT_VALUES[ALERTING_TYPE.OFFLINE];

    if (!THIS.IS_VALID_DELAY(ALERTING_TYPE.OFFLINE, SETTINGS.OFFLINE_EDGE_DELAY)) {
      DELAYS.PUSH({ value: SETTINGS.OFFLINE_EDGE_DELAY, label: THIS.GET_LABEL_TO_DELAY(SETTINGS.OFFLINE_EDGE_DELAY) });
    }

    return {
      formGroup: new FormGroup({
        "offline-toggle": new FormControl(SETTINGS.IS_OFFLINE_ACTIVE, VALIDATORS.REQUIRED),
        "offline-delay-selection": new FormControl(SETTINGS.OFFLINE_EDGE_DELAY, VALIDATORS.REQUIRED),
        "offline-checkbox": new FormControl(SETTINGS.IS_OFFLINE_ACTIVE, VALIDATORS.REQUIRED_TRUE),
        "fault-toggle": new FormControl(SETTINGS.IS_FAULT_ACTIVE, VALIDATORS.REQUIRED),
        "fault-delay-selection": new FormControl(SETTINGS.FAULT_EDGE_DELAY, VALIDATORS.REQUIRED),
        "fault-checkbox": new FormControl(SETTINGS.IS_FAULT_ACTIVE, VALIDATORS.REQUIRED_TRUE),
      }),
      options: {},
      model: {},
      fields: [{
        key: "currentUser",
        type: "input",
        templateOptions: {
          options: currentUserRows(THIS.DEFAULT_VALUES, THIS.TRANSLATE, edgeRole),
        },
        wrappers: ["formly-current-user-alerting"],
      },
      ],
    };
  }

  private setupOtherUsers(response: AlertingSettingResponse[]) {
    if (!response || RESPONSE.LENGTH === 0) {
      return;
    }

    const formGroup = new FormGroup({});
    THIS.OTHER_USER_INFORMATION = [];

    const sorted = ARRAY_UTILS.SORTED_ALPHABETICALLY(response, e => E.USER_LOGIN);
    SORTED.FOR_EACH((r) => {

      const setting: AlertingSettingResponse = {
        userLogin: R.USER_LOGIN,
        offlineEdgeDelay: THIS.GET_VALUE_OR_DEFAULT(r, ALERTING_TYPE.OFFLINE),
        faultEdgeDelay: THIS.GET_VALUE_OR_DEFAULT(r, ALERTING_TYPE.FAULT),
        warningEdgeDelay: THIS.GET_VALUE_OR_DEFAULT(r, ALERTING_TYPE.WARNING),
      };

      THIS.OTHER_USER_INFORMATION.PUSH(setting);

      FORM_GROUP.ADD_CONTROL(SETTING.USER_LOGIN, //
        THIS.FORM_BUILDER.GROUP({
          "offline-toggle": new FormControl(R.OFFLINE_EDGE_DELAY > 0, VALIDATORS.REQUIRED),
          "offline-delay-selection": new FormControl(SETTING.OFFLINE_EDGE_DELAY, VALIDATORS.REQUIRED),
          "offline-checkbox": new FormControl(R.OFFLINE_EDGE_DELAY > 0, VALIDATORS.REQUIRED_TRUE),
          "fault-toggle": new FormControl(R.FAULT_EDGE_DELAY > 0, VALIDATORS.REQUIRED),
          "fault-delay-selection": new FormControl(SETTING.FAULT_EDGE_DELAY, VALIDATORS.REQUIRED),
          "fault-checkbox": new FormControl(R.FAULT_EDGE_DELAY > 0, VALIDATORS.REQUIRED_TRUE),
        }));
    });

    THIS.OTHER_USER_FORM = {
      formGroup: formGroup,
      options: {},
      model: {},
      fields: [{
        key: "otherUsers",
        type: "input",
        props: {
          options: otherUserRows(response, THIS.DEFAULT_VALUES, THIS.TRANSLATE),
        },
        wrappers: ["formly-other-users-alerting"],
      },
      ],
    };
  }

  private getValue(setting: AlertingSetting, type: AlertingType): number {
    switch (type) {
      case ALERTING_TYPE.OFFLINE:
        return SETTING.OFFLINE_EDGE_DELAY;
      case ALERTING_TYPE.FAULT:
        return SETTING.FAULT_EDGE_DELAY;
      case ALERTING_TYPE.WARNING:
        return SETTING.WARNING_EDGE_DELAY;
    }
  }

  private getValueOrDefault(setting: AlertingSetting, type: AlertingType) {
    const val = THIS.GET_VALUE(setting, type);
    return val <= 0 ? THIS.DEFAULT_VALUES[type][0].value : val;
  }

  private asDetailedSettings(setting: AlertingSetting): DetailedAlertingSetting {
    return {
      userLogin: SETTING.USER_LOGIN,
      offlineEdgeDelay: THIS.GET_VALUE_OR_DEFAULT(setting, ALERTING_TYPE.OFFLINE),
      warningEdgeDelay: THIS.GET_VALUE_OR_DEFAULT(setting, ALERTING_TYPE.WARNING),
      faultEdgeDelay: THIS.GET_VALUE_OR_DEFAULT(setting, ALERTING_TYPE.FAULT),
      isOfflineActive: SETTING.OFFLINE_EDGE_DELAY > 0,
      isFaultActive: SETTING.FAULT_EDGE_DELAY > 0,
      isWarningActive: SETTING.WARNING_EDGE_DELAY > 0,
    };
  }

  private asDelayOptions(settings: number[]): Delay[] {
    return SETTINGS.MAP(e => THIS.AS_DELAY_OPTION(e));
  }

  private asDelayOption(setting: number): Delay {
    return { value: setting, label: THIS.GET_LABEL_TO_DELAY(setting) };
  }

  /**
   * send requests, show events using toasts and reset given formGroup if successful.
   * @param request   stucture containing neccesary parameters
   * @param formGroup   formGroup to update
   * @returns @GetUserAlertingConfigsResponse containing logged in users data, as well as data other users, if user is admin
   */
  private sendRequestAndUpdate(request: GetUserAlertingConfigsRequest | SetUserAlertingConfigsRequest, formGroup: FormGroup<any>[]) {
    THIS.SEND_REQUEST(request)
      .then(() => {
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
        for (const group of FORM_GROUP.VALUES()) {
          GROUP.MARK_AS_PRISTINE();
        }
      })
      .catch((response) => {
        const error = RESPONSE.ERROR;
        THIS.ERROR_TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED"), ERROR.MESSAGE);
      });
  }

  /**
   * send requests and show events using toasts.
   * @param request   stucture containing neccesary parameters
   * @returns @GetUserAlertingConfigsResponse containing logged in users data, as well as data other users, if user is admin
   */
  private sendRequest(request: GetUserAlertingConfigsRequest | SetUserAlertingConfigsRequest): Promise<GetUserAlertingConfigsResponse> {
    return new Promise((resolve, reject) => {
      THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
      THIS.WEBSOCKET.SEND_REQUEST(request).then(response => {
        resolve(response as GetUserAlertingConfigsResponse);
      }).catch(reason => {
        const error = REASON.ERROR;
        THIS.ERROR_TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.ALERTING.TOAST.ERROR"), ERROR.MESSAGE);
        reject(reason);
      }).finally(() => {
        THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
      });
    });
  }

  private errorToast(errorType: string, errorMsg: string) {
    THIS.SERVICE.TOAST("[ " + errorType + " ]<br/>" + errorMsg, "danger");
  }

  private getDelays(formGroup: FormGroup): Omit<UserSettingRequest, "userLogin"> {
    const offlineDelay = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<number>(formGroup, "offline-delay-selection");
    const isOfflineAlerting = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<boolean>(formGroup, "offline-toggle");
    const faultDelay = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<number>(formGroup, "fault-delay-selection");
    const isFaultAlerting = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<boolean>(formGroup, "fault-toggle");

    return {
      offlineEdgeDelay: isOfflineAlerting ? offlineDelay : 0,
      warningEdgeDelay: 0,
      faultEdgeDelay: isFaultAlerting ? faultDelay : 0,
    };
  }


}


export type ToggleFormlyField = {
  type: "toggle",
  name: string,
  formControl: string,
  icon?: Icon & {
    position: "start" | "end",
  }
};

export type RadioButtonsFormlyField = {
  type: "radio-buttons",
  name: string,
  formControl: string,
  options: Delay[]
};

export type CheckboxFormlyField = {
  type: "checkbox",
  name: string,
  formControl: string,
};
