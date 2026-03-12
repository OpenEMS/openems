import { CommonModule } from "@angular/common";
import { Component, effect, inject, OnDestroy } from "@angular/core";
import { IonicModule, ModalController } from "@ionic/angular";
import { filter } from "rxjs";
import { v4 as uuidv4 } from "uuid";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { SumState } from "src/app/index/shared/sumState";
import { ChannelAddress, Edge, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";
import { DataService } from "../../shared/dataservice";
import { StatusSingleComponent } from "../single/status.component";


@Component({
    selector: SystemStatusComponent.SELECTOR,
    templateUrl: "./system-status.component.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
    ],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class SystemStatusComponent implements OnDestroy {
    private static readonly SELECTOR = "oe-system-status";
    private static readonly SUM_STATE_CHANNEL = new ChannelAddress("_sum", "State");

    public environment = environment;
    public edge: Edge | null = null;

    protected sumState: SumState | null = null;

    private subscribed = false;
    private liveDataService = inject(DataService);

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
        private websocket: Websocket,
    ) {
        effect(() => {
            const edge = this.service.currentEdge();
            if (edge == null || this.subscribed) {
                return;
            }

            this.subscribed = true;
            this.edge = edge;

            this.liveDataService.getValues([
                SystemStatusComponent.SUM_STATE_CHANNEL,
            ], edge, uuidv4());

            edge.currentData.pipe(
                filter(currentData => currentData !== null),
            ).subscribe((currentData) => {
                const channelValue: number = currentData.channel[SystemStatusComponent.SUM_STATE_CHANNEL.toString()];

                this.sumState = this.mapChannelValueToSumState(channelValue);
            });
        });
    }

    ngOnDestroy() {
        this.edge?.unsubscribeFromChannels(SystemStatusComponent.SELECTOR, this.websocket, [SystemStatusComponent.SUM_STATE_CHANNEL]);
    }

    async presentSingleStatusModal() {
        const modal = await this.modalCtrl.create({
            component: StatusSingleComponent,
        });
        return await modal.present();
    }

    private mapChannelValueToSumState(channelValue: number): SumState {
        switch (channelValue) {
            case 0: {
                return SumState.OK;
            }
            case 1: {
                return SumState.INFO;
            }
            case 2: {
                return SumState.WARNING;
            }
            case 3: {
                return SumState.FAULT;
            }
        }
        return SumState.UNDEFINED;
    }
}
