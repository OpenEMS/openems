console.log('hello world');

const Influx = require('influx');

const express = require('express')
const http = require('http')
const os = require('os')

const app = express()





const influx = new Influx.InfluxDB({
  host: '10.192.1.23',
  port: 8086,
  database: 'Romano',
  schema: [
    {
      measurement: 'data',
      fields: { 
        "meter15/PowerFpsys": Influx.FieldType.FLOAT 
       , "meter18/PowerFpsys": Influx.FieldType.FLOAT
      }
      , tags: ['unit', 'KW']
    }
  ]
});

influx.getDatabaseNames()
  .then(names => {
    if (!names.includes('Romano')) {
      console.log("db Romano was not in");
      exit(0);
    }
  })
  .then(() => {
//    app.listen(app.get('port'), () => {
//      console.log(`Listening on ${app.get('port')}.`);
    
    console.log("DB is in");
  })
  .catch(error => console.log({ error }));

// SELECT "time", "meter18/PowerFpsys"/1000 as Production, "meter15/PowerFpsys"/1000 as Consumption from data


function getInfluxData() {
  influx.query(`

SELECT "time", "meter18/PowerFpsys"/1000 as Production, "meter15/PowerFpsys"/1000 as Consumption from data limit 10
  `).then(result => {
    console.log(result);
  }).catch(err => {
    console.log(err.stack);
  })
}


getInfluxData()

