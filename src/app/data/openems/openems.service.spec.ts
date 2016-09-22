/* tslint:disable:no-unused-variable */

import { TestBed, async, inject } from '@angular/core/testing';
import { OpenemsServiceService } from './openems.service';

describe('Service: OpenemsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OpenemsServiceService]
    });
  });

  it('should ...', inject([OpenemsServiceService], (service: OpenemsServiceService) => {
    expect(service).toBeTruthy();
  }));
});
