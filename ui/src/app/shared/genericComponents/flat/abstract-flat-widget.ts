import { Directive, Inject, Input, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { v4 as uuidv4 } from 'uuid';

import { DataService } from "../shared/dataservice";
import { Converter } from "../shared/converter";
import { Websocket } from "../../service/websocket";
import { Service } from "../../service/service";

@Directive()
export abstract class AbstractFlatWidget implements OnInit, OnDestroy {

    public readonly Utils = Utils;
    public readonly Converter = Converter;

    @Input()
    protected componentId: string;

    /**
     * True after this.edge, this.config and this.component are set.
     */
    public isInitialized: boolean = false;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    public component: EdgeConfig.Component = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();

    private selector: string = uuidv4();

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) protected modalController: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        protected dataService: DataService,
    ) {
    }

    public ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                // store important variables publically
                this.edge = edge;
                this.config = config;
                this.component = config.components[this.componentId];

                // announce initialized
                this.isInitialized = true;

                this.afterIsInitialized();

                // get the channel addresses that should be subscribed
                let channelAddresses: Set<ChannelAddress> = new Set(this.getChannelAddresses());
                let channelIds = this.getChannelIds();
                for (let channelId of channelIds) {
                    channelAddresses.add(new ChannelAddress(this.componentId, channelId));
                }
                this.dataService.getValues(Array.from(channelAddresses), this.edge, this.componentId);
                this.dataService.currentValue.pipe(takeUntil(this.stopOnDestroy)).subscribe(value => {
                    this.onCurrentData(value);
                    this.afterOnCurrentData();
                });
            });
        });
    };

    public ngOnDestroy() {
        this.dataService.unsubscribeFromChannels(this.getChannelAddresses());

        // Unsubscribe from CurrentData subject
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    /**
     * Called on every new data.
     *
     * @param currentData new data for the subscribed Channel-Addresses
    */
    protected onCurrentData(currentData: CurrentData) { }

    /**
     * Gets the ChannelAddresses that should be subscribed.
    */
    protected getChannelAddresses(): ChannelAddress[] {
        return [];
    }

    /**
     * Gets the ChannelIds of the current Component that should be subscribed.
    */
    protected getChannelIds(): string[] {
        return [];
    }

    /**
     * Gets called after {@link isInitialized} is true
     */
    protected afterIsInitialized() { }

    /**
     * Gets called after {@link onCurrentData}, every time the currentValue changes
     */
    protected afterOnCurrentData() { };
}
