import { TestBed } from "@angular/core/testing";
import { LoginComponent } from "./login.component";

describe("Login", () => {
    const password = " password ";
    const username = " username ";

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [LoginComponent],
        }).compileComponents();
    });

    it("#preprocessCredentials should trim password and username and should lowerCase username", () => {
        {
            // Username and password - OpenEMS Backend
            expect(LoginComponent.preprocessCredentials(password, username)).toEqual({
                password: "password",
                username: "username",
            });
        }
        {
            // Only Password - OpenEMS Edge
            expect(LoginComponent.preprocessCredentials(password)).toEqual({ password: "password" });
        }
        {
            // Whitespace-only password is trimmed to empty string
            expect(LoginComponent.preprocessCredentials("   ")).toEqual({ password: "" });
        }
        {
            // Username in Upper case
            expect(LoginComponent.preprocessCredentials(password, username.toUpperCase())).toEqual({
                password: "password",
                username: "username",
            });
        }
    });
});
