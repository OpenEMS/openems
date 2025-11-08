import { Component, Input, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { Filter } from "../filter/filter.component";

export enum SumState {
    OK = "OK",
    INFO = "INFO",
    WARNING = "WARNING",
    FAULT = "FAULT",
}

@Component({
    selector: "oe-sum-state",
    template: `
  <ion-col class="sum-state-icon">
    @if (!isEdgeOnline) {
      <ion-icon name="cloud-offline-outline" color="danger"></ion-icon>
    } @else {
      @if (!isAtLeastInstaller) {
        <ion-icon color="primary" name="play-outline"></ion-icon>
      } @else {
        <ng-container class="sum-state-icon">
          @switch (sumState) {
            @case (SUM_STATE.OK) {
              <ion-icon color="success" name="checkmark-circle-outline"></ion-icon>
            }
            @case (SUM_STATE.INFO) {
              <ion-icon color="success" name="information-circle-outline"></ion-icon>
            }
            @case (SUM_STATE.WARNING) {
              <ion-icon color="warning" name="warning-outline"></ion-icon>
            }
            @case (SUM_STATE.FAULT) {
              <ion-icon color="danger" name="alert-circle-outline"></ion-icon>
            }
            @default {
              <ion-icon color="primary" name="play-outline"></ion-icon>
            }
          }
        </ng-container>
      }
    }


  </ion-col>
  `,
    styles: [`
  .sum-state-icon > ion-icon{
    font-size: 20pt !important;
}
  `],
    standalone: false,
})
export class SumStateComponent implements OnInit {

    @Input() protected sumState: SumState = SumState.OK;
    @Input() protected isEdgeOnline: boolean = false;
    protected isAtLeastInstaller: boolean = false;
    protected readonly SUM_STATE = SumState;

    constructor(private service: Service) { }

    ngOnInit() {
        const user = this.service.metadata?.value?.user ?? null;

        if (user) {
            this.isAtLeastInstaller = Role.isAtLeast(user.globalRole, Role.INSTALLER);
        }
    }
}

export const SUM_STATES = (translate: TranslateService): Filter => ({
    placeholder: translate.instant("GENERAL.SUM_STATE"),
    category: "sumState",
    options: [
        {
            name: "Ok",
            value: "ok",
        },
        {
            name: translate.instant("GENERAL.INFO"),
            value: "Info",
        },
        {
            name: translate.instant("GENERAL.WARNING"),
            value: "Warning",
        },
        {
            name: translate.instant("GENERAL.FAULT"),
            value: "Fault",
        },
    ],
    setAdditionalFilter: () => ({
        key: "isOnline",
        value: true,
    }),
});
