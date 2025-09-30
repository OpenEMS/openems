// @ts-strict-ignore
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared/shared";
import { DelayedSellToGridModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
    selector: DELAYED_SELL_TO_GRID_COMPONENT.SELECTOR,
    templateUrl: "./DELAYEDSELLTOGRID.COMPONENT.HTML",
    standalone: false,
})
export class DelayedSellToGridComponent implements OnInit, OnDestroy {

    private static readonly SELECTOR = "delayedselltogrid";

    @Input({ required: true }) public componentId!: string;

    public edge: Edge | null = null;

    public component: EDGE_CONFIG.COMPONENT | null = null;

    constructor(
        private route: ActivatedRoute,
        private websocket: Websocket,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;
            THIS.SERVICE.GET_CONFIG().then(config => {
                THIS.COMPONENT = CONFIG.GET_COMPONENT(THIS.COMPONENT_ID);
            });
        });
    }

    ngOnDestroy() {
        THIS.EDGE.UNSUBSCRIBE_CHANNELS(THIS.WEBSOCKET, DELAYED_SELL_TO_GRID_COMPONENT.SELECTOR);
    }

    async presentModal() {
        const modal = await THIS.MODAL_CTRL.CREATE({
            component: DelayedSellToGridModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
                edge: THIS.EDGE,
            },
        });
        return await MODAL.PRESENT();
    }
}
