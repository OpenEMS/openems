import { ChangeDetectorRef, Directive, Inject, Input, OnDestroy } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { v4 as uuidv4 } from 'uuid';
import { Role } from "../../type/role";

@Directive()
export abstract class AbstractModalLine implements OnDestroy {

    /** FormGroup */
    @Input() formGroup: FormGroup;

    /** component */
    @Input() component: EdgeConfig.Component = null;

    /** FormGroup ControlName */
    @Input() controlName: string;

    /**
    * Use `converter` to convert/map a CurrentData value to another value, e.g. an Enum number to a text.
    * 
    * @param value the value from CurrentData
    * @returns converter function
    */
    @Input()
    converter = (value: any): string => { return value }

    /** Name for parameter, displayed on the left side*/
    @Input()
    name: string;

    /** value defines value of the parameter, displayed on the right */
    @Input() values: {
        value: string | number,
        /* If no role provided, default is guest */
        roleIsAtLeast?: Role,
        /* the converter to pass*/
        unit?: (value: any) => {}
    }[] = []

    /** Channel defines the channel, you need for this line */
    @Input()
    set channelAddress(channel: { address: string, unit?: (value: any) => {}, roleIsAtLeast?: Role }[]) {
        this.subscribe(channel);
    }

    /** Selector needed for Subscribe (Identifier) */
    private selector: string = uuidv4()

    /** 
     * displayValue is the displayed @Input value in html
     */
    public displayValue: string | string[] = null;

    /** Checks if any value of this line can be seen => hides line if false */
    protected isAllowed: boolean = true;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) protected modalCtrl: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        @Inject(FormBuilder) public formBuilder: FormBuilder,
        private ref: ChangeDetectorRef
    ) {
        ref.detach();
        setInterval(() => {
            this.ref.detectChanges(); // manually trigger change detection
        }, 0);
    }

    ngOnChanges() {
        this.convertvalues(this.values);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                // store important variables publically
                this.edge = edge;
                this.config = config;

                // get the channel addresses that should be subscribed
                let channelAddresses: ChannelAddress[] = this.getChannelAddresses();
                let channelIds = this.getChannelIds();
                for (let channelId of channelIds) {
                    channelAddresses.push(new ChannelAddress(this.component.id, channelId));
                }
                if (channelAddresses.length != 0) {
                    this.edge.subscribeChannels(this.websocket, this.selector, channelAddresses);
                }

                // call onCurrentData() with latest data
                edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                    let allComponents = {};
                    let thisComponent = {};
                    for (let channelAddress of channelAddresses) {
                        let ca = channelAddress.toString();
                        allComponents[ca] = currentData.channel[ca];
                        if (channelAddress.componentId === this.component.id) {
                            thisComponent[channelAddress.channelId] = currentData.channel[ca];
                        }
                    }
                    this.onCurrentData({ thisComponent: thisComponent, allComponents: allComponents });
                });
            })
        })
    }

    /** value defines value of the parameter, displayed on the right */
    protected setValue(value: any[]) {
        this.displayValue = [];
        for (let val of value) {
            this.displayValue.push(this.converter(val))
        }
    }

    /** Subscribe on HTML passed Channels */
    protected subscribe(channel: { address: string, unit?: (value: any) => {}, roleIsAtLeast?: Role }[]) {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;

            // Check if user is allowed to see these channel-values
            let permittedChannels: ChannelAddress[] = channel.filter(element => {
                if (this.edge.roleIsAtLeast(element.roleIsAtLeast ?? Role.GUEST)) {
                    return element.address
                }
            }).map(element => ChannelAddress.fromString(element.address));

            edge.subscribeChannels(this.websocket, this.selector, permittedChannels);

            // call onCurrentData() with latest data
            edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                let values: any[] = [];
                permittedChannels.forEach(element => {
                    if (currentData.channel[element.toString()] != null) {
                        let converter = channel.find(chan => chan.address == element.toString()).unit ?? function (value: any) { return value };
                        values.push(converter(currentData.channel[element.toString()]))
                    }
                });

                this.isAllowed = values.length !== 0;
                values.length > 0 && this.setValue(values);
            })
        });
    }
    /**
     * Converts a value based on its passed unit
     * 
     * @param data the data to be parsed
     */
    protected convertvalues(data: { value: string | number, roleIsAtLeast?: Role, unit?: (value: any) => {} }[]) {
        let values: any[] = [];
        data.forEach(element => {
            values.push(element.unit ? element.unit(element.value) : element.value)
        });
        this.setValue(values)
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
        return
    }
    /**
   * Gets the ChannelIds of the current Component that should be subscribed.
   */
    protected getChannelIds(): string[] {
        return [];
    }
}

