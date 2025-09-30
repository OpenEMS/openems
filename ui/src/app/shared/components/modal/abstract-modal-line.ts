// @ts-strict-ignore
import { ChangeDetectorRef, Directive, Inject, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from "uuid";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils, Websocket } from "src/app/shared/shared";

import { Role } from "../../type/role";
import { Converter } from "../shared/converter";
import { Filter } from "../shared/filter";

@Directive()
export abstract class AbstractModalLine implements OnInit, OnDestroy, OnChanges {

    /** FormGroup */
    @Input({ required: true }) public formGroup!: FormGroup;

    /** component */
    @Input() public component: EDGE_CONFIG.COMPONENT = null;

    /** FormGroup ControlName */
    @Input({ required: true }) public controlName!: string;

    /**
    * Use `converter` to convert/map a CurrentData value to another value, E.G. an Enum number to a text.
    *
    * @param value the current data value
    * @returns converter function
    */
    @Input() public converter: Converter = Converter.TO_STRING;

    /**
    * Use `filter` to remove a line depending on a value.
    *
    * @param value the current data value
    * @returns converter function
    */
    @Input() public filter: Filter = Filter.NO_FILTER;
    @Input({ required: true }) public value!: number | string;
    @Input() public roleIsAtLeast?: Role = ROLE.GUEST;

    /**
     * displayValue is the displayed @Input value in html
    */
    public displayValue: string | null = null;
    public displayName: string | null = null;
    public edge: Edge | null = null;
    public config: EdgeConfig | null = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();

    /** Checks if any value of this line can be seen => hides line if false
     *
     * @deprecated can be remove in future when live-view is refactored with formlyfield
    */
    protected isAllowedToBeSeen: boolean = true;
    protected show: boolean = true;
    protected readonly Role = Role;
    protected readonly Utils = Utils;
    protected readonly Converter = Converter;

    private _name: string | ((value: any) => string);
    /** Selector needed for Subscribe (Identifier) */
    private selector: string = uuidv4();

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) protected modalCtrl: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        @Inject(FormBuilder) public formBuilder: FormBuilder,
        private ref: ChangeDetectorRef,
    ) {
        REF.DETACH();
        setInterval(() => {
            THIS.REF.DETECT_CHANGES(); // manually trigger change detection
        }, 0);
    }

    /** Name for parameter, displayed on the left side*/
    @Input() set name(value: string | { channel: ChannelAddress, converter: (value: any) => string }) {
        if (typeof value === "object") {
            THIS.SUBSCRIBE(VALUE.CHANNEL);
            this._name = VALUE.CONVERTER;
        } else {
            this._name = value;
        }
    }

    /** Channel defines the channel, you need for this line */
    @Input()
    set channelAddress(channelAddress: string) {
        if (channelAddress) {
            THIS.SUBSCRIBE(CHANNEL_ADDRESS.FROM_STRING(channelAddress));
        }
    }

    ngOnChanges() {
        THIS.SET_VALUE(THIS.VALUE);
    }

    ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.SERVICE.GET_CONFIG().then(config => {
                // store important variables publically
                THIS.EDGE = edge;
                THIS.CONFIG = config;

                // get the channel addresses that should be subscribed
                const channelAddresses: ChannelAddress[] = [...THIS.GET_CHANNEL_ADDRESSES()];

                if (typeof THIS.NAME == "object") {
                    CHANNEL_ADDRESSES.PUSH(THIS.NAME.CHANNEL);
                }

                const channelIds = THIS.GET_CHANNEL_IDS();
                for (const channelId of channelIds) {
                    CHANNEL_ADDRESSES.PUSH(new ChannelAddress(THIS.COMPONENT.ID, channelId));
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
            });
        });
    }

    public ngOnDestroy() {
        // Unsubscribe from OpenEMS
        if (THIS.EDGE != null) {
            THIS.EDGE.UNSUBSCRIBE_CHANNELS(THIS.WEBSOCKET, THIS.SELECTOR);
        }

        // Unsubscribe from CurrentData subject
        THIS.STOP_ON_DESTROY.NEXT();
        THIS.STOP_ON_DESTROY.COMPLETE();
    }

    /** value defines value of the parameter, displayed on the right */
    protected setValue(value: number | string | null) {

        /** Prevent undefined values */
        value = value != null ? value : null;

        if (THIS.FILTER) {
            THIS.SHOW = THIS.FILTER(value);
        }

        if (typeof this._name == "function") {
            THIS.DISPLAY_NAME = this._name(value);

        } else {
            THIS.DISPLAY_NAME = this._name;
            if (THIS.CONVERTER) {
                THIS.DISPLAY_VALUE = THIS.CONVERTER(value);
            }
        }
    }

    /** Subscribe on HTML passed Channels */
    protected subscribe(channelAddress: ChannelAddress) {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;

            // Check if user is allowed to see these channel-values
            if (THIS.EDGE.ROLE_IS_AT_LEAST(THIS.ROLE_IS_AT_LEAST)) {
                THIS.IS_ALLOWED_TO_BE_SEEN = true;
                EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, THIS.SELECTOR, [channelAddress]);

                // call onCurrentData() with latest data
                EDGE.CURRENT_DATA.PIPE(takeUntil(THIS.STOP_ON_DESTROY)).subscribe(currentData => {
                    if (CURRENT_DATA.CHANNEL[CHANNEL_ADDRESS.TO_STRING()] != null) {
                        THIS.SET_VALUE(CURRENT_DATA.CHANNEL[CHANNEL_ADDRESS.TO_STRING()]);
                    }
                });
            } else {
                THIS.IS_ALLOWED_TO_BE_SEEN = false;
            }
        });

    }

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

    protected getFormGroup(): FormGroup {
        return;
    }
    /**
   * Gets the ChannelIds of the current Component that should be subscribed.
   */
    protected getChannelIds(): string[] {
        return [];
    }
}
