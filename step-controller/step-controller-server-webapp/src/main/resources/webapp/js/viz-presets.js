function PerformanceDashboard(executionId, measurementType) {

	var widgetsArray = [];

	//addLastMeasurements(widgetsArray);	
	addAggregatesOverTimeTpl(widgetsArray);
	addLastMeasurementsTpl(widgetsArray);
	addAggregatesSummaryTpl(widgetsArray);
	//TODO:addMeasurementExplorer(widgetsArray) //with paging
	//TODO:addAggregatesSummaryOptzTpl(widgetsArray);
	var dashboardObject = new Dashboard(
			'Transaction Performance',
			new DashboardState(
					new GlobalSettings(
							[new Placeholder("__eId__", executionId, false), new Placeholder("__measurementType__", measurementType, false)],
							false,
							false,
							'Global Settings',
							3000
					),
					widgetsArray,
					'Viz Dashboard',
					'aggregated',
					new DefaultDashboardGui()
			)
	);

	dashboardObject.oid = "perfDashboardId";
	return dashboardObject;
};

function RTMAggBaseQueryTmpl(metric, transform){
	return new AsyncQuery(
			null,
			new Service(//service
					"/rtm/rest/aggregate/get", "Post",
					"",//templated
					new Preproc("function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].key, workData[i].value);}return newRequestFragment;}"),
					new Postproc("", "",[], "function(response){if(!response.data.payload){console.log('No payload ->' + JSON.stringify(response)); return null;}return [{ placeholder : '__streamedSessionId__', value : response.data.payload.streamedSessionId, isDynamic : false }];}", "")
			),
			new Service(//callback
					"/rtm/rest/aggregate/refresh", "Post",
					"{\"streamedSessionId\": \"__streamedSessionId__\"}",
					new Preproc("function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].placeholder, workData[i].value);}return newRequestFragment;}"),
					new Postproc("function(response){return response.data.payload.stream.complete;}", transform ,[{"key" : "metric", "value" : metric, "isDynamic" : false}], {}, ""))
	);
};

function RTMAggBaseTemplatedQueryTmpl(metric, pGranularity, transform){
	return new TemplatedQuery(
			"Template",
			new RTMAggBaseQueryTmpl(metric, transform),
			new DefaultPaging(),
			new Controls(
					new Template(
							"{ \"selectors1\": [{ \"textFilters\": [{ \"key\": \"eId\", \"value\": \"__eId__\", \"regex\": \"false\" }, { \"key\": \"type\", \"value\": \"__measurementType__\", \"regex\": \"false\" }], \"numericalFilters\": [] }], \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"__granularity__\", \"aggregateService.groupby\": \"name\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" } }",
							"",
							[new Placeholder("__granularity__", pGranularity, false)]
					)
			)
	);
};

var addAggregatesSummaryTpl = function(widgetsArray){
	var summaryTransform = "function (response) {\r\n    //var metrics = response.data.payload.metricList;\r\n    var metrics = [\"cnt\",\"avg\", \"min\", \"max\", \"tpm\", \"tps\", \"90th pcl\"];\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    if (payload && payloadKeys.length > 0) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[0]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            for (i = 0; i < metrics.length; i++) {\r\n                var metric = metrics[i];\r\n                if (payload[payloadKeys[0]][serieskeys[j]][metric]) {\r\n                    retData.push({\r\n                        x: metric,\r\n                        y: Math.round(payload[payloadKeys[0]][serieskeys[j]][metric]),\r\n                        z: serieskeys[j]\r\n                    });\r\n                }\r\n            }\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var standalone = new Widget(getUniqueId(), new DefaultWidgetState(), new DashletState("Transaction summary", false, 0, {}, new ChartOptions('seriesTable'), new Config('Off', false, false, ''), new RTMAggBaseTemplatedQueryTmpl("sum", "max", summaryTransform), new DefaultGuiClosed(), new DefaultInfo()));
	widgetsArray.push(standalone);
};

var addAggregatesOverTimeTpl = function(widgetsArray){
	var overtimeTransform = "function (response, args) {\r\n    var metric = args.metric;\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: payload[payloadKeys[i]][serieskeys[j]][metric],\r\n                z: serieskeys[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var overtimeFillBlanksTransform = "function (response, args) {\r\n    var metric = args.metric;\r\n    var retData = [], series = [];\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            if(!series.includes(serieskeys[j])){\r\n                series.push(serieskeys[j]);\r\n            }\r\n        }\r\n    }\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < series.length; j++) {\r\n            var yval;\r\n            if(payload[payloadKeys[i]][serieskeys[j]] && payload[payloadKeys[i]][serieskeys[j]][metric]){\r\n              yval = payload[payloadKeys[i]][serieskeys[j]][metric];\r\n            }else{\r\n              //console.log('missing dot: x=' + payloadKeys[i] + '; series=' + series[j]);\r\n              yval = 0;\r\n            }\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: yval,\r\n                z: series[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var config = getMasterSlaveConfig("raw", "Average Response Time over time (ms)", "Transaction count over time (#)");

	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new ChartOptions('lineChart'), config.masterconfig, new RTMAggBaseTemplatedQueryTmpl("avg", "auto", overtimeTransform), new DefaultGuiClosed(), new DefaultInfo()));
	//var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new ChartOptions('lineChart'), config.slaveconfig, new RTMAggBaseTemplatedQueryTmpl("cnt", "auto", overtimeTransform), new DefaultGuiClosed(), new DefaultInfo()));
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new ChartOptions('stackedAreaChart', false, true), config.slaveconfig, new RTMAggBaseTemplatedQueryTmpl("cnt", "auto", overtimeFillBlanksTransform), new DefaultGuiClosed(), new DefaultInfo()));

	widgetsArray.push(master);
	widgetsArray.push(slave);
};

//No paging: FACTOR 100 via template
var addLastMeasurementsTpl = function(widgetsArray){
	function RTMLatestMeasurementBaseQueryTmpl(){
		return new SimpleQuery(
				"Raw", new Service(
						"/rtm/rest/measurement/latest", "Post",
						"",
						new Preproc("function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].key, workData[i].value);}return newRequestFragment;}"),
						new Postproc("", "function (response, args) {\r\n    var x = 'begin', y = 'value', z = 'name';\r\n    var retData = [], index = {};\r\n    var payload = response.data.payload;\r\n    for (var i = 0; i < payload.length; i++) {\r\n        retData.push({\r\n            x: payload[i][x],\r\n            y: payload[i][y],\r\n            z: payload[i][z]\r\n        });\r\n    }\r\n    return retData;\r\n}",
								[], {}, "")
				)
		);
	};

	function RTMLatestMeasurementTemplatedQuery(){
		return new TemplatedQuery(
				"Template",
				new RTMLatestMeasurementBaseQueryTmpl(),
				new DefaultPaging(),
				//new Paging("On", new Offset("__FACTOR__", "return 0;", "return value + 1;", "if(value > 0){return value - 1;} else{return 0;}"), null),
				new Controls(
						new Template(
								"{ \"selectors1\": [{ \"textFilters\": [{ \"key\": \"eId\", \"value\": \"__eId__\", \"regex\": \"false\" }, { \"key\": \"type\", \"value\": \"__measurementType__\", \"regex\": \"false\" }], \"numericalFilters\": [] }], \"serviceParams\": { \"measurementService.nextFactor\": \"__FACTOR__\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \"name\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" } }",
								"",
								[new Placeholder("__FACTOR__", "100", false)]
						)
				)
		);
	};

	var config = getMasterSlaveConfig("transformed", "Last 100 Measurements - Scattered values (ms)", "Last 100 Measurements - Value table (ms)");

	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new ChartOptions('scatterChart'), config.masterconfig, new RTMLatestMeasurementTemplatedQuery(), new DefaultGuiClosed(), new DefaultInfo()) );
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new ChartOptions('seriesTable'), config.slaveconfig, new RTMLatestMeasurementTemplatedQuery(), new DefaultGuiClosed(), new DefaultInfo()) );

	widgetsArray.push(master);
	//widgetsArray.push(slave);
};

var getMasterSlaveConfig = function(rawOrTransformed, masterTitle, slaveTitle){
	var masterId, slaveId, masterTitle, slaveTitle, masterConfig, slaveConfig, datatype;

	if(rawOrTransformed === 'raw'){
		datatype = 'state.data.rawresponse';
	}else{
		datatype = 'state.data.transformed';
	}

	var random = getUniqueId();
	masterId = random + "-master";
	slaveId = random + "-slave";

	masterConfig = new Config('Off', true, false, 'unnecessaryAsMaster');
	slaveConfig = new Config('Off', false, true, datatype);
	slaveConfig.currentmaster = {
			oid: masterId,
			title: masterTitle
	};

	return {masterid: masterId, slaveid: slaveId, mastertitle: masterTitle, slavetitle: slaveTitle, masterconfig : masterConfig, slaveconfig: slaveConfig};
};

function StaticPresets() {
	return {
		queries: [],
			controls: {
				templates: []
			},
			configs: []
	};
}