import { ChangeDetectionStrategy, Component } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { ComponentsModule } from "src/app/shared/components/components.module";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ControllerIoHeatpumpModalComponent } from "../modal/modal";
import { SharedControllerIoHeatpump } from "../shared/shared";

@Component({
    selector: "oe-controller-io-heatpump",
    templateUrl: "./flat.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [IonicModule, TranslateModule, ComponentsModule],
})
export class ControllerIoHeatpumpComponent extends AbstractFlatWidget {
    private static readonly PROPERTY_MODE = "_PropertyMode";
    private static readonly STATE_DISCONNECTED = 3;

    public override component: EdgeConfig.Component | null = null;
    public isConnectionSuccessful: boolean = false;
    public mode: string | null = null;
    public statusValue: string | null = null;
    protected modalComponent: Modal | null = null;
    protected activePower: number | null = null;
    protected consumptionMeter: EdgeConfig.Component | null = null;
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

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, "Status"),
            new ChannelAddress(this.component.id, "State"),
            new ChannelAddress(this.component.id, ControllerIoHeatpumpComponent.PROPERTY_MODE),
        ];

        AssertionUtils.assertIsDefined(this.config);
        this.consumptionMeter = SharedControllerIoHeatpump.getConsumptionMeter(this.config, this.component);

        if (this.consumptionMeter) {
            channelAddresses.push(new ChannelAddress(this.consumptionMeter.id, "ActivePower"));
        }

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        AssertionUtils.assertIsDefined(this.config);
        AssertionUtils.assertIsDefined(this.component);
        this.isConnectionSuccessful =
            currentData.allComponents[this.componentId + "/State"] !== ControllerIoHeatpumpComponent.STATE_DISCONNECTED;

        // Status
        this.statusValue = Converter.HEAT_PUMP_STATES(this.translate)(
            currentData.allComponents[this.componentId + "/Status"],
        );

        // Mode
        this.mode = Converter.CONTROLLER_PROPERTY_MODES(this.translate)(
            currentData.allComponents[this.componentId + "/" + ControllerIoHeatpumpComponent.PROPERTY_MODE],
        );

        if (this.consumptionMeter) {
            this.activePower = currentData.allComponents[this.consumptionMeter.id + "/ActivePower"];
        }
    }
}
