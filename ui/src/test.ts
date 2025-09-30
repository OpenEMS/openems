// TO NOT AUTOFORMAT THIS FILE AS IT WILL BREAK CI BUILD WITH FOLLOWING ERROR:
//   Error: zone-TESTING.JS is needed for the fakeAsync() test helper but could not be found.
//   Please make sure that your environment includes ZONE.JS/testing
// (See https://STACKOVERFLOW.COM/a/68797535/4137113)

// This file is a copy of https://GITHUB.COM/angular/angular/blob/main/aio/src/TEST.TS

// This file is required by KARMA.CONF.JS and loads recursively all the .spec and framework files

import "ZONE.JS/testing";

import { getTestBed } from "@angular/core/testing";
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from "@angular/platform-browser-dynamic/testing";

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting(),
);
