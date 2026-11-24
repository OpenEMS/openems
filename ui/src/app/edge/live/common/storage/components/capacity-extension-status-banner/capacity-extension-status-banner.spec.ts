import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";

import { PlatFormService } from "src/app/platform.service";
import { RouteService } from "src/app/shared/service/route/route.service";
import { Service } from "src/app/shared/shared";
import { CapacityExtensionDeterminationService } from "../../service/capacity-extension-determination.service";
import { CapacityExtensionStatusBannerComponent } from "./capacity-extension-status-banner";

describe("CapacityExtensionStatusBannerComponent", () => {
    let fixture: ComponentFixture<CapacityExtensionStatusBannerComponent>;

    const isRunning = signal(false);
    const isExpansionAvailable = signal(false);

    const translationKeyRunning = "EDGE.INDEX.RETROFITTING.EXPANSION_RUNNING";
    const translationKeyExpansionAvailable = "EDGE.INDEX.RETROFITTING.EXPANSION_AVAILABLE";

    let capacityExtensionDeterminationServiceSpy: jasmine.SpyObj<CapacityExtensionDeterminationService>;

    beforeEach(async () => {
        capacityExtensionDeterminationServiceSpy = jasmine.createSpyObj(
            "CapacityExtensionDeterminationService",
            ["initializeCapacityExtensionTracking"],
            {
                anyCapacityExtensionRunning: isRunning,
                anyExpansionAvailable: isExpansionAvailable,
            },
        );

        await TestBed.configureTestingModule({
            imports: [CapacityExtensionStatusBannerComponent, TranslateModule.forRoot()],
            providers: [
                PlatFormService,
                RouteService,
                Service,
                {
                    provide: CapacityExtensionDeterminationService,
                    useValue: capacityExtensionDeterminationServiceSpy,
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(CapacityExtensionStatusBannerComponent);
    });

    it("renders the running banner when capacity extension is running", () => {
        isRunning.set(true);
        isExpansionAvailable.set(false);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;

        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_STATUS_BANNER")).not.toBeNull();
        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_INFO_TEXT")).not.toBeNull();
        expect(nativeElement.textContent).toContain(translationKeyRunning);
        expect(nativeElement.textContent).not.toContain(translationKeyExpansionAvailable);
    });

    it("renders the expansion-available message when running and state machine is at target SoC", () => {
        isRunning.set(true);
        isExpansionAvailable.set(true);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;

        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_STATUS_BANNER")).not.toBeNull();
        expect(nativeElement.textContent).toContain(translationKeyExpansionAvailable);
        expect(nativeElement.textContent).not.toContain(translationKeyRunning);
    });

    it("renders no banner when capacity extension is not running", () => {
        isRunning.set(false);
        isExpansionAvailable.set(false);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;

        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_STATUS_BANNER")).toBeNull();
        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_INFO_TEXT")).toBeNull();
    });
});
