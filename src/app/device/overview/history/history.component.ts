import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Device } from '../../../shared/shared';

import * as d3 from 'd3';
import * as d3shape from 'd3-shape';
import * as moment from 'moment';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit, OnDestroy {

  @Input()
  public device: Device;

  private dataSoc = [];
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  ngOnInit() {
    if (this.device != null) {
      this.device.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
        /**
         * Query state of charge of all ESS devices
         */
        // get all configured ESS devices
        let essDevices: string[] = [];
        let natures = config._meta.natures;
        for (let nature in natures) {
          if (natures[nature].implements.includes("EssNature")) {
            essDevices.push(nature);
          }
        }
        // create channels for query
        let channels = {};
        essDevices.forEach(device => channels[device] = ['Soc']);
        // execute query (for today)
        this.device.query(moment(), moment(), channels);

        /**
         * Receive data and prepare for chart
         */
        this.device.queryreply.takeUntil(this.ngUnsubscribe).subscribe(queryreplies => {
          // prepare datas array and prefill with each device
          let datas: {
            [thing: string]: {
              name: moment.Moment,
              value: number
            }[]
          } = {};
          essDevices.forEach(device => datas[device] = []);

          if (queryreplies != null) {
            for (let reply of queryreplies) {
              // read timestamp and soc of each device' reply
              let timestamp = moment(reply.time);
              let soc = 0;
              essDevices.forEach(device => {
                if (device in reply.channels && "Soc" in reply.channels[device] && reply.channels[device]["Soc"]) {
                  datas[device].push({ name: timestamp, value: reply.channels[device].Soc });
                }
              });
            }
          }
          // refresh global dataSoc array
          this.dataSoc = [];
          for (let device in datas) {
            this.dataSoc.push({
              name: "Ladezustand (" + device + ")",
              series: datas[device]
            });
          }
        });
      });
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  colorScheme = {
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA']
  };
}
