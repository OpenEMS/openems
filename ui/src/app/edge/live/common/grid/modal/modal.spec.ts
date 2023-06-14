import { registerLocaleData } from "@angular/common";
import localeDe from '@angular/common/locales/de';
import localeDeExtra from '@angular/common/locales/extra/de';
import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";
import { GridMode } from "src/app/shared/shared";
import { TestSystem } from "src/app/shared/test/testSystem.spec";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { Constants } from "./constants.spec";
import { ModalComponent } from "./modal";
describe('ExampleSystemsTest', () => {

  let translate: TranslateService;
  beforeEach((() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: Language.DEFAULT.key })
      ],
      providers: [TranslateService, { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] }]
    }).compileComponents();
    registerLocaleData(localeDe, 'de', localeDeExtra);
    translate = TestBed.inject(TranslateService);
  }));

  // let testSystems: AbstractSystem[] = [
  //   new TestSystem.Ems1([
  //     new GridWidget(new Modal("GridModal", new Map()
  //       // .set(Role.INSTALLER, Constants.EMS1_ADMIN_AND_INSTALLER)
  //       // .set(Role.OWNER, Constants.EMS1_OWNER_AND_GUEST)
  //       // .set(Role.GUEST, Constants.EMS1_OWNER_AND_GUEST)
  //     ))]),
  // new TestSystem.Ems4([
  //   new Grid(new Modal("GridModal", new Map()
  //     .set(Role.ADMIN, Constants.EMS4_ADMIN_AND_INSTALLER)
  //     .set(Role.INSTALLER, Constants.EMS4_ADMIN_AND_INSTALLER)
  //     .set(Role.OWNER, Constants.EMS4_OWNER_AND_GUEST)
  //     .set(Role.GUEST, Constants.EMS4_OWNER_AND_GUEST)
  //   ))]),
  // new TestSystem.Ems10004([
  //   new Grid(new Modal("GridModal", new Map()
  //     .set(Role.ADMIN, Constants.EMS10004_ADMIN_AND_INSTALLER)
  //     .set(Role.INSTALLER, Constants.EMS10004_ADMIN_AND_INSTALLER)
  //     .set(Role.OWNER, Constants.EMS10004_OWNER_AND_GUEST)
  //     .set(Role.GUEST, Constants.EMS10004_OWNER_AND_GUEST)
  //   ))]),
  // new TestSystem.Ems12786([
  //   new Grid(new Modal("GridModal", new Map()
  //     .set(Role.ADMIN, Constants.EMS12786_ADMIN_AND_INSTALLER)
  //     .set(Role.INSTALLER, Constants.EMS12786_ADMIN_AND_INSTALLER)
  //     .set(Role.OWNER, Constants.EMS12786_OWNER_AND_GUEST)
  //     .set(Role.GUEST, Constants.EMS12786_OWNER_AND_GUEST)
  //   ))]),
  // new TestSystem.Ems30012([
  //   new Grid(new Modal("GridModal", new Map()
  //     .set(Role.ADMIN, Constants.EMS30012_ADMIN_AND_INSTALLER)
  //     .set(Role.INSTALLER, Constants.EMS30012_ADMIN_AND_INSTALLER)
  //     .set(Role.OWNER, Constants.EMS30012_OWNER_AND_GUEST)
  //     .set(Role.GUEST, Constants.EMS30012_OWNER_AND_GUEST)
  //   ))]),
  // new TestSystem.Ems30034([
  //   new Grid(new Modal("GridModal", new Map()
  //     .set(Role.ADMIN, Constants.EMS30034_ADMIN_AND_INSTALLER)
  //     .set(Role.INSTALLER, Constants.EMS30034_ADMIN_AND_INSTALLER)
  //     .set(Role.OWNER, Constants.EMS30034_OWNER_AND_GUEST)
  //     .set(Role.GUEST, Constants.EMS30034_OWNER_AND_GUEST)
  //   ))]),
  // new TestSystem.Ems30048([
  //   new Grid(new Modal("GridModal", new Map()
  //     .set(Role.ADMIN, Constants.EMS30048_ADMIN_AND_INSTALLER)
  //     .set(Role.INSTALLER, Constants.EMS30048_ADMIN_AND_INSTALLER)
  //     .set(Role.OWNER, Constants.EMS30048_OWNER_AND_GUEST)
  //     .set(Role.GUEST, Constants.EMS30048_OWNER_AND_GUEST)
  // ))])
  // ];

  it('ModalComponent.generateView()', () => {
    var config = new TestSystem.Ems1().config;
    // expect(Constants.EMS1_ADMIN_AND_INSTALLER).toEqual(ModalComponent.generateView(ems.config, Role.ADMIN, translate));

    var result = OeFormlyViewTester.apply(ModalComponent.generateView(config, Role.ADMIN, translate), {
      "_sum/GridMode": GridMode.ON_GRID,
      "meter0/ActivePower": -1000,
      "meter0/VoltageL1": 230000,
      "meter0/CurrentL1": 2170,
      "meter0/ActivePowerL1": -500,
      "meter0/ActivePowerL2": 1500
    });

    console.log(JSON.stringify(result, null, 2));

    expect(result).toEqual(Constants.EMS1_ADMIN_AND_INSTALLER);

  });

  // for (let key in testSystems) {
  //   let system = testSystems[key];
  //   for (let widget of system.widgets) {
  //     it(system.key + "-" + widget.modal.name, () => {
  //       for (let [key, modelToBeMatched] of widget.modal.views) {
  //         let view = ModalComponent.generateView(system.config, Role.getRole(Role[key]), translate);
  //         console.log("#1");
  //         console.log(view);
  //         console.log("#2");
  //         console.log(modelToBeMatched);

  //         expect(view).toBe(modelToBeMatched);
  //         // expect(JSON.stringify(view)).toBe(JSON.stringify(modelToBeMatched));
  //       }
  //     });
  //   }
  // }
});
