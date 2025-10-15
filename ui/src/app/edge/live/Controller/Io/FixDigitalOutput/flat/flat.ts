// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";


@Component({
  selector: "Controller_Io_FixDigitalOutput",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public state: string = "-";
  public outputChannel: string;
  protected modalComponent: Modal | null = null;
  protected override afterIsInitialized(): void {
    this.modalComponent = this.getModalComponent();
  }

  protected getModalComponent(): Modal {
    return {
      component: ModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge,
      },
    };
  };

  protected override getChannelAddresses(): ChannelAddress[] {
    this.outputChannel = this.component.properties["outputChannelAddress"];
    return [ChannelAddress.fromString(this.outputChannel)];
  }

  protected override onCurrentData(currentData: CurrentData) {
    const channel = currentData.allComponents[this.outputChannel];
    if (channel != null) {
      if (channel == 1) {
        this.state = this.translate.instant("General.on");
      } else if (channel == 0) {
        this.state = this.translate.instant("General.off");
      }
    }
  }

}
