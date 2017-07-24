import { Injectable } from '@angular/core';
import { WebsocketService } from './websocket.service'

@Injectable()
export class TemplateHelper {

  constructor(
    private websocketService: WebsocketService
  ) { }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object keys.
   * Source: https://stackoverflow.com/a/39896058
   */
  keys(object: {}): string[] {
    return Object.keys(object);
  }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object key value pairs.
   */
  keyvalues(object: {}): any[] | {} {
    if (!object) {
      return object;
    }
    let keyvalues = [];
    for (let key in object) {
      keyvalues.push({ key: key, value: object[key] });
    }
    return keyvalues;
  }

  /**
   * Returns true if an object has a property
   */
  has(object: {}, property: string): boolean {
    if (property in object) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns a sorted array
   */
  sort(obj: any[], ascending: boolean = true, property?: string) {
    if (obj == null) {
      return obj;
    }
    return obj.sort((a, b) => {
      if (property) {
        a = a[property];
        b = b[property];
      }
      let result = 0;
      if (a > b) {
        result = 1;
      } else if (a < b) {
        result = -1;
      }
      if (!ascending) {
        result *= -1;
      }
      return result;
    })
  }

  /**
   * Returns the short classname
   */
  classname(value): string {
    let parts = value.split(".");
    return parts[parts.length - 1];
  }

  /**
   * Creates a deep copy of the object
   */
  deepCopy(obj) {
    var copy;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
      copy = new Date();
      copy.setTime(obj.getTime());
      return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
      copy = [];
      for (var i = 0, len = obj.length; i < len; i++) {
        copy[i] = this.deepCopy(obj[i]);
      }
      return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
      copy = {};
      for (var attr in obj) {
        if (obj.hasOwnProperty(attr)) copy[attr] = this.deepCopy(obj[attr]);
      }
      return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
  }

  /**
   * Receive meta information for thing/channel/...
   */
  meta(identifier: string, type: 'controller' | 'channel'): {} {
    let property = type == 'controller' ? 'availableControllers' : type;
    let device = this.websocketService.currentDevice.getValue();
    if (device) {
      let config = device.config.getValue();
      let meta = config._meta[property];
      if (identifier in meta) {
        return (meta[identifier]);
      }
    }
    return null;
  }
}