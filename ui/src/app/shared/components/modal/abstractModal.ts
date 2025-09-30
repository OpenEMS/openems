// @ts-strict-ignore
import { ChangeDetectorRef, Directive, Inject, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject, Subscription } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from "uuid";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils, Websocket } from "src/app/shared/shared";

import { Role } from "../../type/role";
import { Converter } from "../shared/converter";
import { TextIndentation } from "./modal-line/modal-line";

@Directive()
export abstract class AbstractModal implements OnInit, OnDestroy {

    @Input() public component: EDGE_CONFIG.COMPONENT | null = null;

    /** Enum for User Role */
    public readonly Role = Role;

    /** Enum for Indentation */
    public readonly TextIndentation = TextIndentation;

    public readonly Utils = Utils;
    public readonly Converter = Converter;

    public isInitialized: boolean = false;
    public edge: Edge | null = null;
    public config: EdgeConfig = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();
    public formGroup: FormGroup | null = null;

    /** Should be used to unsubscribe from all subscribed observables at once */
    protected subscription: Subscription = new Subscription();

    private selector: string = uuidv4();

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) public modalController: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        @Inject(FormBuilder) public formBuilder: FormBuilder,
        public ref: ChangeDetectorRef,
    ) {
        REF.DETACH();
        setInterval(() => {
            THIS.REF.DETECT_CHANGES(); // manually trigger change detection
        }, 0);
    }

    public ngOnDestroy() {
        THIS.EDGE.UNSUBSCRIBE_FROM_CHANNELS(THIS.WEBSOCKET, THIS.GET_CHANNEL_ADDRESSES());
        THIS.SUBSCRIPTION.UNSUBSCRIBE();

        // Unsubscribe from CurrentData subject
        THIS.STOP_ON_DESTROY.NEXT();
        THIS.STOP_ON_DESTROY.COMPLETE();
    }

    public ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.SERVICE.GET_CONFIG().then(async config => {

                // store important variables publically
                THIS.EDGE = edge;
                THIS.CONFIG = config;

                await THIS.UPDATE_COMPONENT(config);

                // If component is passed
                let channelAddresses: ChannelAddress[] = [];

                // get the channel addresses that should be subscribed
                channelAddresses = THIS.GET_CHANNEL_ADDRESSES();
                if (THIS.COMPONENT != null) {
                    THIS.COMPONENT = EDGE_CONFIG.COMPONENT.OF(CONFIG.COMPONENTS[THIS.COMPONENT.ID]);

                    const channelIds = THIS.GET_CHANNEL_IDS();
                    for (const channelId of channelIds) {
                        CHANNEL_ADDRESSES.PUSH(new ChannelAddress(THIS.COMPONENT.ID, channelId));
                    }
                }
                if (CHANNEL_ADDRESSES.LENGTH != 0) {
                    THIS.EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, THIS.SELECTOR, channelAddresses);
                }

                // call onCurrentData() with latest data
                EDGE.CURRENT_DATA.PIPE(takeUntil(THIS.STOP_ON_DESTROY)).subscribe(currentData => {
                    const allComponents = {};
                    for (const channelAddress of channelAddresses) {
                        const ca = CHANNEL_ADDRESS.TO_STRING();
                        allComponents[ca] = CURRENT_DATA.CHANNEL[ca];
                    }
                    THIS.ON_CURRENT_DATA({ allComponents: allComponents });
                });
                THIS.FORM_GROUP = THIS.GET_FORM_GROUP();

                // announce initialized
                THIS.IS_INITIALIZED = true;

                THIS.ON_IS_INITIALIZED();
            });
        });
    }

    protected updateComponent(config: EdgeConfig) {
        return;
    }

    protected onIsInitialized() { }

    /**
     * Called on every new data.
     *
     * @param currentData new data for the subscribed Channel-Addresses
     */
    protected onCurrentData(currentData: CurrentData) {
    }

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

    /** Gets the FormGroup of the current Component */
    protected getFormGroup(): FormGroup | null {
        return null;
    }
}
