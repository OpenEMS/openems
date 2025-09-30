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
      THIS.SUBSCRIBE(VALUE.CHANNEL);
      this._name = VALUE.CONVERTER;
    } else {
      this._name = value;
    }
  }

  /** Channel defines the channel, you need for this line */
  @Input()
  set channelAddress(channelAddress: string) {
    this._channelAddress = CHANNEL_ADDRESS.FROM_STRING(channelAddress);
    THIS.SUBSCRIBE(CHANNEL_ADDRESS.FROM_STRING(channelAddress));
  }

  /**
   * Use `converter` to convert/map a CurrentData value to another value, E.G. an Enum number to a text.
  *
  * @param value the value from CurrentData
  * @returns converter function
  */
  @Input() public converter = (value: any): string => { return value; };

  public ngOnChanges() {
    THIS.SET_VALUE(THIS.VALUE);
  }

  public ngOnDestroy() {

    // Unsubscribe from CurrentData subject
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
    THIS.SUBSCRIPTION?.destroy();
  }

  protected setValue(value: any) {
    if (typeof this._name == "function") {
      THIS.DISPLAY_NAME = this._name(value);

    } else {
      THIS.DISPLAY_NAME = this._name;
    }
    THIS.DISPLAY_VALUE = THIS.CONVERTER(value);

    if (THIS.FILTER) {
      THIS.SHOW = THIS.FILTER(value);
    }
  }

  protected subscribe(channelAddress: ChannelAddress) {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;

      THIS.DATA_SERVICE.GET_VALUES([channelAddress], THIS.EDGE);

      THIS.SUBSCRIPTION = effect(() => {
        const val = THIS.DATA_SERVICE.CURRENT_VALUE();
        THIS.SET_VALUE(VAL.ALL_COMPONENTS[CHANNEL_ADDRESS.TO_STRING()]);
      }, { injector: THIS.INJECTOR });
    });
  }
}
