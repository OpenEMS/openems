import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import * as moment from 'moment';

import { Device, Dataset } from '../../../shared/shared';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit, OnDestroy {

  @Input()
  public device: Device;

  public datasets: Dataset[];
  public labels: moment.Moment[];

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
          if (queryreplies == null) {
            // reset datasets and labels
            this.datasets = null;
            this.labels = null;
          } else {
            // prepare datas array and prefill with each device
            let datasets: {
              [thing: string]: number[];
            } = {};
            let labels: moment.Moment[] = [];
            essDevices.forEach(device => datasets[device] = []);
            for (let reply of queryreplies) {
              // read timestamp and soc of each device' reply
              labels.push(moment(reply.time));
              essDevices.forEach(device => {
                let soc = 0;
                if (device in reply.channels && "Soc" in reply.channels[device] && reply.channels[device]["Soc"]) {
                  soc = Math.round(reply.channels[device].Soc);
                }
                datasets[device].push(soc);
              });
            }
            // refresh global datasets and labels
            this.datasets = [];
            for (let device in datasets) {
              this.datasets.push({
                label: "Ladezustand (" + device + ")",
                data: datasets[device]
              });
            }
            this.labels = labels;
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
