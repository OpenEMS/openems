import { Form, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { filter } from "rxjs/operators";

import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "../../shared";
import { SharedModule } from "../../shared.module";
import { Role } from "../../type/role";
import { ButtonLabel } from "../modal/modal-button/modal-button";
import { TextIndentation } from "../modal/modal-line/modal-line";
import { Converter } from "./converter";

export abstract class AbstractFormlyComponent {

  protected readonly translate: TranslateService;
  protected fields: FormlyFieldConfig[] = [];
  protected form: FormGroup = new FormGroup({});
  protected websocket: Websocket;
  protected service: Service;

  constructor() {
    this.service = SharedModule.injector.get<Service>(Service);
    const route = SharedModule.injector.get<ActivatedRoute>(ActivatedRoute);
    this.translate = SharedModule.injector.get<TranslateService>(TranslateService);
    this.websocket = SharedModule.injector.get<Websocket>(Websocket);

    this.service.setCurrentComponent('', route).then(edge => {
      edge.getConfig(this.service.websocket)
        .pipe(filter(config => !!config))
        .subscribe((config) => {
          var view = this.generateView(config, edge.role, this.translate);

          this.fields = [{
            type: "input",
            props: {
              attributes: {
                title: view.title,
              },
              required: true,
              options: [
                {
                  ...(view.formToBeBuildt && { formToBeBuildt: view.formToBeBuildt }),
                  formGroup: view.formGroup ?? new FormGroup({}),
                  ...(view.component && { component: view.component })
                },
                {
                  lines: view.lines,
                }]
            },
            wrappers: ['formly-field-modal']
          }];
        });
    });
  }

  /**
    * Generate the View.
    * 
    * @param config the Edge-Config
    * @param role  the Role of the User for this Edge
    * @param translate the Translate-Service
    */
  protected abstract generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView;

  protected static getFormGroup(): FormGroup {
    return new FormGroup({});
  }
}

export type OeFormlyView = {
  title: string,
  lines: OeFormlyField[],
  formGroup?: FormGroup,
  formToBeBuildt?: { controlName: string, channel: string }[],
  component?: EdgeConfig.Component
}

export type OeFormlyField =
  | OeFormlyField.InfoLine
  | OeFormlyField.Item
  | OeFormlyField.ChildrenLine
  | OeFormlyField.ChannelLine
  | OeFormlyField.HorizontalLine
  | OeFormlyField.ButtonsLine;



export namespace OeFormlyField {

  export type InfoLine = {
    type: 'info-line',
    name: string
  }

  export type Item = {
    type: 'item',
    channel: string,
    filter?: (value: number | null) => boolean,
    converter?: (value: number | null) => string
  }

  export type ChildrenLine = {
    type: 'children-line',
    name: /* actual name string */ string | /* name string derived from channel value */ { channel: ChannelAddress, converter: Converter },
    indentation?: TextIndentation,
    children: Item[],
  }

  export type ChannelLine = {
    type: 'channel-line',
    name: /* actual name string */ string | /* name string derived from channel value */ Converter,
    channel: string,
    filter?: (value: number | null) => boolean,
    converter?: (value: number | null) => string
    indentation?: TextIndentation,
  }

  export type HorizontalLine = {
    type: 'horizontal-line',
  }

  export type ButtonsLine = {
    type: 'buttons-line',
    /** The channel will be used as value for the buttons */
    channel: string,
    buttons: ButtonLabel[],
    controlName: string,
    formControlValues: (currentData: CurrentData) => Converter
  }
}