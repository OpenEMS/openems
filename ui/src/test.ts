// TO NOT AUTOFORMAT THIS FILE AS IT WILL BREAK CI BUILD WITH FOLLOWING ERROR:
//   Error: zone-testing.js is needed for the fakeAsync() test helper but could not be found.
//   Please make sure that your environment includes zone.js/testing 
// (See https://stackoverflow.com/a/68797535/4137113)

// This file is a copy of https://github.com/angular/angular/blob/main/aio/src/test.ts

// This file is required by karma.conf.js and loads recursively all the .spec and framework files

import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting(),
);
