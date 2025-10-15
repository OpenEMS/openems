// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig, FormlyFormOptions, FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { Subscription } from "rxjs";
import tr from "src/app/edge/settings/alerting/shared/translation.json";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { GetUserAlertingConfigsRequest } from "src/app/shared/jsonrpc/request/getUserAlertingConfigsRequest";
import { SetUserAlertingConfigsRequest, UserSettingRequest } from "src/app/shared/jsonrpc/request/setUserAlertingConfigsRequest";
import { AlertingSettingResponse, GetUserAlertingConfigsResponse } from "src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse";
import { User } from "src/app/shared/jsonrpc/shared";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { Icon } from "src/app/shared/type/widget";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { CommonUiModule } from "../../../shared/common-ui.module";
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
  selector: AlertingComponent.SELECTOR,
  templateUrl: "./alerting.component.html",
  standalone: true,
  imports: [
    CommonUiModule,
    LocaleProvider,
    HelpButtonComponent,
    NgxSpinnerModule,
    FormlyModule,
  ],
})
export class AlertingComponent implements OnInit, OnDestroy {
  protected static readonly SELECTOR = "alerting";
  private static readonly NO_ALERTING: number = 0;
  public readonly spinnerId: string = AlertingComponent.SELECTOR;
  protected AlertingType = AlertingType;

  protected readonly defaultValues: DefaultValues = {
    [AlertingType.OFFLINE]: this.asDelayOptions([15, 60, 1440]),
    [AlertingType.FAULT]: this.asDelayOptions([15, 60, 1440]),
    [AlertingType.WARNING]: this.asDelayOptions([15, 60, 1440]),
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
    Language.setAdditionalTranslationFile(tr, translate).then(({ lang, translations, shouldMerge }) => {
      translate.setTranslation(lang, translations, shouldMerge);
    });
  }

  /**
   * Checks if form is valid
   *
   * @param formGroup the formGroup
   * @returns true, if controls are valid, else false
   */
  public static isFormValid(formGroup: FormGroup): boolean {
    const isFaultAlerting = FormUtils.findFormControlsValueSafely<boolean>(formGroup, "fault-toggle");
    const isOfflineAlerting = FormUtils.findFormControlsValueSafely<boolean>(formGroup, "offline-toggle");

    const isOfflineCheckboxChecked = FormUtils.findFormControlSafely(formGroup, "offline-checkbox");
    isOfflineCheckboxChecked.setValidators(Validators.requiredTrue);
    isOfflineCheckboxChecked.updateValueAndValidity();

    const isFaultCheckboxChecked = FormUtils.findFormControlSafely(formGroup, "fault-checkbox");
    isFaultCheckboxChecked.setValidators(Validators.requiredTrue);
    isFaultCheckboxChecked.updateValueAndValidity();

    const faultInvalid = isFaultAlerting && isFaultCheckboxChecked.invalid;
    const offlineInvalid = isOfflineAlerting && isOfflineCheckboxChecked.invalid;

    return !(faultInvalid || offlineInvalid);
  }

  public ngOnInit(): void {
    this.service.startSpinner(this.spinnerId);
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;

      this.service.metadata.subscribe(metadata => {
        this.user = metadata.user;
      });

      const request = new GetUserAlertingConfigsRequest({ edgeId: this.edge.id });

      this.sendRequest(request).then(response => {
        const result = response.result;

        this.setupCurrentUser(result.currentUserSettings);
        this.setupOtherUsers(result.otherUsersSettings);
        this.service.stopSpinner(this.spinnerId);
      }).catch(error => {
        this.error = error.error;
      });
    });
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  /**
   * get if given delay is valid
 */
  protected isValidDelay(type: AlertingType, delay: number): boolean {
    if (delay <= 0) {
      return false;
    }
    return this.defaultValues[type].some(e => e.value === delay);
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
      return this.translate.instant("Edge.Config.ALERTING.DEACTIVATED");
    }
    if (delay >= 1440) {
      delay = delay / 1440;
      return delay + " " + (delay == 1
        ? this.translate.instant("General.TIME.DAY")
        : this.translate.instant("General.TIME.DAYS"));
    } else if (delay >= 60) {
      delay = delay / 60;
      return delay + " " + (delay == 1
        ? this.translate.instant("General.TIME.HOUR")
        : this.translate.instant("General.TIME.HOURS"));
    } else {
      return delay + " " + (delay == 1
        ? this.translate.instant("General.TIME.MINUTE")
        : this.translate.instant("General.TIME.MINUTES"));
    }
  }

  protected setUsersAlertingConfig() {
    const edgeId: string = this.edge.id;

    const dirtyformGroups: FormGroup<any>[] = [];
    const changedUserSettings: UserSettingRequest[] = [];

    // current user form
    if (this.currentUserForm.formGroup.dirty) {
      const formGroup = this.currentUserForm.formGroup;
      dirtyformGroups.push(formGroup);
      const isFormValid = AlertingComponent.isFormValid(formGroup);

      if (!isFormValid) {
        formGroup.markAllAsTouched();
        this.service.toast("Please check the mail option", "warning");
        return;
      }

      changedUserSettings.push({
        userLogin: this.currentUserInformation.userLogin,
        ...this.getDelays(formGroup),
      });
    }

    // other users form
    const userOptions: AlertingSetting[] = [];
    if (this.otherUserInformation) {
      if (this.otherUserForm.formGroup.dirty) {
        dirtyformGroups.push(this.otherUserForm.formGroup);

        for (const user of this.otherUserInformation) {
          const formGroup = this.otherUserForm.formGroup.controls[user.userLogin] as FormGroup;

          if (formGroup.pristine) {
            continue;
          }

          const isFormValid = AlertingComponent.isFormValid(formGroup);
          if (!isFormValid) {
            formGroup.markAllAsTouched();
            this.service.toast("Please check all required fields", "warning");
            return;
          }

          changedUserSettings.push({
            userLogin: user.userLogin,
            ...this.getDelays(formGroup),
          });
          userOptions.push(user);
        }
      }
    }

    const request = new SetUserAlertingConfigsRequest({ edgeId: edgeId, userSettings: changedUserSettings });
    this.sendRequestAndUpdate(request, dirtyformGroups);
  }

  /**
 * get if any userSettings has changed/is dirty.
 * @returns true if any settings are changed, else false
 */
  protected isDirty(): boolean {
    if (this.error || !this.currentUserForm) {
      return false;
    }
    return this.currentUserForm?.formGroup?.dirty || this.otherUserForm.formGroup?.dirty;
  }

  private setupCurrentUser(response: AlertingSettingResponse) {
    this.currentUserInformation = this.asDetailedSettings(response);
    this.currentUserForm = this.generateForm(this.currentUserInformation, this.edge.role);
  }

  private generateForm(settings: DetailedAlertingSetting, edgeRole: Role): { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: any, } {
    const delays: Delay[] = this.defaultValues[AlertingType.OFFLINE];

    if (!this.isValidDelay(AlertingType.OFFLINE, settings.offlineEdgeDelay)) {
      delays.push({ value: settings.offlineEdgeDelay, label: this.getLabelToDelay(settings.offlineEdgeDelay) });
    }

    return {
      formGroup: new FormGroup({
        "offline-toggle": new FormControl(settings.isOfflineActive, Validators.required),
        "offline-delay-selection": new FormControl(settings.offlineEdgeDelay, Validators.required),
        "offline-checkbox": new FormControl(settings.isOfflineActive, Validators.requiredTrue),
        "fault-toggle": new FormControl(settings.isFaultActive, Validators.required),
        "fault-delay-selection": new FormControl(settings.faultEdgeDelay, Validators.required),
        "fault-checkbox": new FormControl(settings.isFaultActive, Validators.requiredTrue),
      }),
      options: {},
      model: {},
      fields: [{
        key: "currentUser",
        type: "input",
        templateOptions: {
          options: currentUserRows(this.defaultValues, this.translate, edgeRole),
        },
        wrappers: ["formly-current-user-alerting"],
      },
      ],
    };
  }

  private setupOtherUsers(response: AlertingSettingResponse[]) {
    if (!response || response.length === 0) {
      return;
    }

    const formGroup = new FormGroup({});
    this.otherUserInformation = [];

    const sorted = ArrayUtils.sortedAlphabetically(response, e => e.userLogin);
    sorted.forEach((r) => {

      const setting: AlertingSettingResponse = {
        userLogin: r.userLogin,
        offlineEdgeDelay: this.getValueOrDefault(r, AlertingType.OFFLINE),
        faultEdgeDelay: this.getValueOrDefault(r, AlertingType.FAULT),
        warningEdgeDelay: this.getValueOrDefault(r, AlertingType.WARNING),
      };

      this.otherUserInformation.push(setting);

      formGroup.addControl(setting.userLogin, //
        this.formBuilder.group({
          "offline-toggle": new FormControl(r.offlineEdgeDelay > 0, Validators.required),
          "offline-delay-selection": new FormControl(setting.offlineEdgeDelay, Validators.required),
          "offline-checkbox": new FormControl(r.offlineEdgeDelay > 0, Validators.requiredTrue),
          "fault-toggle": new FormControl(r.faultEdgeDelay > 0, Validators.required),
          "fault-delay-selection": new FormControl(setting.faultEdgeDelay, Validators.required),
          "fault-checkbox": new FormControl(r.faultEdgeDelay > 0, Validators.requiredTrue),
        }));
    });

    this.otherUserForm = {
      formGroup: formGroup,
      options: {},
      model: {},
      fields: [{
        key: "otherUsers",
        type: "input",
        props: {
          options: otherUserRows(response, this.defaultValues, this.translate),
        },
        wrappers: ["formly-other-users-alerting"],
      },
      ],
    };
  }

  private getValue(setting: AlertingSetting, type: AlertingType): number {
    switch (type) {
      case AlertingType.OFFLINE:
        return setting.offlineEdgeDelay;
      case AlertingType.FAULT:
        return setting.faultEdgeDelay;
      case AlertingType.WARNING:
        return setting.warningEdgeDelay;
    }
  }

  private getValueOrDefault(setting: AlertingSetting, type: AlertingType) {
    const val = this.getValue(setting, type);
    return val <= 0 ? this.defaultValues[type][0].value : val;
  }

  private asDetailedSettings(setting: AlertingSetting): DetailedAlertingSetting {
    return {
      userLogin: setting.userLogin,
      offlineEdgeDelay: this.getValueOrDefault(setting, AlertingType.OFFLINE),
      warningEdgeDelay: this.getValueOrDefault(setting, AlertingType.WARNING),
      faultEdgeDelay: this.getValueOrDefault(setting, AlertingType.FAULT),
      isOfflineActive: setting.offlineEdgeDelay > 0,
      isFaultActive: setting.faultEdgeDelay > 0,
      isWarningActive: setting.warningEdgeDelay > 0,
    };
  }

  private asDelayOptions(settings: number[]): Delay[] {
    return settings.map(e => this.asDelayOption(e));
  }

  private asDelayOption(setting: number): Delay {
    return { value: setting, label: this.getLabelToDelay(setting) };
  }

  /**
   * send requests, show events using toasts and reset given formGroup if successful.
   * @param request   stucture containing neccesary parameters
   * @param formGroup   formGroup to update
   * @returns @GetUserAlertingConfigsResponse containing logged in users data, as well as data other users, if user is admin
   */
  private sendRequestAndUpdate(request: GetUserAlertingConfigsRequest | SetUserAlertingConfigsRequest, formGroup: FormGroup<any>[]) {
    this.sendRequest(request)
      .then(() => {
        this.service.toast(this.translate.instant("General.changeAccepted"), "success");
        for (const group of formGroup.values()) {
          group.markAsPristine();
        }
      })
      .catch((response) => {
        const error = response.error;
        this.errorToast(this.translate.instant("General.changeFailed"), error.message);
      });
  }

  /**
   * send requests and show events using toasts.
   * @param request   stucture containing neccesary parameters
   * @returns @GetUserAlertingConfigsResponse containing logged in users data, as well as data other users, if user is admin
   */
  private sendRequest(request: GetUserAlertingConfigsRequest | SetUserAlertingConfigsRequest): Promise<GetUserAlertingConfigsResponse> {
    return new Promise((resolve, reject) => {
      this.service.startSpinner(this.spinnerId);
      this.websocket.sendRequest(request).then(response => {
        resolve(response as GetUserAlertingConfigsResponse);
      }).catch(reason => {
        const error = reason.error;
        this.errorToast(this.translate.instant("Edge.Config.ALERTING.TOAST.ERROR"), error.message);
        reject(reason);
      }).finally(() => {
        this.service.stopSpinner(this.spinnerId);
      });
    });
  }

  private errorToast(errorType: string, errorMsg: string) {
    this.service.toast("[ " + errorType + " ]<br/>" + errorMsg, "danger");
  }

  private getDelays(formGroup: FormGroup): Omit<UserSettingRequest, "userLogin"> {
    const offlineDelay = FormUtils.findFormControlsValueSafely<number>(formGroup, "offline-delay-selection");
    const isOfflineAlerting = FormUtils.findFormControlsValueSafely<boolean>(formGroup, "offline-toggle");
    const faultDelay = FormUtils.findFormControlsValueSafely<number>(formGroup, "fault-delay-selection");
    const isFaultAlerting = FormUtils.findFormControlsValueSafely<boolean>(formGroup, "fault-toggle");

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
