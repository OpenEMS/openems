import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";

import { PlatFormService } from "src/app/platform.service";
import { RouteService } from "src/app/shared/service/route/route.service";
import { Service } from "src/app/shared/shared";
import { CapacityExtensionDeterminationService } from "../../service/capacity-extension-determination.service";
import { CapacityExtensionStatusBannerComponent } from "./capacity-extension-status-banner";

describe("CapacityExtensionStatusBannerComponent", () => {
    let fixture: ComponentFixture<CapacityExtensionStatusBannerComponent>;
    let capacityExtensionDeterminationServiceSpy: jasmine.SpyObj<CapacityExtensionDeterminationService>;

    const translationKey = "EDGE.INDEX.RETROFITTING.EXPANSION_RUNNING";

    beforeEach(async () => {
        capacityExtensionDeterminationServiceSpy = jasmine.createSpyObj<CapacityExtensionDeterminationService>(
            "CapacityExtensionDeterminationService",
            ["anyCapacityExtensionRunning", "initializeCapacityExtensionTracking"],
        );

        capacityExtensionDeterminationServiceSpy.anyCapacityExtensionRunning.and.returnValue(false);

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

    it("delegates getter to CapacityExtensionDeterminationService", () => {
        capacityExtensionDeterminationServiceSpy.anyCapacityExtensionRunning.and.returnValue(true);

        fixture.detectChanges();

        expect(capacityExtensionDeterminationServiceSpy.anyCapacityExtensionRunning).toHaveBeenCalled();
    });

    it("renders info banner when capacity extension is running", () => {
        capacityExtensionDeterminationServiceSpy.anyCapacityExtensionRunning.and.returnValue(true);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;

        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_STATUS_BANNER")).not.toBeNull();
        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_INFO_TEXT")).not.toBeNull();
        expect(nativeElement.textContent).toContain(translationKey);
    });

    it("renders no banner when no capacity extension is running", () => {
        capacityExtensionDeterminationServiceSpy.anyCapacityExtensionRunning.and.returnValue(false);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;

        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_STATUS_BANNER")).toBeNull();
        expect(nativeElement.querySelector("#CAPACITY_EXTENSION_INFO_TEXT")).toBeNull();
        expect(nativeElement.textContent).not.toContain(translationKey);
    });
});
