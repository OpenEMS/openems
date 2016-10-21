/* tslint:disable:no-unused-variable */

import { TestBed, async, inject } from '@angular/core/testing';
import { OdooDataService } from './odoo-data.service';

describe('Service: OdooService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OdooDataService]
    });
  });

  it('should ...', inject([OdooDataService], (service: OdooDataService) => {
    expect(service).toBeTruthy();
  }));
});
