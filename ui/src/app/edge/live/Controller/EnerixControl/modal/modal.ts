import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";

@Component({
  templateUrl: "./modal.html",
  standalone: false,
})
export class ModalComponent extends AbstractModal {

  private static PROPERTY_MODE: string = "_PropertyMode";
  private static PROPERTY_READ_ONLY: string = "_PropertyReadOnly";

  protected readonly CONVERT_ENERIX_CONTROL_STATE = Converter.CONVERT_ENERIX_CONTROL_STATE(this.translate);

  protected propertyMode: string | null = null;
  protected controlModeNumber: number | null = null;
  protected controlMode: State | null = null;
  protected readOnly: boolean | null = true;
  protected overwriteLabel: string | null = null;
  protected unableToSend: boolean | null = null;

  protected override getChannelAddresses(): ChannelAddress[] {
    if (!this.component) { return []; }

    const channelAddresses: ChannelAddress[] = [
      new ChannelAddress(this.component.id, ModalComponent.PROPERTY_MODE),
      new ChannelAddress(this.component.id, ModalComponent.PROPERTY_READ_ONLY),
      new ChannelAddress(this.component.id, "ControlMode"),
      new ChannelAddress(this.component.id, "UnableToSend"),
    ];

    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData) {
    if (!this.component) { return []; }

    this.propertyMode = currentData.allComponents[this.component.id + "/" + ModalComponent.PROPERTY_MODE];
    this.controlModeNumber = currentData.allComponents[this.component.id + "/ControlMode"];
    this.readOnly = currentData.allComponents[this.component.id + "/" + ModalComponent.PROPERTY_READ_ONLY];
    this.unableToSend = currentData.allComponents[this.component.id + "/UnableToSend"];

    if (this.readOnly) {
      this.controlMode = this.unableToSend ? State.disconnected : State.connected;
      return;
    } else {
      if (this.controlModeNumber === null) {
        return;
      }
      switch (this.controlModeNumber) {
        case ControlMode.idle:
          this.controlMode =
            this.component.properties.mode === "REMOTE_CONTROL" ? State.on : State.off;
          break;
        case ControlMode.noDischarge:
          this.controlMode = State.noDischarge;
          break;
        case ControlMode.forceCharge:
          this.controlMode = State.forceCharge;
          break;
        default:
          this.controlMode = State.off;
          break;
      }

      if (this.controlModeNumber != ControlMode.idle) {
        this.overwriteLabel = this.translate.instant("Edge.Index.Widgets.ENERIX_CONTROL.OVERWRITE");
      } else {
        this.overwriteLabel = this.translate.instant("Edge.Index.Widgets.ENERIX_CONTROL.NO_OVERWRITE");
      }
    }
  }

  protected override getFormGroup(): FormGroup {
    return this.formBuilder.group({
      mode: new FormControl(this.component?.properties.mode),
    });
  }
}

enum ControlMode {
  idle,
  noDischarge,
  forceCharge,
}

enum State {
  on,
  off,
  noDischarge,
  forceCharge,
  disconnected,
  connected,
}
