import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { SohDeterminationService } from "../../service/soh-determination.service";

import { SohStatusBannerComponent } from "./soh-status-banner";

describe("SohStatusBannerComponent", () => {
    let fixture: ComponentFixture<SohStatusBannerComponent>;
    let component: SohStatusBannerComponent;
    let sohDeterminationServiceSpy: jasmine.SpyObj<SohDeterminationService>;

    beforeEach(async () => {
        sohDeterminationServiceSpy = jasmine.createSpyObj<SohDeterminationService>("SohDeterminationService", [
            "anySohCycleRunningWithoutError",
            "anySohCycleRunningWithError",
        ]);

        await TestBed.configureTestingModule({
            imports: [
                SohStatusBannerComponent,
                TranslateModule.forRoot(),
            ],
            providers: [
                { provide: SohDeterminationService, useValue: sohDeterminationServiceSpy },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(SohStatusBannerComponent);
        component = fixture.componentInstance;
    });

    it("delegates both getters to SohDeterminationService", () => {
        sohDeterminationServiceSpy.anySohCycleRunningWithoutError.and.returnValue(true);
        sohDeterminationServiceSpy.anySohCycleRunningWithError.and.returnValue(false);

        expect(component.anySohCycleRunningWithoutError).toBeTrue();
        expect(sohDeterminationServiceSpy.anySohCycleRunningWithoutError).toHaveBeenCalledTimes(1);

        expect(component.anySohCycleRunningWithError).toBeFalse();
        expect(sohDeterminationServiceSpy.anySohCycleRunningWithError).toHaveBeenCalledTimes(1);
    });

    it("renders info banner when cycle runs without error", () => {
        sohDeterminationServiceSpy.anySohCycleRunningWithoutError.and.returnValue(true);
        sohDeterminationServiceSpy.anySohCycleRunningWithError.and.returnValue(false);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;
        expect(nativeElement.querySelector("#SOH_INFO_TEXT")).not.toBeNull();
        expect(nativeElement.textContent).toContain("STORAGE.ADMIN_MODAL.SOH.CAPACITY_MEASURING");
        expect(nativeElement.textContent).not.toContain("STORAGE.ADMIN_MODAL.SOH.ERROR");
    });

    it("renders error banner when cycle runs with error", () => {
        sohDeterminationServiceSpy.anySohCycleRunningWithoutError.and.returnValue(false);
        sohDeterminationServiceSpy.anySohCycleRunningWithError.and.returnValue(true);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;
        expect(nativeElement.querySelector("#SOH_INFO_TEXT")).toBeNull();
        expect(nativeElement.querySelector("#SOH_ERROR_TEXT")).not.toBeNull();
        expect(nativeElement.textContent).toContain("STORAGE.ADMIN_MODAL.SOH.ERROR");
        expect(nativeElement.textContent).not.toContain("STORAGE.ADMIN_MODAL.SOH.CAPACITY_MEASURING");
    });

    it("renders no banner when no cycle is running", () => {
        sohDeterminationServiceSpy.anySohCycleRunningWithoutError.and.returnValue(false);
        sohDeterminationServiceSpy.anySohCycleRunningWithError.and.returnValue(false);

        fixture.detectChanges();

        const nativeElement = fixture.nativeElement as HTMLElement;
        expect(nativeElement.querySelector("#SOH_INFO_TEXT")).toBeNull();
        expect(nativeElement.querySelector("#SOH_ERROR_TEXT")).toBeNull();
        expect(nativeElement.textContent).not.toContain("STORAGE.ADMIN_MODAL.SOH.CAPACITY_MEASURING");
        expect(nativeElement.textContent).not.toContain("STORAGE.ADMIN_MODAL.SOH.ERROR");
    });
});
