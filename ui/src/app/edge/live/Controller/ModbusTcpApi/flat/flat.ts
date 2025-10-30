import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { OverrideStatus } from "src/app/shared/type/general";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Api_ModbusTcp",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  protected overrideStatus: OverrideStatus | null = null;
  protected modalComponent: Modal | null = null;

  protected override afterIsInitialized(): void {
    this.modalComponent = this.getModalComponent();
  }
  protected getModalComponent(): Modal {
    return {
      component: ModalComponent,
      componentProps: {
        component: this.component,
      },
    };
  };

  protected override getChannelAddresses(): ChannelAddress[] {

    return [
      new ChannelAddress(this.component.id, "OverrideStatus"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.overrideStatus = this.getTranslatedState(currentData.allComponents[this.component.id + "/OverrideStatus"]);
  }

  private getTranslatedState(state: OverrideStatus) {
    switch (state) {
      case OverrideStatus.ACTIVE:
        return this.translate.instant("MODBUS_TCP_API_READ_WRITE.OVERRIDING");
      case OverrideStatus.ERROR:
        return this.translate.instant("EVCS.error");
      default:
        return this.translate.instant("MODBUS_TCP_API_READ_WRITE.NOT_OVERRIDING");
    }
  }

}
