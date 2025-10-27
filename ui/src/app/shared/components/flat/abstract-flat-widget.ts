// @ts-strict-ignore
import { Directive, effect, EffectRef, inject, Inject, Injector, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";

import { ChannelAddress, CurrentData, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { Service } from "../../service/service";
import { UserService } from "../../service/user.service";
import { Websocket } from "../../service/websocket";
import { Converter } from "../shared/converter";
import { DataService } from "../shared/dataservice";

@Directive()
export abstract class AbstractFlatWidget implements OnInit, OnDestroy {

    @Input()
    protected componentId: string;

    public readonly Utils = Utils;
    public readonly Converter = Converter;

    /**
     * True after this.edge, this.config and this.component are set.
     */
    public isInitialized: boolean = false;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    public component: EdgeConfig.Component = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();
    public formGroup: FormGroup | null = null;

    /** @deprecated used for new navigation migration purposes */
    public isNewNavigation = false;
    /** @deprecated */
    public newNavigationUrlSegment: string;

    private injector = inject(Injector);
    private subscription: EffectRef[] = [];


    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) protected modalController: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        protected dataService: DataService,
        protected formBuilder: FormBuilder,
        protected router: Router,
        protected userService: UserService,
    ) {

        effect(() => {
            const isNewNavigation = this.userService.isNewNavigation();
            this.newNavigationUrlSegment = isNewNavigation ? "/live" : "";
        });
    }

    public ngOnInit() {

        this.service.getCurrentEdge().then(edge => {
            this.service.getConfig().then(config => {
                // store important variables publically
                this.edge = edge;
                this.config = config;
                this.component = EdgeConfig.Component.of(config.components[this.componentId]);

                // announce initialized
                this.isInitialized = true;
                this.afterIsInitialized();
                // get the channel addresses that should be subscribed
                const channelAddresses: Set<ChannelAddress> = new Set(this.getChannelAddresses());
                const channelIds = this.getChannelIds();
                for (const channelId of channelIds) {
                    channelAddresses.add(new ChannelAddress(this.componentId, channelId));
                }
                this.dataService.getValues(Array.from(channelAddresses), this.edge, this.componentId);
                this.subscription.push(effect(() => {
                    const value = this.dataService.currentValue();
                    this.onCurrentData(value);
                    this.afterOnCurrentData();
                }, { injector: this.injector }));

                this.formGroup = this.getFormGroup();
            });
        });
    }

    public ngOnDestroy() {
        this.dataService.unsubscribeFromChannels(this.getChannelAddresses());
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
        this.subscription.every(el => el.destroy());
    }

    /**
     * Gets the first valid --non null/undefined-- value for this channel
     *
     * @param channelAddress the channel address
     * @returns a non null/undefined value
     */
    protected async getFirstValidValueForChannel<T = any>(channelAddress: ChannelAddress): Promise<T> {
        return new Promise<any>((res) => {
            const subscription = effect(() => {
                const val = this.dataService.currentValue();
                res(val.allComponents[channelAddress.toString()]);
            }, { injector: this.injector });

            subscription.destroy();
        });
    }

    /**
     * Gets the first valid --non null/undefined-- value for this channel and unsubscribes afterwards
     *
     * @param channelAddress the channel address
     * @returns a non null/undefined value
     */
    protected async subscribeAndGetFirstValidValueForChannel(channelAddress: ChannelAddress): Promise<any> {
        this.dataService.getValues([channelAddress], this.edge, this.componentId);
        return new Promise<any>((res) => {
            const subscription = effect(() => {
                const val = this.dataService.currentValue();
                res(val.allComponents[channelAddress.toString()]);
            }, { injector: this.injector });
            subscription.destroy();
        });
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
    protected afterOnCurrentData() { }

    /** Gets the FormGroup of the current Component */
    protected getFormGroup(): FormGroup | null {
        return null;
    }
}
