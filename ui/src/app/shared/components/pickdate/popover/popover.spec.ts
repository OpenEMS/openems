import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { AngularDelegate, IonicModule, PopoverController } from "@ionic/angular";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/ROUTE.SERVICE";
import { Service } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/TRANSLATE.EXTENSION";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { PickdateModule } from "../PICKDATE.MODULE";
import { PickDatePopoverComponent } from "./POPOVER.COMPONENT";

describe("PickdatePopover", () => {

    let fixture: ComponentFixture<PickDatePopoverComponent>;
    let component: PickDatePopoverComponent;

    beforeEach(async () => {
        await TEST_BED.CONFIGURE_TESTING_MODULE({
            declarations: [PickDatePopoverComponent],
            imports: [
                TRANSLATE_MODULE.FOR_ROOT({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: LANGUAGE.DEFAULT.KEY, useDefaultLang: false }),
                IonicModule,
                PickdateModule,
            ],
            providers: [
                TranslateService,
                { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
                { provide: LOCALE_ID, useValue: LANGUAGE.DEFAULT.KEY },
                Service,
                PopoverController,
                AngularDelegate,
                RouteService,
            ],
        }).compileComponents().then(() => {
            fixture = TEST_BED.CREATE_COMPONENT(PickDatePopoverComponent);
            component = FIXTURE.COMPONENT_INSTANCE;
            FIXTURE.DETECT_CHANGES();
        });
    });

    it("is AngularMyDatePickerModule calendar opening on \"other period\" button", () => {
        const { debugElement } = fixture;
        const popoverBtn = DEBUG_ELEMENT.QUERY(BY.CSS("[testId=\"popover-button\"]"));
        POPOVER_BTN.TRIGGER_EVENT_HANDLER("click", null);
        FIXTURE.DETECT_CHANGES();

        expect(component).toBeDefined();
        expect((debugElement?.nativeNode?.children as HTMLCollection)?.item(2)?.localName).toEqual("lib-angular-mydatepicker-calendar");
    });
});
