import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { AngularDelegate, IonicModule, PopoverController } from "@ionic/angular";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { Service } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { PickdateModule } from "../pickdate.module";
import { PickDatePopoverComponent } from "./popover.component";

describe("PickdatePopover", () => {

    let fixture: ComponentFixture<PickDatePopoverComponent>;
    let component: PickDatePopoverComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [PickDatePopoverComponent],
            imports: [
                TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: Language.DEFAULT.key, useDefaultLang: false }),
                IonicModule,
                PickdateModule,
            ],
            providers: [
                TranslateService,
                { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
                { provide: LOCALE_ID, useValue: Language.DEFAULT.key },
                Service,
                PopoverController,
                AngularDelegate,
            ],
        }).compileComponents().then(() => {
            fixture = TestBed.createComponent(PickDatePopoverComponent);
            component = fixture.componentInstance;
            fixture.detectChanges();
        });
    });

    it("is AngularMyDatePickerModule calendar opening on \"other period\" button", () => {
        const { debugElement } = fixture;
        const popoverBtn = debugElement.query(By.css("[testId=\"popover-button\"]"));
        popoverBtn.triggerEventHandler("click", null);
        fixture.detectChanges();
        expect(component).toBeDefined();
        expect((debugElement?.nativeNode?.children as HTMLCollection)?.item(2)?.localName).toEqual("lib-angular-mydatepicker-calendar");
    });
});
