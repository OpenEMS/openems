// @ts-strict-ignore
import { Directive, effect, EffectRef, inject, Inject, Injector, Input, OnChanges, OnDestroy } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { Subject } from "rxjs";
import { v4 as uuidv4 } from "uuid";
import { ChannelAddress, Edge, Service, Websocket } from "src/app/shared/shared";

import { DataService } from "../shared/dataservice";
import { Filter } from "../shared/filter";

@Directive()
export abstract class AbstractFlatWidgetLine implements OnChanges, OnDestroy {

    /** value defines value of the parameter, displayed on the right */
    @Input()
    public value: any;

    /**
   * Use `filter` to remove a line depending on a value.
  *
     * @param value the current data value
     * @returns converter function
     */
    @Input() public filter: Filter = Filter.NO_FILTER;

    /**
   * displayValue is the displayed @Input value in html
  */
    public displayValue: string | null = null;

    protected displayName: string = null;
    protected show: boolean = true;

    private _name: string | ((value: any) => string);
    private _channelAddress: ChannelAddress | null = null;

    /**
   * selector used for subscribe
  */
    private selector: string = uuidv4();
    private stopOnDestroy: Subject<void> = new Subject<void>();
    private edge: Edge | null = null;
    private subscription: EffectRef;
    private injector = inject(Injector);

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) protected modalCtrl: ModalController,
        @Inject(DataService) private dataService: DataService,
    ) { }

    @Input() set name(value: string | { channel: ChannelAddress, converter: (value: any) => string }) {
        if (typeof value === "object") {
            this.subscribe(value.channel);
            this._name = value.converter;
        } else {
            this._name = value;
        }
    }

    /** Channel defines the channel, you need for this line */
    @Input()
    set channelAddress(channelAddress: string) {
        this._channelAddress = ChannelAddress.fromString(channelAddress);
        this.subscribe(ChannelAddress.fromString(channelAddress));
    }

    /**
   * Use `converter` to convert/map a CurrentData value to another value, e.g. an Enum number to a text.
  *
  * @param value the value from CurrentData
  * @returns converter function
  */
    @Input() public converter = (value: any): string => { return value; };

    public ngOnChanges() {
        this.setValue(this.value);
    }

    public ngOnDestroy() {

        // Unsubscribe from CurrentData subject
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
        this.subscription?.destroy();
    }

    protected setValue(value: any) {
        if (typeof this._name == "function") {
            this.displayName = this._name(value);

        } else {
            this.displayName = this._name;
        }
        this.displayValue = this.converter(value);

        if (this.filter) {
            this.show = this.filter(value);
        }
    }

    protected subscribe(channelAddress: ChannelAddress) {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;

            this.dataService.getValues([channelAddress], this.edge);

            this.subscription = effect(() => {
                const val = this.dataService.currentValue();
                this.setValue(val.allComponents[channelAddress.toString()]);
            }, { injector: this.injector });
        });
    }
}
