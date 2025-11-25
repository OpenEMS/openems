import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { Modal } from "src/app/shared/components/flat/flat";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Controller_EnerixControl",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    private static PROPERTY_READ_ONLY: string = "_PropertyReadOnly";
    protected readonly CONVERT_ENERIX_CONTROL_STATE = Converter.CONVERT_ENERIX_CONTROL_STATE(this.translate);

    protected controlMode: ControlMode | null = null;
    protected state: State | null = null;
    protected readOnly: boolean | null = null;
    protected overwriteLabel: string | null = null;
    protected unableToSend: boolean | null = null;
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
        if (!this.component) { return []; }

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, FlatComponent.PROPERTY_READ_ONLY),
            new ChannelAddress(this.component.id, "RemoteControlMode"),
            new ChannelAddress(this.component.id, "UnableToSend"),
        ];

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        const id = this.component.id;
        const data = currentData.allComponents;

        this.readOnly = data[`${id}/${FlatComponent.PROPERTY_READ_ONLY}`];
        this.controlMode = data[`${id}/RemoteControlMode`];
        this.unableToSend = data[`${id}/UnableToSend`];

        if (this.readOnly) {
            this.state = this.unableToSend ? State.DISCONNECTED : State.CONNECTED;
            return;
        } else {

            if (this.controlMode === null) {
                return;
            }

            this.state = this.mapControlMode(this.controlMode);
            this.overwriteLabel = this.getOverwriteLabel(this.controlMode);
        }
    }

    private mapControlMode(mode: ControlMode): State {
        switch (mode) {
            case ControlMode.IDLE:
                return this.component.properties.controlMode === "REMOTE_CONTROL"
                    ? State.ON
                    : State.OFF;
            case ControlMode.NO_DISCHARGE:
                return State.NO_DISCHARGE;
            case ControlMode.FORCE_CHARGE:
                return State.FORCE_CHARGE;
            default:
                return State.OFF;
        }
    }

    private getOverwriteLabel(mode: ControlMode): string {
        return mode !== ControlMode.IDLE
            ? this.translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.OVERWRITE")
            : this.translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_OVERWRITE");
    }
}

enum ControlMode {
    IDLE,
    NO_DISCHARGE,
    FORCE_CHARGE,
}

enum State {
    ON,
    OFF,
    NO_DISCHARGE,
    FORCE_CHARGE,
    DISCONNECTED,
    CONNECTED,
}
