import { FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { filter } from "rxjs/operators";
import { v4 as uuidv4 } from 'uuid';
import { EdgeConfig, Service } from "../../shared";
import { SharedModule } from "../../shared.module";
import { Role } from "../../type/role";
import { TextIndentation } from "../modal/modal-line/modal-line";

export abstract class AbstractFormlyComponent {

  protected readonly translate: TranslateService;
  protected fields: FormlyFieldConfig[] = [];
  protected form: FormGroup = new FormGroup({});

  constructor() {
    const service = SharedModule.injector.get<Service>(Service);
    const route = SharedModule.injector.get<ActivatedRoute>(ActivatedRoute);
    this.translate = SharedModule.injector.get<TranslateService>(TranslateService);

    service.setCurrentComponent('', route).then(edge => {
      edge.getConfig(service.websocket)
        .pipe(filter(config => !!config))
        .subscribe((config) => {
          var view = this.generateView(edge.id, config, edge.role);

          this.fields = [{
            key: uuidv4(),
            type: "input",

            templateOptions: {
              attributes: {
                title: view.title
              },
              required: true,
              options: [{ lines: view.lines }]
            },
            wrappers: ['formly-field-modal']
          }];
        });
    });
  }

  /**
   * Generate the View.
   * 
   * @param edgeId the Edge-ID
   * @param config the Edge-Config
   * @param role  the Role of the User for this Edge
   * @param translate the Translate-Service
   */
  protected abstract generateView(edgeId: string, config: EdgeConfig, role: Role): { title: string, lines: OeFormlyField[] };
}

export type OeFormlyField =
  | OeFormlyFieldLine
  | OeFieldLineInfo
  | OeFieldLineItem
  | OeFormlyFieldHorizontalLine

type OeFieldLineInfo = {
  type: 'line-info',
  name: string
}

type OeFieldLineItem = {
  type: 'line-item',
  channel: string,
  converter?: Function,
  filter?: Function,
}

type OeFormlyFieldLine = {
  type: 'line',
  name: string | Function,
  filter?: Function,
  channel?: string,
  converter?: Function,
  indentation?: TextIndentation,
  children?: OeFormlyField[]
}

type OeFormlyFieldHorizontalLine = {
  type: 'line-horizontal',
}