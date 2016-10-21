import { Component } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { DataService } from '../../data/data.service';

@Component({
  selector: 'app-dess-monitor',
  templateUrl: './dess-monitor.component.html',
  styleUrls: ['./dess-monitor.component.css']
})
export class DessMonitorComponent {
  private device: any = {}; 

  constructor(
    private route: ActivatedRoute,
    private dataService: DataService
  ) {
    this.route.params.subscribe(params => {
      let id = +params['id'];
      this.dataService.getOne(id, ['BSMU_Battery_Stack_Overall_SOC']).then(json => {
        var res = JSON.parse(json);
        this.device = {
          soc: res.current[0].BSMU_Battery_Stack_Overall_SOC        
        };
      });
    })
  }
}
