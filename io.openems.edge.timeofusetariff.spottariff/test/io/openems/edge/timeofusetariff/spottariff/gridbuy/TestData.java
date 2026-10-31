package io.openems.edge.timeofusetariff.spottariff.gridbuy;

class TestData {
	protected static final String SCHEDULE = """
			[
			  {
			    "year": 2026,
			    "tariffs": {
			      "low": 0.10,
			      "standard": 0.20,
			      "high": 0.30
			    },
			    "quarters": [
			      {
			        "quarter": 1,
			        "dailySchedule": [
			          { "tariff": "low", "from": "00:00", "to": "06:00" },
			          { "tariff": "standard", "from": "06:00", "to": "18:00" },
			          { "tariff": "high", "from": "18:00", "to": "23:59" }
			        ]
			      },
				  {
			        "quarter": 2,
			        "dailySchedule": [ ]
			      },
				  {
			        "quarter": 3,
			        "dailySchedule": [ ]
			      },
				  {
			        "quarter": 4,
			        "dailySchedule": [
			          { "tariff": "low", "from": "00:00", "to": "06:00" },
			          { "tariff": "standard", "from": "06:00", "to": "18:00" },
			          { "tariff": "high", "from": "18:00", "to": "23:59" }
			        ]
			      }
			    ]
			  }
			]
			""";
}
