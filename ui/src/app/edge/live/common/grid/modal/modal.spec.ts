import { registerLocaleData } from "@angular/common";
import localeDe from '@angular/common/locales/de';
import localeDeExtra from '@angular/common/locales/extra/de';
import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";
import { TestSystem } from "src/app/shared/test/testSystem.spec";
import { AbstractSystem } from "src/app/shared/test/types.spec";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { Constants } from "./constants.spec";
import { ModalComponent } from "./modal";

export const EXPECT_VIEW = (system: AbstractSystem, role: Role, viewContext: OeFormlyViewTester.ViewContext, translate: TranslateService): void => {
  expect(
    OeFormlyViewTester.apply(ModalComponent
      .generateView(system.config, role, translate),
      viewContext.context)
  ).toEqual(viewContext.view);
};

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

    // Empty EMS
    EXPECT_VIEW(new TestSystem.EmptyEms(), Role.ADMIN, Constants.EMPTY_EMS, translate);

    // Admin and Installer -> singleMeter
    EXPECT_VIEW(new TestSystem.Ems1(), Role.ADMIN, Constants.EMS1_ADMIN_AND_INSTALLER_SINGLE_METER, translate);

    // Admin and Installer -> two meters
    EXPECT_VIEW(new TestSystem.Ems30093(), Role.ADMIN, Constants.EMS30093_ADMIN_AND_INSTALLER_TWO_METERS, translate);

    // Owner and Guest -> single meter
    EXPECT_VIEW(new TestSystem.Ems1(), Role.OWNER, Constants.EMS1_OWNER_AND_GUEST_SINGLE_METER, translate);

    // Owner and Guest -> two meters
    EXPECT_VIEW(new TestSystem.Ems30093(), Role.OWNER, Constants.EMS30093_OWNER_AND_GUEST_TWO_METERS, translate);

    // Offgrid
    EXPECT_VIEW(new TestSystem.Ems1(), Role.ADMIN, Constants.EMS1_OFF_GRID, translate);
  });
});
