

// influx
const Influx = require('influx');



//
// ==================================================================
//
//   Influx
//

// accounts data:
// influx
function influxAccount(dbName, _fields){
  let influxAccount = {
    host: '10.192.1.23',
    port: 8086,
    database: dbName,
    schema: [
      {
        measurement: 'data'
        , fields: _fields
        , tags: ['unit', 'KWH']
      }
    ]
  
  }
  return influxAccount;
}

//
// build fields schema info
function generateInfluxFieldsDescriptors(meterName, _fields){

  _fields[`${meterName}/EnergyExpFeact1`] = Influx.FieldType.FLOAT; 
  _fields[`${meterName}/EnergyExpFeact2`] = Influx.FieldType.FLOAT;
  _fields[`${meterName}/EnergyExpFeact3`] = Influx.FieldType.FLOAT;  
  _fields[`${meterName}/EnergyExpFeactsys`] = Influx.FieldType.FLOAT; 
  _fields[`${meterName}/EnergyImpFeact1`] = Influx.FieldType.FLOAT; 
  _fields[`${meterName}/EnergyImpFeact2`] = Influx.FieldType.FLOAT; 
  _fields[`${meterName}/EnergyImpFeact3`] = Influx.FieldType.FLOAT; 
  _fields[`${meterName}/EnergyImpFeactsys`] = Influx.FieldType.FLOAT;

  return _fields;

}

// influx connection
function startInfluxConnection(dbName, _fields){


  const influxJSO = influxAccount(dbName, _fields);

  const influx = new Influx.InfluxDB(influxJSO);
  return influx;
}


module.exports = {
  influxAccount,
  generateInfluxFieldsDescriptors,
  startInfluxConnection
}