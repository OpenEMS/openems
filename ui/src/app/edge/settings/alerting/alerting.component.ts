import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig, FormlyFormOptions } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { SetUserAlertingConfigsRequest } from 'src/app/shared/jsonrpc/request/setUserAlertingConfigsRequest';
import { GetUserAlertingConfigsRequest } from 'src/app/shared/jsonrpc/request/getUserAlertingConfigsRequest';
import { GetUserAlertingConfigsResponse, UserSettingResponse } from 'src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse';
import { User } from 'src/app/shared/jsonrpc/shared';
import { Role } from 'src/app/shared/type/role';
import { Edge, Service, Utils, Websocket } from 'src/app/shared/shared';

export type UserSetting = { userId: string, delayTime: number }

type Delay = { value: number, label: string }
type UserSettingOptions = UserSetting & { options: Delay[] }
type UserSettingRole = UserSetting & { role: Role };
type RoleUsersSettings = { role: Role, form: FormGroup, settings: UserSettingOptions[] }

@Component({
  selector: AlertingComponent.SELECTOR,
  templateUrl: './alerting.component.html'
})
export class AlertingComponent implements OnInit {
  protected static readonly SELECTOR = "alerting";
  public readonly spinnerId: string = AlertingComponent.SELECTOR;
  protected static readonly DELAYS: number[] = [15, 60, 1440];

  protected edge: Edge;
  protected user: User;
  protected error: Error;

  protected currentUserInformation: UserSettingRole;
  protected currentUserForm: { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: FormlyFormOptions };

  protected otherUserSettings: RoleUsersSettings[];

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
    public formBuilder: FormBuilder
  ) { }

  public ngOnInit(): void {
    this.service.startSpinner(this.spinnerId);

    this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.alerting' }, this.route).then(edge => {
      this.edge = edge;

      this.service.metadata.subscribe(metadata => {
        this.user = metadata.user;
      });

      let request = new GetUserAlertingConfigsRequest({ edgeId: this.edge.id });

      this.sendRequest(request).then(response => {
        const result = response.result;
        this.findRemoveAndSetCurrentUser(result.userSettings);
        if (edge?.roleIsAtLeast('admin')) {
          this.setRemainingUserSettings(result.userSettings);
        }
      }).catch(error => {
        this.error = error.error;
      }).finally(() => {
        this.service.stopSpinner(this.spinnerId);
      });
    });
  }

  private findRemoveAndSetCurrentUser(userSettings: UserSettingResponse[]) {
    let currentUserIndex = userSettings.findIndex(setting => setting.userId === this.user.id);

    if (currentUserIndex != -1) {
      this.currentUserInformation = userSettings.splice(currentUserIndex, 1)[0];
    } else {
      this.currentUserInformation = { delayTime: 0, userId: this.user.id, role: Role.getRole(this.user.globalRole) };
    }
    this.currentUserForm = this.generateFormFor(this.currentUserInformation);
  }

  private generateFormFor(userSettings: UserSettingRole): { formGroup: FormGroup, model: any, fields: FormlyFieldConfig[], options: any } {
    let delays: Delay[] = this.Delays;
    if (this.isInvalidDelay(userSettings.delayTime)) {
      delays.push({ value: userSettings.delayTime, label: this.getLabelToDelay(userSettings.delayTime) });
    }
    return {
      formGroup: new FormGroup({}),
      options: {
        formState: {
          awesomeIsForced: false,
        }
      },
      model: {
        isActivated: userSettings.delayTime > 0,
        delayTime: userSettings.delayTime
      },
      fields: [{
        key: 'isActivated',
        type: 'checkbox',
        templateOptions: {
          label: this.translate.instant('Edge.Config.Alerting.activate'),
        },
      },
      {
        key: 'delayTime',
        type: 'radio',
        templateOptions: {
          label: this.translate.instant('Edge.Config.Alerting.delay'),
          type: 'number',
          required: true,
          options: delays,
        },
        hideExpression: model => !model.isActivated,
      }
      ],
    };
  }

  private setRemainingUserSettings(userSettings: UserSettingResponse[]) {
    if (!userSettings || userSettings.length === 0) {
      return;
    }

    userSettings = this.sortedAlphabetically(userSettings);
    let otherUserSettings: RoleUsersSettings[] = [];

    userSettings.forEach(userSetting => {
      let roleSettings = this.findOrGetNew(otherUserSettings, userSetting.role);
      this.addUserToRoleUserSettings(userSetting, roleSettings);
    });
    this.otherUserSettings = this.sortedByRole(otherUserSettings);
  }

  private sortedAlphabetically(userSettings: UserSettingResponse[]): UserSettingResponse[] {
    return userSettings.sort((userA, userB) => {
      return userA.userId.localeCompare(userB.userId, undefined, { sensitivity: 'accent' });
    });
  }

  /**
   * get entry for role. if no entry is found genarate a new one.
   * eather way return the value.
   *
   * @param from array to search in
   * @param role the role to search for
   * @returns the found/created setting
   */
  private findOrGetNew(from: RoleUsersSettings[], role: Role): RoleUsersSettings {
    let roleSettings = from.find(setting => setting.role === role);
    if (!roleSettings) {
      roleSettings = { role: role, settings: [], form: new FormGroup({}) };
      from.push(roleSettings);
    }
    return roleSettings;
  }

  /**
   * add a user to a roleUserSetting.
   *
   * @param userSetting user to add
   * @param roleSetting settings to add user to
   */
  private addUserToRoleUserSettings(userSetting: UserSettingResponse, roleSetting: RoleUsersSettings) {
    let activated = userSetting.delayTime > 0;
    let delay = userSetting.delayTime == 0 ? 15 : userSetting.delayTime;
    roleSetting.settings.push({ userId: userSetting.userId, delayTime: userSetting.delayTime, options: this.getDelayOptions(delay) });
    roleSetting.form.addControl(userSetting.userId, this.formBuilder.group({
      isActivated: new FormControl(activated),
      delayTime: new FormControl(delay),
    }));
  }

  private getDelayOptions(delay: number): Delay[] {
    let delays: Delay[] = this.Delays;
    if (this.isInvalidDelay(delay)) {
      delays.push({ value: delay, label: this.getLabelToDelay(delay) });
    }
    return delays;
  }

  private sortedByRole(roleUsersSettings: RoleUsersSettings[]): RoleUsersSettings[] {
    return roleUsersSettings.sort((settingA, settingB) => {
      return Role.isAtLeast(settingA.role, settingB.role) ? 1 : -1;
    });
  }

  /**
   * get if given delay is valid
   */
  protected isInvalidDelay(delay: number): boolean {
    if (delay === 0) {
      return false;
    }
    return !AlertingComponent.DELAYS.includes(delay);
  }

  /**
   * get list of delays with translated labels.
   */
  protected get Delays() {
    return AlertingComponent.DELAYS.map((delay) => { return { value: delay, label: this.getLabelToDelay(delay) }; });
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
    let changedUserSettings: UserSetting[] = [];

    if (this.currentUserForm.formGroup.dirty) {
      dirtyformGroups.push(this.currentUserForm.formGroup);

      changedUserSettings.push({
        delayTime: this.currentUserForm.formGroup.controls['delayTime']?.value ?? 0,
        userId: this.currentUserInformation.userId,
      });
    }

    let userOptions: UserSettingOptions[] = [];
    if (this.otherUserSettings) {
      for (let setting of this.otherUserSettings) {
        if (setting.form.dirty) {
          dirtyformGroups.push(setting.form);

          for (let user of setting.settings) {
            let control = setting.form.controls[user.userId];
            if (control.dirty) {
              let delayTime = control.value['delayTime'];
              let isActivated = control.value['isActivated'];
              changedUserSettings.push({
                delayTime: isActivated ? delayTime : 0,
                userId: user.userId,
              });
              userOptions.push(user);
            }
          }
        }
      }
    }

    let request = new SetUserAlertingConfigsRequest({ edgeId: edgeId, userSettings: changedUserSettings });
    this.sendRequestAndUpdate(request, dirtyformGroups);

    // reset options for users with a non-default option.
    var defaultSettingsCount = this.Delays.length;
    userOptions.forEach(user => {
      if (user.options.length > defaultSettingsCount) {
        user.options = this.getDelayOptions(user.delayTime);
      }
    });
  }

  /**
   * send requests, show events using toasts and reset given formGroup if successful.
   * @param request   stucture containing neccesary parameters
   * @param formGroup   formGroup to update
   * @returns @GetUserAlertingConfigsResponse containing logged in users data, as well as data other users, if user is admin
   */
  private sendRequestAndUpdate(request: GetUserAlertingConfigsRequest | SetUserAlertingConfigsRequest, formGroup: FormGroup<any>[]) {
    this.service.startSpinner(this.spinnerId);
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
      })
      .finally(() => {
        this.service.stopSpinner(this.spinnerId);
      });
  }

  /**
   * send requests and show events using toasts.
   * @param request   stucture containing neccesary parameters
   * @returns @GetUserAlertingConfigsResponse containing logged in users data, as well as data other users, if user is admin
   */
  private sendRequest(request: GetUserAlertingConfigsRequest | SetUserAlertingConfigsRequest): Promise<GetUserAlertingConfigsResponse> {
    return new Promise((resolve, reject) => {
      this.websocket.sendRequest(request).then(response => {
        resolve(response as GetUserAlertingConfigsResponse);
      }).catch(reason => {
        let error = reason.error;
        console.error(error);
        this.errorToast(this.translate.instant('Edge.Config.Alerting.toast.error'), error.message);
        reject(reason);
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
    return this.currentUserForm?.formGroup.dirty || this.otherUserSettings?.findIndex(setting => setting.form.dirty) != -1;
  }
}
