import { FormGroup } from "@angular/forms";
import { Service } from "./shared";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { filter } from "rxjs/operators";
import { SharedModule } from "./shared.module";

/**Custom class decorators*/
export function CustomViewComponent<T extends { new(...args: any[]) }>(baseClass: T) {

  return class ViewComponent extends baseClass {

    protected fields: FormlyFieldConfig[] = [];
    protected form: FormGroup = new FormGroup({});

    constructor(...args: any[]) {
      super();

      const service = SharedModule.injector.get<Service>(Service);
      const route = SharedModule.injector.get<ActivatedRoute>(ActivatedRoute);
      const translate = SharedModule.injector.get<TranslateService>(TranslateService);

      service.setCurrentComponent('', route).then(edge => {
        edge.getConfig(service.websocket)
          .pipe(filter(config => !!config))
          .subscribe((config) => {
            this.fields = baseClass['generateView'](this.edge.id, config, this.edge.role, translate);
          })
      })
    }
  }
}