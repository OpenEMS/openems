import { Injectable } from '@angular/core';


@Injectable()
export class TemplateHelper {

  constructor(
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
  sort(array: string[]) {
    return array.sort();
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
}