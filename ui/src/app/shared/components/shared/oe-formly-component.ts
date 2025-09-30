import { Directive, effect, EffectRef, inject, Injector, OnDestroy } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { filter, take, takeUntil } from "rxjs/operators";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "../../shared";
import { SharedModule } from "../../SHARED.MODULE";
import { Role } from "../../type/role";
import { AssertionUtils } from "../../utils/assertions/ASSERTIONS.UTILS";
import { FormUtils } from "../../utils/form/FORM.UTILS";
import { ButtonLabel } from "../modal/modal-button/modal-button";
import { ModalLineComponent, TextIndentation } from "../modal/modal-line/modal-line";
import { Converter } from "./converter";
import { DataService } from "./dataservice";

@Directive()
export abstract class AbstractFormlyComponent implements OnDestroy {

  protected readonly translate: TranslateService;
  protected SKIP_COUNT: number = 2;
  protected dataService: DataService;
  protected fields: FormlyFieldConfig[] = [];
  protected form: FormGroup = new FormGroup({});
  protected formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-modal";

  protected stopOnDestroy: Subject<void> = new Subject<void>();

  /** Skips next two currentData events */
  protected skipCurrentData: boolean = false;
  private injector: Injector = inject(Injector);
  private subscription: EffectRef | null = null;

  constructor() {
    const service = SHARED_MODULE.INJECTOR.GET<Service>(Service);
    THIS.TRANSLATE = SHARED_MODULE.INJECTOR.GET<TranslateService>(TranslateService);
    THIS.DATA_SERVICE = inject(DataService);
    const websocket = inject(Websocket);

    SERVICE.GET_CURRENT_EDGE().then(async edge => {

      // Subscribe on channels only once
      EDGE.GET_CONFIG(SERVICE.WEBSOCKET)
        .pipe(filter(config => !!config), take(1))
        .subscribe(() => THIS.SUBSCRIBE_CHANNELS(service));

      EDGE.GET_CONFIG(SERVICE.WEBSOCKET)
        .pipe(filter(config => !!config), takeUntil(THIS.STOP_ON_DESTROY))
        .subscribe((config) => {
          const view = THIS.GENERATE_VIEW(config, EDGE.ROLE, THIS.TRANSLATE);
          THIS.FORM = THIS.GET_FORM_GROUP();

          THIS.FIELDS = [{
            type: "input",
            props: {
              attributes: {
                title: VIEW.TITLE,
              },
              required: true,
              options: [{ lines: VIEW.LINES, component: VIEW.COMPONENT }],
              onSubmit: (fg: FormGroup) => {
                THIS.APPLY_CHANGES(fg, service, websocket, VIEW.COMPONENT ?? null, VIEW.EDGE ?? null);
              },
            },
            className: "ion-full-height",
            wrappers: [THIS.FORMLY_WRAPPER],
            form: THIS.FORM,
          }];
        });
    });
  }

  public async ngOnDestroy() {
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
    THIS.DATA_SERVICE?.unsubscribeFromChannels(await THIS.GET_CHANNEL_ADDRESSES());
  }

  /**
   * Subscribes on passed channels
   *
   * @param service the service
   * @returns {Promise<void>} A Promise that resolves without a value.
   */
  public async subscribeChannels(service: Service): Promise<void> {
    const channelAddresses = await THIS.GET_CHANNEL_ADDRESSES();
    const edge = await SERVICE.GET_CURRENT_EDGE();
    ASSERTION_UTILS.ASSERT_IS_DEFINED(edge);

    THIS.DATA_SERVICE.GET_VALUES(channelAddresses, edge);
    THIS.FETCH_CURRENT_DATA(service);
  }

  /**
   * Fetches currentdata
   *
   * @note skips 2 currentData events, because changes are not instantly applied
   * after a {@link UpdateComponentConfigRequest} that the new value is returned with the notification event: currentData
   *
   * @workaround still needed, due to no event returned after component update
   */
  protected async fetchCurrentData(service: Service) {
    let skipCount = 0;
    THIS.SUBSCRIPTION = effect(() => {
      const val = THIS.DATA_SERVICE.CURRENT_VALUE();
      if (THIS.SKIP_CURRENT_DATA && skipCount < this.SKIP_COUNT) {
        skipCount++;
        return;
      }

      THIS.SKIP_CURRENT_DATA = false; // Reset after skipping 2 values
      SERVICE.STOP_SPINNER("formly-field-modal");
      skipCount = 0;
      THIS.ON_CURRENT_DATA(val);

    }, { injector: THIS.INJECTOR });
  }


  /**
   * Called on every new data - executed on every currentData notification.
   *
   * @param currentData new data for the subscribed Channel-Addresses
   */
  protected onCurrentData(currentData: CurrentData) { }

  /**
   * Gets the ChannelAddresses that should be subscribed.
   *
   * @returns the channel addresses to subscribe
   */
  protected async getChannelAddresses(): Promise<ChannelAddress[]> { return []; }

  /**
   * Applys the formGroup changes
   *
   * @note calls an {@link UpdateComponentConfigRequest} with the current componentId and the changed controls,
   * form control names resemble edge config properties, so they need to match
   *
   *
   * @param fg the formGroup
   * @param service the service
   * @param websocket the websocket
   * @param component the current component
   * @param edge the edge
   */
  protected applyChanges(fg: FormGroup<any>, service: Service, websocket: Websocket, component: EDGE_CONFIG.COMPONENT | null, edge: Edge | null) {
    ASSERTION_UTILS.ASSERT_IS_DEFINED(component);
    ASSERTION_UTILS.ASSERT_IS_DEFINED(edge);


    const updateComponentArray: { name: string, value: any }[] = [];
    SERVICE.START_SPINNER("formly-field-modal");
    for (const key in FG.CONTROLS) {
      const control = FG.CONTROLS[key];
      FG.CONTROLS[key];

      // Check if formControl-value didn't change
      if (CONTROL.PRISTINE) {
        continue;
      }

      UPDATE_COMPONENT_ARRAY.PUSH({
        name: key,
        value: FG.VALUE[key],
      });
    }

    if (!edge || !component) {
      throw new Error("Either edge or component not provided");
    }

    EDGE.UPDATE_COMPONENT_CONFIG(websocket, COMPONENT.ID, updateComponentArray)
      .then(() => {
        SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
      }).catch(reason => {
        SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
      }).finally(() => {
        THIS.SKIP_CURRENT_DATA = true;
        FG.MARK_AS_PRISTINE();
        SERVICE.STOP_SPINNER("formly-field-modal");
      });
  }

  /**
   * Collects the formGroup
   *
   * @note Every formControl resembles the corresponding edgeconfig property, so naming is important
   *
   * @tipp initialize {@link FormControl} with null, hides component dependent on this Formcontrol, till a non null/undefined is set
   **/
  protected getFormGroup() {
    return new FormGroup({});
  }

  /**
   * Sets the formControls value to a given channel value
   *
   * @param fg the formGroup
   * @param formControlName the control name to change
   * @param currentData the current data
   * @param channel the channel to use
   * @returns the new formGroup
   */
  protected setFormControlSafely<T>(fg: FormGroup, formControlName: string, currentData: CurrentData, channel: ChannelAddress | null) {
    if (THIS.SKIP_CURRENT_DATA || FG.DIRTY || FG.TOUCHED || !channel || CURRENT_DATA.ALL_COMPONENTS[CHANNEL.TO_STRING()] == null) {
      return;
    }

    const prevFormControlValue: T | null = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY(fg, formControlName);
    const currFormControlValue: T | null = CURRENT_DATA.ALL_COMPONENTS[CHANNEL.TO_STRING()];

    if (currFormControlValue != null && (prevFormControlValue !== currFormControlValue)) {
      FG.CONTROLS[formControlName].setValue(currFormControlValue);
      FG.CONTROLS[formControlName].markAsTouched();
    }
  }

  /**
    * Generate the View.
    *
    * @param config the Edge-Config
    * @param role  the Role of the User for this Edge
    * @param translate the Translate-Service
    */
  protected abstract generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView;
}

export type OeFormlyView = {
  title: string,
  lines: OeFormlyField[],
  component?: EDGE_CONFIG.COMPONENT,
  edge?: Edge,
};

export type OeFormlyField =
  | OE_FORMLY_FIELD.INFO_LINE
  | OE_FORMLY_FIELD.ITEM
  | OE_FORMLY_FIELD.CHILDREN_LINE
  | OE_FORMLY_FIELD.CHANNEL_LINE
  | OE_FORMLY_FIELD.HORIZONTAL_LINE
  | OE_FORMLY_FIELD.VALUE_FROM_CHANNELS_LINE
  | OE_FORMLY_FIELD.VALUE_FROM_FORM_CONTROL_LINE
  | OE_FORMLY_FIELD.BUTTONS_FROM_FORM_CONTROL_LINE
  | OE_FORMLY_FIELD.RANGE_BUTTON_FROM_FORM_CONTROL_LINE;

export namespace OeFormlyField {

  export type InfoLine = {
    type: "info-line",
    name: string
  };

  export type Item = {
    type: "item",
    channel: string,
    filter?: (value: number | null) => boolean,
    converter?: (value: number | null) => string
  };

  export type ChildrenLine = {
    type: "children-line",
    name: /* actual name string */ string | /* name string derived from channel value */ { channel: ChannelAddress, converter: Converter },
    indentation?: TextIndentation,
    children: Item[],
  };

  export type ChannelLine = {
    type: "channel-line",
    name: /* actual name string */ string | /* name string derived from channel value */ Converter,
    channel: string,
    filter?: (value: number | null) => boolean,
    converter?: (value: number | null) => string
    indentation?: TextIndentation,
  };

  export type ValueFromChannelsLine = {
    type: "value-from-channels-line",
    name: string,
    value: (data: CurrentData) => string,
    channelsToSubscribe: ChannelAddress[],
    indentation?: TextIndentation,
    filter?: (value: number[] | null) => boolean,
  };

  export type ButtonsFromFormControlLine = {
    type: "buttons-from-form-control-line",
    name: string,
    controlName: string,
    buttons: ButtonLabel[];
  };

  export type RangeButtonFromFormControlLine = {
    type: "range-button-from-form-control-line",
    controlName: string,
    properties: Partial<Extract<ModalLineComponent["control"], { type: "RANGE" }>["properties"]>,
    // channel: string,
  };
  export type ValueFromFormControlLine = {
    type: "value-from-form-control-line",
    controlName: string,
    name: string,
    converter: Converter,
  };

  export type HorizontalLine = {
    type: "horizontal-line",
  };
}
