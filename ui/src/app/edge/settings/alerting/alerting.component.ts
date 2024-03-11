import { User } from 'src/app/shared/jsonrpc/shared';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { FormlyFieldConfig, FormlyFormOptions } from '@ngx-formly/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { SetUserAlertingConfigsRequest, UserSettingRequest } from 'src/app/shared/jsonrpc/request/setUserAlertingConfigsRequest';
import { GetUserAlertingConfigsRequest } from 'src/app/shared/jsonrpc/request/getUserAlertingConfigsRequest';
import { GetUserAlertingConfigsResponse, AlertingSettingResponse } from 'src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse';
import { Edge, Service, Utils, Websocket } from 'src/app/shared/shared';
import { ArrayUtils } from 'src/app/shared/service/arrayutils';

export enum AlertingType {
  OFFLINE,
  FAULT,
  WARNING
};

type DefaultValues = { [K in AlertingType]: Delay[]; };
type Delay = { value: number, label: string };
type AlertingSetting = AlertingSettingResponse;
type DetailedAlertingSetting = AlertingSetting & { isOfflineActive: boolean, isFaultActive: boolean, isWarningActive: boolean };

@Component({
  selector: AlertingComponent.SELECTOR,
  templateUrl: './alerting.component.html',
})
export class AlertingComponent implements OnInit {
  private static readonly NO_ALERTING: number = 0;
  protected AlertingType = AlertingType;

  protected static readonly SELECTOR = "alerting";
  public readonly spinnerId: string = AlertingComponent.SELECTOR;

  protected readonly defaultValues: DefaultValues = {
    [AlertingType.OFFLINE]: this.asDelayOptions([15, 60, 1440]),
    [AlertingType.FAULT]: this.asDelayOptions([15, 60, 1440]),
    [AlertingType.WARNING]: this.asDelayOptions([15, 60, 1440]),
  };

  protected edge: Edge;
  protected user: User;
  protected error: Error;

  protected currentUserInformation: DetailedAlertingSetting;
  protected currentUserForm: { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: FormlyFormOptions };;

  protected otherUserInformation: AlertingSetting[];
  protected otherUserForm: FormGroup;

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
    public formBuilder: FormBuilder,
  ) { }

  public ngOnInit(): void {
    this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.alerting' }, this.route).then(edge => {
      this.edge = edge;

      this.service.metadata.subscribe(metadata => {
        this.user = metadata.user;
      });

      let request = new GetUserAlertingConfigsRequest({ edgeId: this.edge.id });

      this.sendRequest(request).then(response => {
        const result = response.result;

        this.setupCurrentUser(result.currentUserSettings);
        this.setupOtherUsers(result.otherUsersSettings);
      }).catch(error => {
        this.error = error.error;
      });
    });
  }

  private setupCurrentUser(response: AlertingSettingResponse) {
    this.currentUserInformation = this.asDetailedSettings(response);
    this.currentUserForm = this.generateForm(this.currentUserInformation);
  }

  private generateForm(settings: DetailedAlertingSetting): { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: any } {
    let delays: Delay[] = this.defaultValues[AlertingType.OFFLINE];
    if (!this.isValidDelay(AlertingType.OFFLINE, settings.offlineEdgeDelay)) {
      delays.push({ value: settings.offlineEdgeDelay, label: this.getLabelToDelay(settings.offlineEdgeDelay) });
    }
    return {
      formGroup: new FormGroup({}),
      options: {
        formState: {
          awesomeIsForced: false,
        },
      },
      model: {
        isOfflineActive: settings.isOfflineActive,
        offlineEdgeDelay: settings.offlineEdgeDelay,
      },
      fields: [{
        key: 'isOfflineActive',
        type: 'checkbox',
        templateOptions: {
          label: this.translate.instant('Edge.Config.ALERTING.ACTIVATE'),
        },
      },
      {
        key: 'offlineEdgeDelay',
        type: 'radio',
        templateOptions: {
          label: this.translate.instant('Edge.Config.ALERTING.DELAY'),
          type: 'number',
          required: true,
          options: delays,
        },
        hideExpression: model => !model.isOfflineActive,
      },
      ],
    };
  }

  private setupOtherUsers(response: AlertingSettingResponse[]) {
    if (!response || response.length === 0) {
      return;
    }

    this.otherUserInformation = [];
    this.otherUserForm = new FormGroup({});

    const sorted = ArrayUtils.sortedAlphabetically(response, e => e.userLogin);

    sorted.forEach((r) => {
      var setting: AlertingSetting = {
        userLogin: r.userLogin,
        offlineEdgeDelay: r.offlineEdgeDelay,
        faultEdgeDelay: r.faultEdgeDelay,
        warningEdgeDelay: r.warningEdgeDelay,
      };

      this.otherUserInformation.push(setting);

      this.otherUserForm.addControl(setting.userLogin, //
        this.formBuilder.group({
          isOfflineActive: new FormControl(setting.offlineEdgeDelay > 0),
          offlineEdgeDelay: new FormControl(this.getValueOrDefault(setting, AlertingType.OFFLINE)),
          faultEdgeDelay: new FormControl(setting.faultEdgeDelay),
          warningEdgeDelay: new FormControl(setting.warningEdgeDelay),
        }));
    });
  }

  private getValue(setting: AlertingSetting, type: AlertingType): number {
    switch (type) {
      case AlertingType.OFFLINE:
        return setting.offlineEdgeDelay;
      case AlertingType.FAULT:
        return setting.faultEdgeDelay;
      case AlertingType.WARNING:
        return setting.warningEdgeDelay;
      default:
        return AlertingComponent.NO_ALERTING;
    }
  }

  private getValueOrDefault(setting: AlertingSetting, type: AlertingType) {
    let val = this.getValue(setting, type);
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
      return delay + ' ' + (delay == 1
        ? this.translate.instant("General.TIME.DAY")
        : this.translate.instant("General.TIME.DAYS"));
    } else if (delay >= 60) {
      delay = delay / 60;
      return delay + ' ' + (delay == 1
        ? this.translate.instant("General.TIME.HOUR")
        : this.translate.instant("General.TIME.HOURS"));
    } else {
      return delay + ' ' + (delay == 1
        ? this.translate.instant("General.TIME.MINUTE")
        : this.translate.instant("General.TIME.MINUTES"));
    }
  }

  protected setUsersAlertingConfig() {
    const edgeId: string = this.edge.id;

    const dirtyformGroups: FormGroup<any>[] = [];
    const changedUserSettings: UserSettingRequest[] = [];

    if (this.currentUserForm.formGroup.dirty) {
      const formGroup = this.currentUserForm.formGroup;
      dirtyformGroups.push(formGroup);

      let offlineEdgeDelay = formGroup.controls['isOfflineActive'].value ?
        formGroup.controls['offlineEdgeDelay'].value : 0;
      let faultEdgeDelay = this.currentUserInformation.isFaultActive ?
        this.currentUserInformation.faultEdgeDelay : 0;
      let warningEdgeDelay = this.currentUserInformation.isWarningActive ?
        this.currentUserInformation.warningEdgeDelay : 0;

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
            const offlineEdgeDelay = control.value['offlineEdgeDelay'];
            const faultEdgeDelay = control.value['faultEdgeDelay'];
            const warningEdgeDelay = control.value['warningEdgeDelay'];
            const isActivated = control.value['isOfflineActive'];

            changedUserSettings.push({
              userLogin: user.userLogin,
              offlineEdgeDelay: isActivated ? offlineEdgeDelay : 0,
              warningEdgeDelay: warningEdgeDelay,
              faultEdgeDelay: faultEdgeDelay,
            });
            userOptions.push(user);
          }
        }
      }
    }

    const request = new SetUserAlertingConfigsRequest({ edgeId: edgeId, userSettings: changedUserSettings });
    this.sendRequestAndUpdate(request, dirtyformGroups);
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
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
        for (const group of formGroup.values()) {
          group.markAsPristine();
        }
      })
      .catch((response) => {
        const error = response.error;
        this.errorToast(this.translate.instant('General.changeFailed'), error.message);
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
        this.errorToast(this.translate.instant('Edge.Config.ALERTING.TOAST.ERROR'), error.message);
        reject(reason);
      }).finally(() => {
        this.service.stopSpinner(this.spinnerId);
      });
    });
  }

  private errorToast(errorType: string, errorMsg: string) {
    this.service.toast('[ ' + errorType + ' ]<br/>' + errorMsg, 'danger');
  }

  /**
   * get if any userSettings has changed/is dirty.
   * @returns true if any settings are changed, else false
   */
  protected isDirty(): boolean {
    if (this.error || !this.currentUserForm) {
      return false;
    }
    return this.currentUserForm?.formGroup?.dirty || this.otherUserForm?.dirty;
  }
}
