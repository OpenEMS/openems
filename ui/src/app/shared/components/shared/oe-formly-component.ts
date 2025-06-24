import { Directive, inject, OnDestroy } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { filter, take, takeUntil } from "rxjs/operators";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "../../shared";
import { SharedModule } from "../../shared.module";
import { Role } from "../../type/role";
import { AssertionUtils } from "../../utils/assertions/assertions.utils";
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

  constructor() {
    const service = SharedModule.injector.get<Service>(Service);
    this.translate = SharedModule.injector.get<TranslateService>(TranslateService);
    this.dataService = inject(DataService);
    const websocket = inject(Websocket);

    service.getCurrentEdge().then(async edge => {

      // Subscribe on channels only once
      edge.getConfig(service.websocket)
        .pipe(filter(config => !!config), take(1))
        .subscribe(() => this.subscribeChannels(service));

      edge.getConfig(service.websocket)
        .pipe(filter(config => !!config), takeUntil(this.stopOnDestroy))
        .subscribe((config) => {
          const view = this.generateView(config, edge.role, this.translate);
          this.form = this.getFormGroup();
          this.fields = [{
            type: "input",
            props: {
              attributes: {
                title: view.title,
              },
              required: true,
              options: [{ lines: view.lines, component: view.component }],
              onSubmit: (fg: FormGroup) => {
                this.applyChanges(fg, service, websocket, view.component ?? null, view.edge ?? null);
              },

            },
            className: "ion-full-height",
            wrappers: [this.formlyWrapper],
            form: this.form,
          }];
        });
    });
  }

  public async ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
    this.dataService?.unsubscribeFromChannels(await this.getChannelAddresses());
  }

  /**
   * Subscribes on passed channels
   *
   * @param service the service
   * @returns {Promise<void>} A Promise that resolves without a value.
   */
  public async subscribeChannels(service: Service): Promise<void> {
    const channelAddresses = await this.getChannelAddresses();
    const edge = await service.getCurrentEdge();
    AssertionUtils.assertIsDefined(edge);

    this.dataService.getValues(channelAddresses, edge);
    this.fetchCurrentData(service);
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
    this.dataService.currentValue.pipe(takeUntil(this.stopOnDestroy), filter(() => {
      if (this.skipCurrentData && skipCount < this.SKIP_COUNT) {
        skipCount++;
        return false;
      }

      this.skipCurrentData = false; // Reset after skipping 2 values
      service.stopSpinner("formly-field-modal");
      skipCount = 0;
      return true;
    })).subscribe((currentData) => {
      this.onCurrentData(currentData);
    });
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
  protected applyChanges(fg: FormGroup<any>, service: Service, websocket: Websocket, component: EdgeConfig.Component | null, edge: Edge | null) {

    AssertionUtils.assertIsDefined(component);
    AssertionUtils.assertIsDefined(edge);

    const updateComponentArray: { name: string, value: any }[] = [];
    service.startSpinner("formly-field-modal");
    for (const key in fg.controls) {
      const control = fg.controls[key];
      fg.controls[key];

      // Check if formControl-value didn't change
      if (control.pristine) {
        continue;
      }

      updateComponentArray.push({
        name: key,
        value: fg.value[key],
      });
    }

    if (!edge || !component) {
      throw new Error("Either edge or component not provided");
    }

    edge.updateComponentConfig(websocket, component.id, updateComponentArray)
      .then(() => {
        service.toast(this.translate.instant("General.changeAccepted"), "success");
      }).catch(reason => {
        service.toast(this.translate.instant("General.changeFailed") + "\n" + reason.error.message, "danger");
      }).finally(() => {
        this.skipCurrentData = true;
        fg.markAsPristine();
        service.stopSpinner("formly-field-modal");
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
  component?: EdgeConfig.Component,
  edge?: Edge,
};

export type OeFormlyField =
  | OeFormlyField.InfoLine
  | OeFormlyField.Item
  | OeFormlyField.ChildrenLine
  | OeFormlyField.ChannelLine
  | OeFormlyField.HorizontalLine
  | OeFormlyField.ValueFromChannelsLine
  | OeFormlyField.ValueFromFormControlLine
  | OeFormlyField.ButtonsFromFormControlLine
  | OeFormlyField.RangeButtonFromFormControlLine;

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
