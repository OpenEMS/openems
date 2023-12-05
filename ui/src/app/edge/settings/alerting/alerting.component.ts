import { User } from 'src/app/shared/jsonrpc/shared';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig, FormlyFormOptions } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { SetUserAlertingConfigsRequest, UserSettingRequest } from 'src/app/shared/jsonrpc/request/setUserAlertingConfigsRequest';
import { GetUserAlertingConfigsRequest } from 'src/app/shared/jsonrpc/request/getUserAlertingConfigsRequest';
import { GetUserAlertingConfigsResponse, AlertingSettingResponse } from 'src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse';
import { Edge, Service, Utils, Websocket } from 'src/app/shared/shared';

enum AlertingType {
  offline = "O",
  fault = "F",
  warning = "W"
};

type DefaultDelayValue = { type: AlertingType, values: number[] };

type Delay = { value: number, label: string }
type DelayOption = { type: AlertingType, values: Delay[] }

type AlertingSetting = AlertingSettingResponse & { options: DelayOption[] }

@Component({
  selector: AlertingComponent.SELECTOR,
  templateUrl: './alerting.component.html',
})
export class AlertingComponent implements OnInit {
  protected static readonly SELECTOR = "alerting";
  public readonly spinnerId: string = AlertingComponent.SELECTOR;

  protected static readonly DEFAULT_DELAYS: DefaultDelayValue[] = [
    { type: AlertingType.offline, values: [15, 60, 1440]},
    { type: AlertingType.fault, values: [15, 60, 1440]},
    { type: AlertingType.warning, values: [15, 60, 1440]},
  ];

  protected edge: Edge;
  protected user: User;
  protected error: Error;

  protected currentUserInformation: AlertingSetting;
  protected currentUserForm: { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: FormlyFormOptions };

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

        this.currentUserInformation = this.combine(result.currentUserSettings, this.getDelayOptions(result.currentUserSettings));
        this.currentUserForm = this.generateFormFor(this.currentUserInformation);

        if (result.otherUsersSettings) {
          [this.otherUserInformation, this.otherUserForm] = this.generateSettings(result.otherUsersSettings);
        }
      }).catch(error => {
        this.error = error.error;
      });
    });
  }

  private combine(response: AlertingSettingResponse, options: DelayOption[]): AlertingSetting {
    return Object.assign({}, response, { options: options });
  }

  private getSetting(userSetting: AlertingSettingResponse, type: AlertingType): number {
    switch(type) {
      case AlertingType.offline: return userSetting.offlineEdgeDelay;
      case AlertingType.fault: return userSetting.faultEdgeDelay;
      case AlertingType.warning: return userSetting.warningEdgeDelay;
    }
  }

  private generateFormFor(userSettings: AlertingSettingResponse): { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: any } {
    let delays: DelayOption[] = this.getDelayOptions(userSettings);
    return {
      formGroup: new FormGroup({}),
      options: {
        formState: {
          awesomeIsForced: false,
        },
      },
      model: {
        faultAlerting: userSettings.faultEdgeDelay,
        warnAlerting: userSettings.warningEdgeDelay,
        offlineAlerting: userSettings.offlineEdgeDelay,
      },
      fields: [
      {
        key: 'offlineAlerting',
        type: 'radio',
        templateOptions: {
          label: this.translate.instant('Edge.Config.Alerting.delay'),
          type: 'number',
          required: true,
          options: delays[AlertingType.offline],
        },
      },
      {
        key: 'faultAlerting',
        type: 'radio',
        templateOptions: {
          label: this.translate.instant('Edge.Config.Alerting.delay'),
          type: 'number',
          required: true,
          options: delays[AlertingType.fault],
        },
      },
      {
        key: 'warnAlerting',
        type: 'radio',
        templateOptions: {
          label: this.translate.instant('Edge.Config.Alerting.delay'),
          type: 'number',
          required: true,
          options: delays[AlertingType.warning],
        },
      },
      ],
    };
  }

  private generateSettings(response: AlertingSettingResponse[]): [AlertingSetting[], FormGroup] {
    var settings: AlertingSetting[] = [];
    var form: FormGroup = new FormGroup({});

    var sorted = this.sortedAlphabetically(response);

    sorted.forEach((s) => {
      settings.push({
        userLogin: s.userLogin,
        offlineEdgeDelay: s.offlineEdgeDelay,
        faultEdgeDelay: s.faultEdgeDelay,
        warningEdgeDelay: s.warningEdgeDelay,
        options: this.getDelayOptions(s),
      });

      form.addControl(s.userLogin, this.formBuilder.group({
        offlineDelay: new FormControl(s.offlineEdgeDelay),
        errorDelay: new FormControl(s.faultEdgeDelay),
        warningDelay: new FormControl(s.warningEdgeDelay)
      }));
    })

    return [settings, form];
  }

  private sortedAlphabetically(userSettings: AlertingSettingResponse[]): AlertingSettingResponse[] {
    return userSettings.sort((userA, userB) => {
      return userA.userLogin.localeCompare(userB.userLogin, undefined, { sensitivity: 'accent' });
    });
  }

  private getDelayOptions(settings: AlertingSettingResponse): DelayOption[] {
    let delayOptions: DelayOption[] = this.Delays;

    delayOptions.forEach((option) => {
      option.values.push({value: 0, label: 'Deaktiviert'});
      let value = this.getSetting(settings, option.type)
      if (this.isInvalidDelay(option.type, value)) {
        option.values.push({ value: value, label: this.getLabelToDelay(value) });
      }
    });
    return delayOptions;
  }

  /**
   * get if given delay is valid
   */
  protected isInvalidDelay(type: AlertingType, delay: number): boolean {
    if (delay === 0) {
      return false;
    }
    return !AlertingComponent.DEFAULT_DELAYS[type].includes(delay);
  }

  /**
   * get list of delays with translated labels.
   */
  protected get Delays() : DelayOption[] {
    let options: DelayOption[] = []

    Object.values(AlertingType).forEach((type: AlertingType) => {
      var delayValues: number[] = AlertingComponent.DEFAULT_DELAYS.find(t => t.type == type).values;

      var delay: Delay[] = [];
      delayValues.forEach((v) => {
        delay.push({ value: v, label: this.getLabelToDelay(v) })
      });

      options[type] = delay;
    });
    
    return options
  }

  /**
   * get the label matching the given delay, with translated timeunits and
   * attention to writing differences and singular and plural.
   *
   * @param delay to generate label for
   * @returns label as string
   */
  private getLabelToDelay(delay: number): string {
    if (delay >= 1440) {
      delay = delay / 1440;
      return delay + ' ' + (delay == 1
        ? this.translate.instant("Edge.Config.Alerting.interval.day")
        : this.translate.instant("Edge.Config.Alerting.interval.days"));
    } else if (delay >= 60) {
      delay = delay / 60;
      return delay + ' ' + (delay == 1
        ? this.translate.instant("Edge.Config.Alerting.interval.hour")
        : this.translate.instant("Edge.Config.Alerting.interval.hours"));
    } else {
      return delay + ' ' + (delay == 1
        ? this.translate.instant("Edge.Config.Alerting.interval.minute")
        : this.translate.instant("Edge.Config.Alerting.interval.minutes"));
    }
  }

  protected setUsersAlertingConfig() {
    let edgeId: string = this.edge.id;

    let dirtyformGroups: FormGroup<any>[] = [];
    let changedUserSettings: UserSettingRequest[] = [];

    if (this.currentUserForm.formGroup.dirty) {
      dirtyformGroups.push(this.currentUserForm.formGroup);

      changedUserSettings.push({
        userLogin: this.currentUserInformation.userLogin,
        offlineEdgeDelay: this.currentUserForm.formGroup.controls['delayTime']?.value ?? 0,
        faultEdgeDelay: 0,
        warningEdgeDelay: 0,
      });
    }

    let userOptions: AlertingSetting[] = [];
    if (this.otherUserInformation) {
      if (this.otherUserForm.dirty) {
        dirtyformGroups.push(this.otherUserForm);

        for (let user of this.otherUserInformation) {
          let control = this.otherUserForm.controls[user.userLogin];
          if (control.dirty) {
            let delayTime = control.value['delayTime'];
            let isActivated = control.value['isActivated'];
            changedUserSettings.push({
              userLogin: user.userLogin,
              offlineEdgeDelay: isActivated ? delayTime : 0,
              warningEdgeDelay: 0,
              faultEdgeDelay: 0,
            });
            userOptions.push(user);
          }
        }
      }
    }

    let request = new SetUserAlertingConfigsRequest({ edgeId: edgeId, userSettings: changedUserSettings });
    this.sendRequestAndUpdate(request, dirtyformGroups);

    /* reset options for users with a non-default option.
    var defaultSettingsCount = this.Delays.size;
    userOptions.forEach(user => {
      if (user.options.size > defaultSettingsCount) {
        user.options = this.getDelayOptions(user.offlineEdgeDelay);
      }
    });*/
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
        for (let group of formGroup.values()) {
          group.markAsPristine();
        }
      })
      .catch((response) => {
        let error = response.error;
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
        let error = reason.error;
        console.error(error);
        this.errorToast(this.translate.instant('Edge.Config.Alerting.toast.error'), error.message);
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
    return this.currentUserForm?.formGroup.dirty || this.otherUserForm?.dirty;
  }
}
