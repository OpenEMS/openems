import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Service } from "src/app/shared/shared";
import { FormlyFieldWaitingSpinnerComponent } from "./formly-field-waiting-spinner";

describe("FormlyFieldWaitingSpinnerComponent", () => {
    let fixture: ComponentFixture<FormlyFieldWaitingSpinnerComponent>;
    let service: jasmine.SpyObj<Service>;

    beforeEach(async () => {
        service = jasmine.createSpyObj<Service>("Service", [
            "startSpinner",
            "stopSpinner",
        ]);

        await TestBed.configureTestingModule({
            imports: [FormlyFieldWaitingSpinnerComponent],
            providers: [{ provide: Service, useValue: service }],
        }).compileComponents();

        fixture = TestBed.createComponent(FormlyFieldWaitingSpinnerComponent);
    });

    it("should call startSpinner with the spinner id on init", () => {
        fixture.detectChanges();

        expect(service.startSpinner).toHaveBeenCalledOnceWith(
            FormlyFieldWaitingSpinnerComponent.SELECTOR,
        );
    });

    it("should call stopSpinner with the spinner id on destroy", () => {
        fixture.detectChanges();
        fixture.destroy();

        expect(service.stopSpinner).toHaveBeenCalledOnceWith(
            FormlyFieldWaitingSpinnerComponent.SELECTOR,
        );
    });
});
