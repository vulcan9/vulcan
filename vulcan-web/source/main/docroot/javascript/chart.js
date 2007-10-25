/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2006 Chris Eldredge
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/* $Id$
 * $HeadURL: https://vulcan.googlecode.com/svn/trunk/vulcan-web/source/main/docroot/javascript/widgets.js $
 */

function drawSuccessRateGraph(form) {
	var intervalSelector = document.getElementById("intervalSelector");
	var interval = parseInt(intervalSelector.options[intervalSelector.selectedIndex].value);
	
	var samples = {};
	var xTicks = [];
	
	var minX = buildData[0].completionDate.time / interval;
	var maxX = buildData[0].completionDate.time / interval;
	
	for (var i=0; i<buildData.length; i++) {
		var projectName = buildData[i].name;
		var x = parseInt(buildData[i].completionDate.time / interval);
		var passCount = 0;
		var total = 0;
		
		while (i<buildData.length && x==parseInt(buildData[i].completionDate.time / interval)) {
			if (buildData[i].status == "PASS") {
				passCount++;
			}
			
			total++;
			i++;
		}
		
		addSample(samples, projectName, [ x, passCount * 100 / total ]);
		var dt = new Date(x * interval);
		xTicks[xTicks.length] = {label:(dt.getMonth()+1)+"/"+dt.getDate()+"/"+dt.getFullYear(), v:x};
		
		if (x < minX) {
			minX = x;
		} else if (x > maxX) {
			maxX = x;
		}
	}
	
	var yTicks = [];
	for (var i=0; i<=10; i++) {
		yTicks[i] = {label:(i*10)+"%", v:i*10};
	}
	
	var layoutOptions = {
		"xAxis": [minX, maxX],
		"xTicks": xTicks,
		"yAxis": [0, 100],
		"yTicks": yTicks,
		"xOriginIsZero": false
	};

	var layout = new PlotKit.Layout("line", layoutOptions);
	for (var key in samples) {
		layout.addDataset(key, samples[key]);
	}

	layout.evaluate();
	
	var canvas = MochiKit.DOM.getElement("myCanvas");
	
	var plotOptions = {
		//"colorScheme": [Color.fromName("red"), Color.fromName("blue")],
		"colorScheme": PlotKit.Base.colorScheme(),
		"shouldStroke": true,
		"shouldFill": false
	};
	
	var plotter = new PlotKit.SweetCanvasRenderer(canvas, layout, plotOptions);
	plotter.clear();
	plotter.render();
 
	legend = new LegendRenderer("myLegend", layout, plotOptions);
	legend.clear();
	legend.render();
}

function drawTotalBuildsGraph() {
	var fullCount = 0;
	var incrCount = 0;
	for (var i=0; i < buildData.length; i++) {
		if (buildData[i].updateType == "Full") {
			fullCount++;
		} else {
			incrCount++;
		}
	}
	
	var xTicks = [{label: "Full", v: 0}, {label: "Incremental", v: 1}];
	var layout = new PlotKit.Layout("bar", {"xTicks": xTicks});
	
	layout.addDataset("x", [[0, fullCount], [1, incrCount]]);
	layout.evaluate();
	
	var canvas = MochiKit.DOM.getElement("myCanvas");
	var plotOptions = {
		"shouldStroke": false,
		"shouldFill": false
	};
	
	var plotter = new PlotKit.SweetCanvasRenderer(canvas, layout, plotOptions);
	plotter.render();
 
 	return false;
}

function initMetricsGraph(form) {
	if (!window.metrics) {
		window.metrics = {};
		
		for (var i=0; i<buildData.length; i++) {
			if (!buildData[i].metrics || buildData[i].metrics.length == 0) {
				continue;
			}
			for (var j=0; j<buildData[i].metrics.length; j++) {
				window.metrics[buildData[i].metrics[j].key] = buildData[i].metrics[j].label;
			}
		}
	}
	
	var ul = document.createElement("ul");
	var count = 0;
	ul.setAttribute("class", "metaDataOptions");
	
	for (var key in window.metrics) {
		var li = document.createElement("li");
		
		var checkbox = document.createElement("input");
		checkbox.type = "checkbox";
		checkbox.value = key;
		checkbox.id = "chkMetric" + count;
		
		var label = document.createElement("label");
		label.setAttribute("for", "chkMetric" + count);
		label.appendChild(document.createTextNode(window.metrics[key]));

		li.appendChild(checkbox);
		li.appendChild(label);
		
		ul.appendChild(li);
		count++;
	}
	
	MochiKit.DOM.replaceChildNodes(form, ul);
}

function drawMetricsGraph(form) {
	var inputs = form.getElementsByTagName("input");
	
	var keys = {};
	
	for (var i=0; i<inputs.length; i++) {
		if (inputs[i].checked) {
			keys[inputs[i].value] = window.metrics[inputs[i].value];
		}
	}
	
	var samples = {};
	
	var minX = buildData[0].completionDate.time;
	var maxX = buildData[0].completionDate.time;
	
	for (var i=0; i<buildData.length; i++) {
		var x = buildData[i].completionDate.time;
		var sampleAdded = false;
		
		for (var j=0; j<buildData[i].metrics.length; j++) {
			var key = buildData[i].metrics[j].key;
			if (keys[key]) {
				addSample(samples, keys[key], [x, parseInt(buildData[i].metrics[j].value)]);
				sampleAdded = true;
			}
		}
		
		if (!sampleAdded) {
			continue;
		}
		
		if (x < minX) {
			minX = x;
		} else if (x > maxX) {
			maxX = x;
		}
	}
	
	var delta = maxX - minX;
	var adjustment = delta * 10 / 100;
	
	maxX += adjustment;
	minX -= adjustment;
	 
	var layoutOptions = {
		"xAxis": [minX, maxX],
		"xTicks": createLabels(buildData, 4),
		//"yTicks": [{label:"15:00", v:900}, {label:"30:00", v:1800}, {label:"45:00", v:2700}, {label:"60:00", v:3600}],
		"xOriginIsZero": false
	};

	var layout = new PlotKit.Layout("line", layoutOptions);
	for (var key in samples) {
		layout.addDataset(key, samples[key]);
	}

	layout.evaluate();
	
	var canvas = MochiKit.DOM.getElement("myCanvas");
	
	var plotOptions = {
		//"colorScheme": [Color.fromName("red"), Color.fromName("blue")],
		"colorScheme": PlotKit.Base.colorScheme(),
		"shouldStroke": true,
		"shouldFill": false
	};
	
	var plotter = new PlotKit.SweetCanvasRenderer(canvas, layout, plotOptions);
	plotter.clear();
	plotter.render();
 
	legend = new LegendRenderer("myLegend", layout, plotOptions);
	legend.clear();
	legend.render();
}

function drawElapsedBuildTimeGraph() {
	fullSamples = {};
	incrementalSamples = {};
	
	var includeFull = document.getElementById("chkFullBuilds").checked;
	var includeIncremental = document.getElementById("chkIncrementalBuilds").checked;
	
	var minX = buildData[0].completionDate.time;
	var maxX = buildData[0].completionDate.time;
	var broken = 0;
	
	for (var i=0; i < buildData.length; i++) {
		if (buildData[i].updateType == "Full" && !includeFull) {
			continue;
		} else if (buildData[i].updateType != "Full" && !includeIncremental) {
			continue;
		}
		
		var x = buildData[i].completionDate.time;
		var elapsed = (buildData[i].completionDate.time - buildData[i].startDate.time) / 1000;

		if (elapsed <= 0) {
			broken++;
		} else if (buildData[i].updateType == "Full") {
			addSample(fullSamples, buildData[i].name, [ x, elapsed ]);
		} else {
			addSample(incrementalSamples, buildData[i].name, [ x, elapsed ]);
		}
		
		if (x < minX) {
			minX = x;
		} else if (x > maxX) {
			maxX = x;
		}
	}

	var delta = maxX - minX;
	var adjustment = delta * 10 / 100;
	
	maxX += adjustment;
	minX -= adjustment;

	var layoutOptions = {
		"xAxis": [minX, maxX],
		"xTicks": createLabels(buildData, 4),
		"yTicks": [{label:"15:00", v:900}, {label:"30:00", v:1800}, {label:"45:00", v:2700}, {label:"60:00", v:3600}],
		"xOriginIsZero": false
	};
		
	var layout = new PlotKit.Layout("line", layoutOptions);
	if (includeFull) {
		for (var pj in fullSamples) {
			layout.addDataset("Full Builds - " + pj, fullSamples[pj]);
		}
	}
	if (includeIncremental) {
		for (var pj in incrementalSamples) {
			layout.addDataset("Incremental Builds - " + pj, incrementalSamples[pj]);
		}
	}
	layout.evaluate();
	
	var canvas = MochiKit.DOM.getElement("myCanvas");
	
	var plotOptions = {
		//"colorScheme": [Color.fromName("red"), Color.fromName("blue")],
		"colorScheme": PlotKit.Base.colorScheme(),
		"shouldStroke": true,
		"shouldFill": false
	};
	
	var plotter = new PlotKit.SweetCanvasRenderer(canvas, layout, plotOptions);
	plotter.clear();
	plotter.render();
 
	legend = new LegendRenderer("myLegend", layout, plotOptions);
	legend.clear();
	legend.render();
	
 	return false;
}
function addSample(sampleMap, key, sample) {
	var samples = sampleMap[key];
	
	if (samples == null) {
		samples = [];
		sampleMap[key] = samples;
	}
	
	samples[samples.length] = sample;
}
function createLabels(samples, numTicks) {
	var first = samples[0];
	var last = samples[samples.length-1];
	
	var delta = last.completionDate.time - first.completionDate.time;
	var inc = delta/(numTicks-1);
	
	var ticks = [];
	
	for (var i=0; i<numTicks; i++) {
		ticks[ticks.length] = createLabel(first.completionDate.time + i*inc);
	}
	
	return ticks;
}

function createLabel(time) {
	var date = new Date(time);
	
	return {label: (date.getMonth()+1) + "/" + date.getDate() + "/" + date.getFullYear(), v:time};
}

function loadChartForm() {
	if (window.previousChartForm) {
		window.previousChartForm.style.display = "none";
		window.previousChartForm = null;
	}

	var chartType = getSelectedChartType();
	
	var chartFormId = chartType + "Form";
	
	var chartForm = document.getElementById(chartFormId);

	if (chartForm) {
		chartForm.style.display = "block";
		window.previousChartForm = chartForm;
	}
	
	invoke("init" + chartType, chartForm);
}

function invoke(name, form) {
	try {
		var initFunc = eval(name);
		
		if (initFunc instanceof Function) {
			initFunc.apply(form, [form]);
		}
	} catch(e) {
	}
}

function drawCurrentChart() {
	var canvas = document.createElement("canvas");
	
	canvas.width = 800;
	canvas.height = 600;
	
	MochiKit.DOM.replaceChildNodes(MochiKit.DOM.getElement("myCanvas").parentNode, canvas);
	
	canvas.id = "myCanvas"
	
	var chartType = getSelectedChartType();
	var chartFormId = chartType + "Form";
	
	var chartForm = document.getElementById(chartFormId);
	
	invoke("draw" + chartType, chartForm);
}

function getSelectedChartType() {
	var select = document.getElementById("chartSelector");
	return select.options[select.selectedIndex].value;
}

function registerChartHandlers() {
	var selector = document.getElementById("chartSelector");
	customAddEventListener(selector, "change", loadChartForm);
	
	var btnRedraw = document.getElementById("btnRedraw");
	customAddEventListener(btnRedraw, "click", drawCurrentChart);
	
	loadChartForm.apply(selector, null);
	
	customAddEventListener(document.getElementById("btnRedraw"), "click", preventDefaultHandler);
}

customAddEventListener(window, "load", registerChartHandlers);
