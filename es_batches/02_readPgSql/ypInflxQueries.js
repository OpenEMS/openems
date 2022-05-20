
// prepare meter fields for reading
function generateInfluxDbQueryMeterSection(meterName){
  const Influx_queryReadMeterIntro = `
		"${meterName}/EnergyExpFeact1"/1000 as ${meterName}_ProductionF1, 
		"${meterName}/EnergyExpFeact2"/1000 as ${meterName}_ProductionF2, 
		"${meterName}/EnergyExpFeact3"/1000 as ${meterName}_ProductionF3, 
		"${meterName}/EnergyExpFeactsys"/1000 as ${meterName}_ProductionSys, 
		"${meterName}/EnergyImpFeact1"/1000 as ${meterName}_ConsumptionF1, 
		"${meterName}/EnergyImpFeact2"/1000 as ${meterName}_ConsumptionF2, 
		"${meterName}/EnergyImpFeact3"/1000 as ${meterName}_ConsumptionF3, 
		"${meterName}/EnergyImpFeactsys"/1000 as ${meterName}_ConsumptionSys`;
    return Influx_queryReadMeterIntro;
}




function generateGivenMetersInfluxDbQuery(metersFields, dateStart, dateEnd){
  const Influx_queryReadMeterIntro = `SELECT 
		"time", 
		${metersFields}
	from 
		data 
	where 
		time >= '${dateStart}' 
	and 
		time < '${dateEnd}'
  tz('Europe/Zurich')
    `;
  return Influx_queryReadMeterIntro;
}



//
//
//
// query meter data on the Influx storage
function generateAllMetersInfluxDbQuery(meterName, dateStart, dateEnd){
  return generateGivenMetersInfluxDbQuery(theMmtr, dateStart, dateEnd);
}



//
// query meter data on the Influx storage
function generateSingleMeterInfluxDbQuery(meterName, dateStart, dateEnd){
  const theMmtr = generateInfluxDbQueryMeterSection(meterName);
  return generateGivenMetersInfluxDbQuery(theMmtr, dateStart, dateEnd);
}


module.exports = {
  generateInfluxDbQueryMeterSection,
  generateGivenMetersInfluxDbQuery,
  generateSingleMeterInfluxDbQuery,
  generateAllMetersInfluxDbQuery,
}