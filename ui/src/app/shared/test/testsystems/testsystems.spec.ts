import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { ModalComponent } from "src/app/edge/live/common/grid/modal/modal";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "../../type/role";
import { ems1, ems10004, ems12786, ems30012, ems30034, ems30048, ems4 } from "./edges.spec";
import { AbstractSystem } from "./types.spec";

describe('ExampleSystemsTest', () => {

  let translate: TranslateService;
  beforeEach((() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: Language.DEFAULT.key })
      ],
      providers: [TranslateService, { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] }]
    }).compileComponents();
    translate = TestBed.inject(TranslateService);
  }));

  let testSystems: AbstractSystem[] = [
    new ems1(), new ems4(), new ems10004(), new ems12786(), new ems30012(), new ems30034(), new ems30048()
  ];

  for (let key in testSystems) {
    let system = testSystems[key];
    for (let widget of system.widgets) {
      it(system.key + "-" + widget.modal.name, () => {
        for (let [key, modelToBeMatched] of widget.modal.fieldsWithRoles) {

          sessionStorage.setItem("first", JSON.stringify(
            ModalComponent.generateView(system.key, system.config, Role.getRole(Role[key]), translate)));
          sessionStorage.setItem("second", JSON.stringify(modelToBeMatched));
          expect(JSON.stringify(
            ModalComponent.generateView(system.key, system.config, Role.getRole(Role[key]), translate))).toBe(JSON.stringify(modelToBeMatched));
        }
      });
    }
  }
});