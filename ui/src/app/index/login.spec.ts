// @ts-strict-ignore
import { TestBed } from "@angular/core/testing";
import { LoginComponent } from "./LOGIN.COMPONENT";

describe("Login", () => {
  const password = " password ";
  const username = " username ";

  beforeEach(() => {
    TEST_BED.CONFIGURE_TESTING_MODULE({
      declarations: [LoginComponent],
    }).compileComponents();
  });

  it("#preprocessCredentials should trim password and username and should lowerCase username", () => {
    {
      // Username and password - OpenEMS Backend
      expect(LOGIN_COMPONENT.PREPROCESS_CREDENTIALS(password, username)).toEqual({ password: "password", username: "username" });
    }
    {
      // Only Password - OpenEMS Edge
      expect(LOGIN_COMPONENT.PREPROCESS_CREDENTIALS(password)).toEqual({ password: "password" });
    }
    {
      // Password is null
      expect(LOGIN_COMPONENT.PREPROCESS_CREDENTIALS(null)).toEqual({ password: undefined });
    }
    {
      // Username is null
      expect(LOGIN_COMPONENT.PREPROCESS_CREDENTIALS(password, null)).toEqual({ password: "password" });
    }
    {
      // Username and password are null
      expect(LOGIN_COMPONENT.PREPROCESS_CREDENTIALS(null, null)).toEqual({ password: undefined });
    }
    {
      // Username in Upper case
      expect(LOGIN_COMPONENT.PREPROCESS_CREDENTIALS(password, USERNAME.TO_UPPER_CASE())).toEqual({ password: "password", username: "username" });
    }
  });
});
