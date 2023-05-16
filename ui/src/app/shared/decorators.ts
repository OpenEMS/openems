import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { filter } from "rxjs/operators";
import { Service } from "./shared";
import { SharedModule } from "./shared.module";

/**Custom class decorators*/

/**
 * Angular class decorator properties are not directly accessible in HTML templates.
 * Decorators are applied to classes and are primarily used to modify or enhance the behavior of the class itself.
 * 
 * This is the reason, why you cant use properties, defined in the decorator, in html.
 * 
 * This behaviour could change with future @angular releases. 
 * 
 * Future-Work: In a Future Version of angular it may be possible to extend the @Component decorator
 * */
export function CustomViewComponent<T extends { new(...args: any[]) }>(baseClass: T) {

  return class ViewComponent extends baseClass {

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