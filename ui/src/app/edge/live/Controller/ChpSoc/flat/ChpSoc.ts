import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { ComponentsModule } from "src/app/shared/components/components.module";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { Icon } from "src/app/shared/type/widget";

import { ChannelAddress, CurrentData } from "../../../../../shared/shared";
import { Controller_ChpSocModalComponent } from "../modal/modal.component";

@Component({
    selector: "Controller_ChpSocComponent",
    templateUrl: "./ChpSoc.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, TranslateModule, ComponentsModule],
})
export class ControllerChpFlatComponent extends AbstractFlatWidget {
    public static readonly PROPERTY_MODE: string = "_PropertyMode";
    public inputChannel: ChannelAddress | null = null;
    public outputChannel: ChannelAddress | null = null;
    public propertyModeChannel: ChannelAddress | null = null;
    public highThresholdValue: number | null = null;
    public lowThresholdValue: number | null = null;
    public state: string | null = null;
    public mode: string | null = null;
    public modeChannelValue: string | null = null;
    public inputChannelValue: number | null = null;
    public icon: Icon = {
        name: "",
        size: "large",
        color: "primary",
    };

    protected modalComponent: Modal | null = null;

    protected get thresholdDelta() {
        if (this.highThresholdValue == null || this.lowThresholdValue == null) {
            return 0;
        }
        const delta = this.highThresholdValue - this.lowThresholdValue;
        return Math.max(0, delta);
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_ChpSocModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                outputChannel: this.outputChannel,
                inputChannel: this.inputChannel,
            },
        });
        return await modal.present();
    }

    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }

    protected getModalComponent(): Modal {
        return {
            component: Controller_ChpSocModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                outputChannel: this.outputChannel,
                inputChannel: this.inputChannel,
            },
        };
    }

    protected override getChannelAddresses() {
        if (this.component == null) {
            return [];
        }

        this.outputChannel = ChannelAddress.fromString(this.component.properties["outputChannelAddress"]);
        this.inputChannel = ChannelAddress.fromString(this.component.properties["inputChannelAddress"]);
        this.propertyModeChannel = new ChannelAddress(this.component.id, ControllerChpFlatComponent.PROPERTY_MODE);
        return [
            this.outputChannel,
            this.inputChannel,
            this.propertyModeChannel,
            new ChannelAddress(this.component.id, "_PropertyHighThreshold"),
            new ChannelAddress(this.component.id, "_PropertyLowThreshold"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (
            this.component == null ||
            this.outputChannel == null ||
            this.inputChannel == null ||
            this.propertyModeChannel == null
        ) {
            return;
        }
        // Mode
        this.modeChannelValue = currentData.allComponents[this.propertyModeChannel.toString()];
        switch (this.modeChannelValue) {
            case "ON":
                this.mode = this.translate.instant("GENERAL.ON");
                break;
            case "OFF":
                this.mode = this.translate.instant("GENERAL.OFF");
                break;
            case "AUTOMATIC":
                this.mode = this.translate.instant("GENERAL.AUTOMATIC");
        }

        const isActive = currentData.allComponents[this.outputChannel.toString()] === 1;

        this.state = this.translate.instant(isActive ? "GENERAL.ACTIVE" : "GENERAL.INACTIVE");

        this.icon.name = isActive ? "" : "help-outline";

        this.inputChannelValue = currentData.allComponents[this.inputChannel.toString()];
        this.highThresholdValue = currentData.allComponents[this.component.id + "/_PropertyHighThreshold"];
        this.lowThresholdValue = currentData.allComponents[this.component.id + "/_PropertyLowThreshold"];
    }
}
