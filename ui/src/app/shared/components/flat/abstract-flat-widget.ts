// @ts-strict-ignore
import { Directive, effect, EffectRef, inject, Inject, Injector, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";

import { ChannelAddress, CurrentData, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { Service } from "../../service/service";
import { UserService } from "../../service/USER.SERVICE";
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
     * True after THIS.EDGE, THIS.CONFIG and THIS.COMPONENT are set.
     */
    public isInitialized: boolean = false;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    public component: EDGE_CONFIG.COMPONENT = null;
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
            const isNewNavigation = THIS.USER_SERVICE.IS_NEW_NAVIGATION();
            THIS.NEW_NAVIGATION_URL_SEGMENT = isNewNavigation ? "/live" : "";
        });
    }

    public ngOnInit() {

        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.SERVICE.GET_CONFIG().then(config => {
                // store important variables publically
                THIS.EDGE = edge;
                THIS.CONFIG = config;
                THIS.COMPONENT = EDGE_CONFIG.COMPONENT.OF(CONFIG.COMPONENTS[THIS.COMPONENT_ID]);

                // announce initialized
                THIS.IS_INITIALIZED = true;
                THIS.AFTER_IS_INITIALIZED();
                // get the channel addresses that should be subscribed
                const channelAddresses: Set<ChannelAddress> = new Set(THIS.GET_CHANNEL_ADDRESSES());
                const channelIds = THIS.GET_CHANNEL_IDS();
                for (const channelId of channelIds) {
                    CHANNEL_ADDRESSES.ADD(new ChannelAddress(THIS.COMPONENT_ID, channelId));
                }
                THIS.DATA_SERVICE.GET_VALUES(ARRAY.FROM(channelAddresses), THIS.EDGE, THIS.COMPONENT_ID);
                THIS.SUBSCRIPTION.PUSH(effect(() => {
                    const value = THIS.DATA_SERVICE.CURRENT_VALUE();
                    THIS.ON_CURRENT_DATA(value);
                    THIS.AFTER_ON_CURRENT_DATA();
                }, { injector: THIS.INJECTOR }));

                THIS.FORM_GROUP = THIS.GET_FORM_GROUP();
            });
        });
    }

    public ngOnDestroy() {
        THIS.DATA_SERVICE.UNSUBSCRIBE_FROM_CHANNELS(THIS.GET_CHANNEL_ADDRESSES());
        THIS.STOP_ON_DESTROY.NEXT();
        THIS.STOP_ON_DESTROY.COMPLETE();
        THIS.SUBSCRIPTION.EVERY(el => EL.DESTROY());
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
                const val = THIS.DATA_SERVICE.CURRENT_VALUE();
                res(VAL.ALL_COMPONENTS[CHANNEL_ADDRESS.TO_STRING()]);
            }, { injector: THIS.INJECTOR });

            SUBSCRIPTION.DESTROY();
        });
    }

    /**
     * Gets the first valid --non null/undefined-- value for this channel and unsubscribes afterwards
     *
     * @param channelAddress the channel address
     * @returns a non null/undefined value
     */
    protected async subscribeAndGetFirstValidValueForChannel(channelAddress: ChannelAddress): Promise<any> {
        THIS.DATA_SERVICE.GET_VALUES([channelAddress], THIS.EDGE, THIS.COMPONENT_ID);
        return new Promise<any>((res) => {
            const subscription = effect(() => {
                const val = THIS.DATA_SERVICE.CURRENT_VALUE();
                res(VAL.ALL_COMPONENTS[CHANNEL_ADDRESS.TO_STRING()]);
            }, { injector: THIS.INJECTOR });
            SUBSCRIPTION.DESTROY();
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
