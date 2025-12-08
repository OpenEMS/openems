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

    private static PROPERTY_CONTROL_MODE: string = "_PropertyControlMode";
    private static PROPERTY_READ_ONLY: string = "_PropertyReadOnly";

    protected readonly CONVERT_ENERIX_CONTROL_STATE = Converter.CONVERT_ENERIX_CONTROL_STATE(this.translate);
    protected propertyMode: string | null = null;
    protected controlMode: ControlMode | null = null;
    protected state: State | null = null;
    protected readOnly: boolean | null = true;
    protected overwriteLabel: string | null = null;
    protected unableToSend: boolean | null = null;
    protected isEssChargeFromGridAllowed: boolean | null = null;


    protected override getChannelAddresses(): ChannelAddress[] {
        if (!this.component) { return []; }

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, ModalComponent.PROPERTY_CONTROL_MODE),
            new ChannelAddress(this.component.id, ModalComponent.PROPERTY_READ_ONLY),
            new ChannelAddress(this.component.id, "RemoteControlMode"),
            new ChannelAddress(this.component.id, "UnableToSend"),
            new ChannelAddress("_meta", "IsEssChargeFromGridAllowed"),
        ];

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (!this.component) { return []; }

        this.propertyMode = currentData.allComponents[this.component.id + "/" + ModalComponent.PROPERTY_CONTROL_MODE];
        this.controlMode = currentData.allComponents[this.component.id + "/RemoteControlMode"];
        this.readOnly = currentData.allComponents[this.component.id + "/" + ModalComponent.PROPERTY_READ_ONLY];
        this.unableToSend = currentData.allComponents[this.component.id + "/UnableToSend"];
        this.isEssChargeFromGridAllowed = currentData.allComponents["_meta" + "/IsEssChargeFromGridAllowed"];

        if (this.readOnly) {
            this.state = this.unableToSend ? State.disconnected : State.connected;
            return;
        } else {
            if (this.controlMode === null) {
                return;
            }
            switch (this.controlMode) {
                case ControlMode.idle:
                    this.state =
                        this.component.properties.controlMode === "REMOTE_CONTROL" ? State.on : State.off;
                    break;
                case ControlMode.noDischarge:
                    this.state = State.noDischarge;
                    break;
                case ControlMode.forceCharge:
                    this.state = State.forceCharge;
                    break;
                default:
                    this.state = State.off;
                    break;
            }

            if (this.controlMode != ControlMode.idle) {
                this.overwriteLabel = this.translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.OVERWRITE");
            } else {
                this.overwriteLabel = this.translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_OVERWRITE");
            }
        }
    }

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            controlMode: new FormControl(this.component?.properties.controlMode),
        });
    }

    protected toggleIsEssChargeFromGridAllowed(event: CustomEvent) {
        this.service.getCurrentEdge()
            .then(edge =>
                edge.updateComponentConfig(this.websocket, "_meta", [{
                    name: "isEssChargeFromGridAllowed", value: event.detail["checked"],
                }]).then(() => {
                    this.service.toast(this.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
                }).catch((reason) => {
                    this.service.toast(this.translate.instant("GENERAL.CHANGE_FAILED") + "\n" + reason.error.message, "danger");
                }));
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
