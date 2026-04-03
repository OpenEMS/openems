import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { ComponentsModule } from "src/app/shared/components/components.module";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { ControllerIoHeatpumpModalComponent } from "../modal/modal";

@Component({
    selector: "oe-controller-io-heatpump",
    templateUrl: "./flat.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
        ComponentsModule,
    ],
})
export class ControllerIoHeatpumpComponent extends AbstractFlatWidget {

    private static PROPERTY_MODE = "_PropertyMode";
    private static STATE_DISCONNECTED = 3;

    public override component: EdgeConfig.Component | null = null;
    public isConnectionSuccessful: boolean = false;
    public mode: string | null = null;
    public statusValue: string | null = null;
    protected modalComponent: Modal | null = null;
    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }
    protected getModalComponent(): Modal {
        return {
            component: ControllerIoHeatpumpModalComponent,
            componentProps: {
                edge: this.edge,
                component: this.component,
            },
        };
    }

    protected override getChannelAddresses() {
        if (this.component == null) {
            return [];
        }
        return [
            new ChannelAddress(this.component.id, "Status"),
            new ChannelAddress(this.component.id, "State"),
            new ChannelAddress(this.component.id, ControllerIoHeatpumpComponent.PROPERTY_MODE),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        this.isConnectionSuccessful = currentData.allComponents[this.componentId + "/State"] !== ControllerIoHeatpumpComponent.STATE_DISCONNECTED;

        // Status
        this.statusValue = Converter.HEAT_PUMP_STATES(this.translate)(currentData.allComponents[this.componentId + "/Status"]);

        // Mode
        this.mode = Converter.CONTROLLER_PROPERTY_MODES(this.translate)(currentData.allComponents[this.componentId + "/" + ControllerIoHeatpumpComponent.PROPERTY_MODE]);
    }
}
