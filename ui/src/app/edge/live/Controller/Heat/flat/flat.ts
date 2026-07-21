import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { ControllerHeatModalComponent } from "../modal/modal";
import { HeatConverter } from "../new-navigation/converter";
import { CONVERT_CHANNEL_MODE_TO_LABEL, HeatStatus } from "../shared/shared";

@Component({
    selector: "oe-controller-heat",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerHeatComponent extends AbstractFlatWidget {
    protected displayStatus: HeatStatus | null = null;
    protected modalComponent: Modal | null = null;
    protected readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
    protected readonly CONVERT_POWER_2_HEAT_STATE = HeatConverter.CONVERT_POWER_2_HEAT_STATE(this.translate);
    protected readonly CONVERT_CHANNEL_MODE_TO_LABEL = (value: number | null): string =>
        CONVERT_CHANNEL_MODE_TO_LABEL(this.translate)(value);

    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }

    protected getModalComponent(): Modal {
        return {
            component: ControllerHeatModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            },
        };
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        if (this == null) {
            return [];
        }

        if (this.component == null) {
            return [];
        }

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, "Status"),
            new ChannelAddress(this.component.id, "ControlNotAllowed"),
            new ChannelAddress(this.component.id, "ActivePower"),
            new ChannelAddress(this.component.id, "Temperature"),
            ...(this.component.factoryId === "Heat.Askoma" ? [new ChannelAddress(this.component.id, "_PropertyMode")] : []),
        ];

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this.component != null) {
            const backendStatus = currentData.allComponents[this.component.id + "/Status"] ?? HeatStatus.ERROR;
            this.displayStatus = this.resolveDisplayStatus(backendStatus, currentData);
        }
    }

    private resolveDisplayStatus(backendStatus: HeatStatus, currentData: CurrentData): HeatStatus {
        switch (backendStatus) {
            case HeatStatus.STANDBY:
            case HeatStatus.EXCESS:
            case HeatStatus.CONTROL_NOT_ALLOWED:
                return HeatStatus.EXCESS;
            case HeatStatus.TEMPERATURE_REACHED:
                return HeatStatus.TEMPERATURE_REACHED;
            case HeatStatus.NO_CONTROL_SIGNAL:
                if (currentData.allComponents[this.component?.id + "/" + "ActivePower"] > 0) {
                    return HeatStatus.EXCESS;
                }
                return HeatStatus.NO_CONTROL_SIGNAL;
            case HeatStatus.ERROR:
            default:
                return HeatStatus.NO_CONTROL_SIGNAL;
        }
    }
}
