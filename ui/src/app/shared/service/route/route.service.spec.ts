import { TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { CookieService } from "ngx-cookie-service";
import { Subject } from "rxjs";
import { RouteService } from "./route.service";

describe("RouteService authentication navigation", () => {
    let service: RouteService;
    let router: jasmine.SpyObj<Router>;

    function setUrl(url: string): void {
        const getter = Object.getOwnPropertyDescriptor(router, "url")?.get as jasmine.Spy;
        getter.and.returnValue(url);
        service.currentUrl.set(url);
    }

    beforeEach(() => {
        router = jasmine.createSpyObj<Router>("Router", ["navigate", "navigateByUrl"], {
            events: new Subject(),
            url: "/",
        });
        const cookies = jasmine.createSpyObj<CookieService>("CookieService", ["get"]);
        cookies.get.and.returnValue("");
        TestBed.configureTestingModule({
            providers: [
                RouteService,
                { provide: Router, useValue: router },
                { provide: CookieService, useValue: cookies },
            ],
        });
        service = TestBed.inject(RouteService);
    });

    for (const url of ["/login"]) {
        it(`should fall back to overview from ${url}`, () => {
            setUrl(url);
            service.navigateAfterAuthentication();
            expect(router.navigate).toHaveBeenCalledOnceWith(["/overview"]);
        });
    }

    it("should preserve query parameters and fragments in a device deep link", () => {
        const url = "/device/edge0/live?componentId=meter0#details";
        setUrl(url);
        service.navigateAfterAuthentication();
        expect(router.navigateByUrl).toHaveBeenCalledOnceWith(url);
        expect(router.navigate).not.toHaveBeenCalled();
    });
});
