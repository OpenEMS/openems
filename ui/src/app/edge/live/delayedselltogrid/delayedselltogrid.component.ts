// @ts-strict-ignore
import { Component, Input, OnDestroy, OnInit, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared/shared";
import { DelayedSellToGridModalComponent } from "./modal/modal.component";

@Component({
    selector: DelayedSellToGridComponent.SELECTOR,
    templateUrl: "./delayedselltogrid.component.html",
    standalone: false,
})
export class DelayedSellToGridComponent implements OnInit, OnDestroy {
    private route = inject(ActivatedRoute);
    private websocket = inject(Websocket);
    protected translate = inject(TranslateService);
    modalCtrl = inject(ModalController);
    service = inject(Service);


    private static readonly SELECTOR = "delayedselltogrid";

    @Input({ required: true }) public componentId!: string;

    public edge: Edge | null = null;

    public component: EdgeConfig.Component | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }

    ngOnDestroy() {
        this.edge.unsubscribeChannels(this.websocket, DelayedSellToGridComponent.SELECTOR);
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: DelayedSellToGridModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            },
        });
        return await modal.present();
    }
}
