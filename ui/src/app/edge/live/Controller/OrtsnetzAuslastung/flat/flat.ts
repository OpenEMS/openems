import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { Icon } from "src/app/shared/type/widget";
import { ModalComponent } from "../modal/modal";
import { SharedControllerOrtsnetzAuslastung as Shared } from "../shared/shared";

@Component({
    selector: "Controller_OrtsnetzAuslastung",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    protected readonly CONVERT_GRID_STATUS = Shared.CONVERT_GRID_STATUS(this.translate);
    protected readonly CONVERT_STORAGE_RECOMMENDATION = Shared.CONVERT_STORAGE_RECOMMENDATION(this.translate);

    protected modalComponent: Modal | null = null;
    protected icon: Icon = { name: "pulse-outline", color: "normal", size: "large" };
    protected gridStatus: number | null = null;
    protected storageRecommendation: number | null = null;
    protected hasProblem: boolean = false;

    protected override afterIsInitialized(): void {
        this.modalComponent = {
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        };
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(this.componentId, "GridStatus"),
            new ChannelAddress(this.componentId, "StorageRecommendation"),
            ...Shared.PROBLEM_CHANNELS.map((problem) => new ChannelAddress(this.componentId, problem.channelId)),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        const data = currentData.allComponents;
        this.gridStatus = data[this.componentId + "/GridStatus"];
        this.storageRecommendation = data[this.componentId + "/StorageRecommendation"];
        this.hasProblem = Shared.getProblemKeys(this.componentId, data).length > 0;

        const color = Shared.getStatusColor(this.gridStatus);
        if (this.icon.color !== color) {
            this.icon = { ...this.icon, color: color };
        }
    }
}
