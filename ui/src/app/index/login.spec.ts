// @ts-strict-ignore
import { LoginComponent } from "./login.component";

describe('Login', () => {
  const password = " password ";
  const username = " username ";

  it('#trimCredentials should trim password and username', () => {
    {
      // Username and password - OpenEMS Backend
      expect(LoginComponent.trimCredentials(password, username)).toEqual({ password: "password", username: "username" });
    }
    {
      // Only Password - OpenEMS Edge
      expect(LoginComponent.trimCredentials(password)).toEqual({ password: "password" });
    }
    {
      // Password is null
      expect(LoginComponent.trimCredentials(null)).toEqual({ password: undefined });
    }
    {
      // Username is null
      expect(LoginComponent.trimCredentials(password, null)).toEqual({ password: "password" });
    }
    {
      // Username and password are null
      expect(LoginComponent.trimCredentials(null, null)).toEqual({ password: undefined });
    }
  });
});
