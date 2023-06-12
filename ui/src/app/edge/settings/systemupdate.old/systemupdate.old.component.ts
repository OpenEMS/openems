import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { compareVersions } from 'compare-versions';
import { Subject, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ExecuteSystemCommandRequest } from 'src/app/shared/jsonrpc/request/executeCommandRequest';
import { ExecuteSystemCommandResponse } from 'src/app/shared/jsonrpc/response/executeSystemCommandResponse';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';

class Package {

  public currentVersion = null;
  public latestVersion = null;
  public isUpdateAvailable: boolean | null = null;

  constructor(
    public readonly name: string,
    public readonly description: string
  ) {
  }

  public setVersions(current: string, latest: string) {
    this.currentVersion = current;
    this.latestVersion = latest;
    if (current == null || latest == null) {
      this.isUpdateAvailable = null;
      return;
    }
    this.isUpdateAvailable = compareVersions(this.latestVersion, this.currentVersion) > 0;
  }

  public resetVersions() {
    this.setVersions(null, null);
  }
};

@Component({
  selector: SystemUpdateOldComponent.SELECTOR,
  templateUrl: './systemupdate.old.component.html'
})
export class SystemUpdateOldComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "systemUpdateOld";
  private static readonly USERNAME = "root";
  private static readonly PASSWORD = "hidden";
  private static readonly MAX_LOG_ENTRIES = 200;

  public edge: Edge = null;
  private ngUnsubscribe = new Subject<void>();
  public isCurrentlyInstalling: Boolean = null;

  public logLines: {
    time: string,
    level: string,
    color: string,
    message: string,
    source: string
  }[] = [];

  public package = new Package("fems", "FEMS");

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent("", this.route).then(edge => {
      this.edge = edge;

      // Update version information now and every minute
      const source = timer(0, 30000);
      source.pipe(
        takeUntil(this.ngUnsubscribe)
      ).subscribe(ignore => {
        this.updateVersions();
        this.readLog();
      });
    });
  }

  ngOnDestroy() {
    this.stopUpdateVersions();
  }

  private updateVersions() {
    this.log("INFO", "Lese Versionen von FEMS");

    let command = ""
      // Read currently installed version
      + "dpkg-query --showformat='" + this.package.name + " current:${Version}\\n' --show " + this.package.name + "; "
      // Read latest version
      + "echo -n \"" + this.package.name + " latest:\"; "
      + "wget -qO- http://fenecon.de/debian-test/" + this.package.name + "-latest.version; "
      // Read running deb/dpkg processes
      + "ps ax | grep 'update-fems.sh\\|wget.*deb\\|dpkg' | wc -l";

    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_host",
        payload: new ExecuteSystemCommandRequest({
          username: SystemUpdateOldComponent.USERNAME,
          password: SystemUpdateOldComponent.PASSWORD,
          timeoutSeconds: 60,
          runInBackground: false,
          command: command
        })
      })).then(response => {
        let result = (response as ExecuteSystemCommandResponse).result;
        this.log("INFO", "Versionen gelesen: "
          + result.stdout.join(", ") + " | "
          + result.stderr.join(", ")
            .replace("dpkg-query: no packages found matching fems", "veralteter Stand mit openems-core/openems-core-fems Paketen")
        );

        if (result.stderr.length >= 1 && result.stderr[0].includes("dpkg-query: no packages found matching fems")) {
          // Handle migration from 'openems-core' and 'openems-core-fems' to 'fems'
          this.package.setVersions(this.edge.version + "-veraltet", result.stdout[0].split(":")[1]);

        } else if (result.stderr.length >= 1 && result.stderr[0].includes("getcwd: cannot access parent directories")) {
          // ignore
          this.package.setVersions(this.edge.version, result.stdout[0].split(":")[1]);

        } else {
          // Default package version handling
          this.package.setVersions(result.stdout[0].split(":")[1], result.stdout[1].split(":")[1]);
        }

        // Is currently installing packages?
        let isCurrentlyInstallingLog = result.stdout[result.stdout.length - 1];
        if (isCurrentlyInstallingLog && parseInt(isCurrentlyInstallingLog) > 2) {
          this.isCurrentlyInstalling = true;
        } else {
          this.isCurrentlyInstalling = false;
        }

        // Stop regular check if there is no Update available
        if (!this.package.isUpdateAvailable) {
          this.stopUpdateVersions();
          this.log("INFO", "Update abgeschlossen");
        }

      }).catch(reason => {
        this.log("ERROR", reason.error.message);
      });
  }

  public updatePackage(pkg: Package) {
    this.isCurrentlyInstalling = true;
    this.log("INFO", "Starte FEMS Update");

    // Start Download
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_host",
        payload: new ExecuteSystemCommandRequest({
          username: SystemUpdateOldComponent.USERNAME,
          password: SystemUpdateOldComponent.PASSWORD,
          timeoutSeconds: 300,
          runInBackground: false,
          command: "which at || DEBIAN_FRONTEND=noninteractive apt-get -y install at; "
            + "echo 'wget http://fenecon.de/debian-test/update-fems.sh -O /tmp/update-fems.sh && chmod +x /tmp/update-fems.sh && /tmp/update-fems.sh' | at now"
        })

      })).catch(reason => {
        this.log("ERROR", reason.error.message);
      });
  }

  private readLog() {
    if (this.isCurrentlyInstalling) {
      this.edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_host",
          payload: new ExecuteSystemCommandRequest({
            username: SystemUpdateOldComponent.USERNAME,
            password: SystemUpdateOldComponent.PASSWORD,
            timeoutSeconds: 300,
            runInBackground: false,
            command: "tail -n 5 /proc/$(pgrep --full /tmp/update-fems.sh --oldest)/fd/1"
          })

        })).then(response => {
          let result = (response as ExecuteSystemCommandResponse).result;
          for (let stdout of result.stdout) {
            this.log("OTHER", stdout);
          }

        }).catch(reason => {
          this.log("ERROR", reason.error.message);
        });
    }
  }

  private getColor(level): string {
    switch (level) {
      case 'INFO':
        return 'green';
      case 'WARN':
        return 'orange';
      case 'DEBUG':
        return 'gray';
      case 'ERROR':
        return 'red';
    };
    return 'black';
  }

  private log(level: 'INFO' | 'ERROR' | 'OTHER', message: string) {
    this.logLines.unshift({
      time: new Date().toLocaleString(),
      color: this.getColor(level),
      level: level,
      source: "",
      message: message
    });
  }

  private stopUpdateVersions() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}