import { Component, Input, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Filter } from "../filter/filter.component";
import { Role } from "src/app/shared/type/role";
import { Service } from "src/app/shared/shared";

export enum SumState {
  OK = 'OK',
  INFO = 'INFO',
  WARNING = 'WARNING',
  FAULT = 'FAULT',
}

@Component({
  selector: 'oe-sum-state',
  template: `
  <ion-col class="sum-state-icon">
    <ng-container *ngIf="!isEdgeOnline, else showSystemState">
      <ion-icon name="cloud-offline-outline" color="danger"></ion-icon>
    </ng-container>

    <ng-template #showSystemState>
      <ng-container *ngIf="!isAtLeastInstaller, else showAllStates">
        <ion-icon color="primary" name="play-outline"></ion-icon>
      </ng-container>
    </ng-template>

      <ng-template #showAllStates>
          <ng-container [ngSwitch]="sumState" class="sum-state-icon">
            <ion-icon *ngSwitchCase="SUM_STATE.OK" color="success" name="checkmark-circle-outline"></ion-icon>
            <ion-icon *ngSwitchCase="SUM_STATE.INFO" color="success" name="information-circle-outline"></ion-icon>
            <ion-icon *ngSwitchCase="SUM_STATE.WARNING" color="warning" name="alert-circle-outline"></ion-icon>
            <ion-icon *ngSwitchCase="SUM_STATE.FAULT" color="danger" name="alert-circle-outline"></ion-icon>
            <ion-icon *ngSwitchDefault color="primary" name="play-outline"></ion-icon>
          </ng-container>
        </ng-template>
  </ion-col>
  `,
  styles: [`
  .sum-state-icon > ion-icon{
    font-size: 20pt !important;
}
  `],
})
export class SumStateComponent implements OnInit {

  protected readonly SUM_STATE = SumState;
  @Input() protected sumState: SumState = SumState.OK;
  @Input() protected isEdgeOnline: boolean = false;
  protected isAtLeastInstaller: boolean = false;

  constructor(private service: Service) { }

  ngOnInit() {
    const user = this.service.metadata?.value?.user ?? null;

    if (user) {
      this.isAtLeastInstaller = Role.isAtLeast(user.globalRole, Role.INSTALLER);
    }
  }
}

export const SUM_STATES = (translate: TranslateService): Filter => ({
  placeholder: translate.instant("General.SUM_STATE"),
  category: "sumState",
  options: [
    {
      name: 'Ok',
      value: "ok",
    },
    {
      name: translate.instant('General.info'),
      value: "Info",
    },
    {
      name: translate.instant('General.warning'),
      value: "Warning",
    },
    {
      name: translate.instant("General.fault"),
      value: "Fault",
    },
  ],
  setAdditionalFilter: () => ({
    key: 'isOnline',
    value: true,
  }),
});
