



//
// ==================================================================
//
// postgres queries
//
// extract the read tasks list which still need to be performed
function generateReadTasksQuery() {

  const queryReadTasks = `
	SELECT 
		* 
	FROM 
		"youpower-billingmeters"."MeterReadingTasksList" "MRTL"
	WHERE
		"MRTL"."Completed"=false
	ORDER BY
    "MRTL"."MeterBillingLevelSetId",
    "MRTL"."MeterBillingLevelId",
    "MRTL"."MeasureDateStart",
    "MRTL"."Id"
	`;
  return queryReadTasks;
}

//
//
//
// list meters to be read in a given billing levels set
function generateMetersReadingsFromReadTasksQuery(billingLevelsSetId, idBillingLevel) {

  const queryReadTasks = `
	SELECT 
		* 
	FROM 
		"youpower-meters"."MetersListForReading" "MLFR"
  JOIN 
    "youpower-billingmeters"."MeterBillLevelsToMeterBillLevelsSets" "MBTMS"
  ON 
    "MBTMS"."MeterBillLevelsSetId" = "MLFR"."idbillinglevelsset"
  WHERE
		"MLFR"."idbillinglevelsset"=${billingLevelsSetId}
  AND
    "MBTMS"."MeterBillLevelId"=${idBillingLevel}
	ORDER BY
		"MLFR"."meterid"
	`;
  return queryReadTasks;
}






module.exports = {
  generateReadTasksQuery,
  generateMetersReadingsFromReadTasksQuery
}