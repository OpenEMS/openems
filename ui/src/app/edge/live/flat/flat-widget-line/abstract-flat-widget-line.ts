import { Directive, Inject, Input, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { UUID } from "angular2-uuid";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ChannelAddress, Edge, Service, Websocket } from "src/app/shared/shared";

@Directive()
export abstract class AbstractFlatWidgetLine implements OnDestroy {

    /**
     * True after this.edge, this.config and this.component are set.
     */
    public isInitialized: boolean = false;

    @Input()
    protected converter = (value: any): string => { return value }
    /**
     * selector used for subscribe
     */
    private selector: string = UUID.UUID().toString();
    /** 
     * displayValue is the displayed @Input value in html
     */
    public displayValue: string;
    private stopOnDestroy: Subject<void> = new Subject<void>();
    private edge: Edge = null;


    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) protected modalCtrl: ModalController
    ) {
    }
    protected setValue(value: any) {
        this.displayValue = this.converter(value);

        // announce initialized
        this.isInitialized = true;
    }
    protected subscribe(channelAddress?: ChannelAddress) {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;

            edge.subscribeChannels(this.websocket, this.selector, [channelAddress]);

            // call onCurrentData() with latest data
            edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                this.setValue(currentData.channel[channelAddress.toString()]);
            });
        });
    }


    public ngOnDestroy() {
        // Unsubscribe from OpenEMS
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, this.selector);
        }

        // Unsubscribe from CurrentData subject
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }
}