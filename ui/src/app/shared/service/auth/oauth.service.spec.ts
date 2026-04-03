import { signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { URLOpenListenerEvent } from "@capacitor/app";
import { ModalController } from "@ionic/angular";
import { TranslateLoader, TranslateModule } from "@ngx-translate/core";
import { Theme } from "src/app/edge/history/shared";
import { PlatFormService } from "src/app/platform.service";
import { User } from "../../jsonrpc/shared";
import { MyTranslateLoader, Language } from "../../type/language";
import { RouteService } from "../route.service";
import { Service } from "../service";
import { UserService } from "../user.service";
import { OAuthService } from "./oauth.service";

describe("OAuthService", () => {
    let service: OAuthService;
    const userServiceSpyObj = jasmine.createSpyObj<UserService>("UserService", ["currentUser"], {
        currentUser: signal(new User("", "", "admin", "", true, { theme: Theme.LIGHT })),
    });
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, fallbackLang: Language.DEFAULT.key }),
            ],
            providers: [
                { provide: UserService, useValue: userServiceSpyObj },
                PlatFormService,
                Service,
                OAuthService,
                RouteService,
                ModalController,
            ],
        });
        service = TestBed.inject(OAuthService);
    });

    it("should be created", () => {
        expect(service).toBeTruthy();
    });

    describe("-getCode", () => {
        it("should extract code from valid URL with query parameters", () => {
            const event = {
                url: "com.example.app://oauthcallback?code=abc123",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBe("abc123");
        });

        it("should remove fragment identifier from code", () => {
            const event = {
                url: "com.example.app://oauthcallback?code=abc123#",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBe("abc123");
        });

        it("should return code without fragment when fragment exists", () => {
            const event = {
                url: "com.example.app://oauthcallback?code=xyz789&state=test#section",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBe("xyz789");
        });

        it("should return null when no query string exists", () => {
            const event = {
                url: "com.example.app://oauthcallback",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBeNull();
        });

        it("should return null when code parameter is missing", () => {
            const event = {
                url: "com.example.app://oauthcallback?state=test&redirect_uri=http://localhost",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBeNull();
        });

        it("should return null when code parameter is empty", () => {
            const event = {
                url: "com.example.app://oauthcallback?code=",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBeNull();
        });

        it("should handle code with multiple query parameters", () => {
            const event = {
                url: "com.example.app://oauthcallback?state=xyz&code=mycode123&scope=openid",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBe("mycode123");
        });

        it("should return null when URL split returns empty array", () => {
            const event = {
                url: "?",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBeNull();
        });

        it("should handle code with special characters", () => {
            const event = {
                url: "com.example.app://oauthcallback?code=abc-123_xyz.456",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBe("abc-123_xyz.456");
        });

        it("should remove only the first fragment marker from code", () => {
            const event = {
                url: "com.example.app://oauthcallback?code=abc123#fragment1",
            } as URLOpenListenerEvent;

            const result = OAuthService["getCode"](event);
            expect(result).toBe("abc123");
        });
    });
});
