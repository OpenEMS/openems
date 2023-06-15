import { registerLocaleData } from "@angular/common";
import localeDe from '@angular/common/locales/de';
import localeDeExtra from '@angular/common/locales/extra/de';
import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";
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

  it('ModalComponent.generateView() GridModal', () => {

    // Admin and Installer -> singleMeter
    expect(
      OeFormlyViewTester.apply(ModalComponent
        .generateView(new TestSystem.Ems1().config, Role.ADMIN, translate),
        Constants.EMS1_ADMIN_AND_INSTALLER_SINGLE_METER.context)
    ).toEqual(Constants.EMS1_ADMIN_AND_INSTALLER_SINGLE_METER.view);

    // Admin and Installer -> two meters
    expect(
      OeFormlyViewTester.apply(ModalComponent
        .generateView(new TestSystem.Ems30093().config, Role.ADMIN, translate),
        Constants.EMS30093_ADMIN_AND_INSTALLER_TWO_METERS.context)
    ).toEqual(Constants.EMS30093_ADMIN_AND_INSTALLER_TWO_METERS.view);

    // // Owner and Guest -> single meter
    expect(
      OeFormlyViewTester.apply(ModalComponent
        .generateView(new TestSystem.Ems1().config, Role.OWNER, translate),
        Constants.EMS1_OWNER_AND_GUEST_SINGLE_METER.context)
    ).toEqual(Constants.EMS1_OWNER_AND_GUEST_SINGLE_METER.view);

    // // Owner and Guest -> two meters
    expect(
      OeFormlyViewTester.apply(ModalComponent
        .generateView(new TestSystem.Ems30093().config, Role.OWNER, translate),
        Constants.EMS30093_OWNER_AND_GUEST_TWO_METERS.context)
    ).toEqual(Constants.EMS30093_OWNER_AND_GUEST_TWO_METERS.view);

    // // Offgrid
    expect(
      OeFormlyViewTester.apply(ModalComponent
        .generateView(new TestSystem.Ems1().config, Role.ADMIN, translate),
        Constants.EMS1_OFF_GRID.context)
    ).toEqual(Constants.EMS1_OFF_GRID.view);
  });
});
