// @ts-strict-ignore
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
      expect(LoginComponent.preprocessCredentials(password, username)).toEqual({ password: "password", username: "username" });
    }
    {
      // Only Password - OpenEMS Edge
      expect(LoginComponent.preprocessCredentials(password)).toEqual({ password: "password" });
    }
    {
      // Password is null
      expect(LoginComponent.preprocessCredentials(null)).toEqual({ password: undefined });
    }
    {
      // Username is null
      expect(LoginComponent.preprocessCredentials(password, null)).toEqual({ password: "password" });
    }
    {
      // Username and password are null
      expect(LoginComponent.preprocessCredentials(null, null)).toEqual({ password: undefined });
    }
    {
      // Username in Upper case
      expect(LoginComponent.preprocessCredentials(password, username.toUpperCase())).toEqual({ password: "password", username: "username" });
    }
  });
});
