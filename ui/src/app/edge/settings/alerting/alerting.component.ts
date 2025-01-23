// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { GetUserAlertingConfigsRequest } from "src/app/shared/jsonrpc/request/getUserAlertingConfigsRequest";
import { SetUserAlertingConfigsRequest, UserSettingRequest } from "src/app/shared/jsonrpc/request/setUserAlertingConfigsRequest";
import { AlertingSettingResponse, GetUserAlertingConfigsResponse } from "src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";

export enum AlertingType {
  offline = 0,
  fault = 1,
  warning = 2,
}

type DefaultValues = { [K in AlertingType]: Delay[]; };
type Delay = { value: number, label: string };
type AlertingSetting = AlertingSettingResponse;
type DetailedAlertingSetting = AlertingSetting & { isOfflineActive: boolean, isFaultActive: boolean, isWarningActive: boolean };

@Component({
  selector: AlertingComponent.SELECTOR,
  templateUrl: "./alerting.component.html",
  standalone: false,
})
export class AlertingComponent implements OnInit {

  protected static readonly SELECTOR = "alerting";

  public readonly spinnerId: string = AlertingComponent.SELECTOR;

  protected readonly defaultValues: DefaultValues;
  protected AlertingType = AlertingType;

  protected edge: Edge;
  protected error: Error;
  protected currentUserInformation: DetailedAlertingSetting;
  protected currentUserForm: FormGroup;
  protected otherUserInformation: AlertingSetting[];
  protected otherUserForm: FormGroup;

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
    public formBuilder: FormBuilder,
  ) {
    this.defaultValues = {
      [AlertingType.offline]: this.asDelayOptions([15, 60, 1440]),
      [AlertingType.fault]: this.asDelayOptions([15, 60, 1440]),
      [AlertingType.warning]: this.asDelayOptions([15, 60, 1440]),
    };
  }

  public ngOnInit(): void {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;

      const request = new GetUserAlertingConfigsRequest({ edgeId: this.edge.id });

      this.sendRequest(request).then(response => {
        const result = response.result;

        this.setupCurrentUser(result.currentUserSettings);
        this.setupOtherUsers(result.otherUsersSettings);
      }).catch(error => {
        this.error = error.error;
      });
    });
  }

  /**
   * get if given delay is valid
   */
  protected isInvalidDelay(type: AlertingType, delay: number): boolean {
    if (delay <= 0) {
      return false;
    }
    return this.defaultValues[type].findIndex(e => e.value === delay) === -1;
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

    if (this.currentUserForm.dirty) {
      const formGroup = this.currentUserForm;
      dirtyformGroups.push(formGroup);

      const offlineEdgeDelay = this.currentUserInformation.isOfflineActive ?
        this.currentUserForm.controls["offlineEdgeDelay"].value : 0;
      const faultEdgeDelay = this.currentUserInformation.isFaultActive ?
        this.currentUserForm.controls["faultEdgeDelay"].value : 0;
      const warningEdgeDelay = this.currentUserInformation.isWarningActive ?
        this.currentUserForm.controls["warningEdgeDelay"].value : 0;

      changedUserSettings.push({
        userLogin: this.currentUserInformation.userLogin,
        offlineEdgeDelay: offlineEdgeDelay,
        warningEdgeDelay: warningEdgeDelay,
        faultEdgeDelay: faultEdgeDelay,
      });
    }

    const userOptions: AlertingSetting[] = [];
    if (this.otherUserInformation) {
      if (this.otherUserForm.dirty) {
        dirtyformGroups.push(this.otherUserForm);

        for (const user of this.otherUserInformation) {
          const control = this.otherUserForm.controls[user.userLogin];
          if (control.dirty) {
            const offlineEdgeDelay = control.value["offlineEdgeDelay"];
            const faultEdgeDelay = control.value["faultEdgeDelay"];
            const warningEdgeDelay = control.value["warningEdgeDelay"];
            //let isActivated = control.value['isActivated'];
            changedUserSettings.push({
              userLogin: user.userLogin,
              offlineEdgeDelay: offlineEdgeDelay,
              warningEdgeDelay: warningEdgeDelay,
              faultEdgeDelay: faultEdgeDelay,
            });
            userOptions.push(user);
          }
        }
      }
    }

    console.log(changedUserSettings);

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
    return this.currentUserForm?.dirty || this.otherUserForm?.dirty;
  }

  protected loadOtherUsers(): void {
    console.info("TEST");
  }

  private setupCurrentUser(response: AlertingSettingResponse) {
    this.currentUserInformation = this.asDetailedSettings(response);
    this.currentUserForm = this.formBuilder.group({
      isOfflineActive: new FormControl(this.currentUserInformation.isOfflineActive),
      offlineEdgeDelay: new FormControl(this.currentUserInformation.offlineEdgeDelay),
      isFaultActive: new FormControl(this.currentUserInformation.isFaultActive),
      faultEdgeDelay: new FormControl(this.currentUserInformation.faultEdgeDelay),
      isWarningActive: new FormControl(this.currentUserInformation.isWarningActive),
      warningEdgeDelay: new FormControl(this.currentUserInformation.warningEdgeDelay),
    });
  }

  private setupOtherUsers(response: AlertingSettingResponse[]) {
    if (!response || response.length == 0) {
      return;
    }

    this.otherUserInformation = [];
    this.otherUserForm = new FormGroup({});

    const sorted = this.sortedAlphabetically(response);

    sorted.forEach((r) => {
      const setting: AlertingSetting = {
        userLogin: r.userLogin,
        offlineEdgeDelay: r.offlineEdgeDelay,
        faultEdgeDelay: r.faultEdgeDelay,
        warningEdgeDelay: r.warningEdgeDelay,
      };

      this.otherUserInformation.push(setting);

      this.otherUserForm.addControl(setting.userLogin, //
        this.formBuilder.group({
          offlineEdgeDelay: new FormControl(setting.offlineEdgeDelay),
          faultEdgeDelay: new FormControl(setting.faultEdgeDelay),
          warningEdgeDelay: new FormControl(setting.warningEdgeDelay),
        }));
    });
  }

  private getValue(setting: AlertingSetting, type: AlertingType): number {
    switch (type) {
      case AlertingType.offline:
        return setting.offlineEdgeDelay;
      case AlertingType.fault:
        return setting.faultEdgeDelay;
      case AlertingType.warning:
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
      offlineEdgeDelay: this.getValueOrDefault(setting, AlertingType.offline),
      warningEdgeDelay: this.getValueOrDefault(setting, AlertingType.warning),
      faultEdgeDelay: this.getValueOrDefault(setting, AlertingType.fault),
      isOfflineActive: setting.offlineEdgeDelay > 0,
      isFaultActive: setting.faultEdgeDelay > 0,
      isWarningActive: setting.warningEdgeDelay > 0,
    };
  }

  private sortedAlphabetically(userSettings: AlertingSettingResponse[]): AlertingSettingResponse[] {
    return userSettings.sort((userA, userB) => {
      return userA.userLogin.localeCompare(userB.userLogin, undefined, { sensitivity: "accent" });
    });
  }

  private asDelayOptions(settings: number[]): Delay[] {
    return settings.map(v => this.asDelayOption(v));
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
        console.error(error);
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
}
