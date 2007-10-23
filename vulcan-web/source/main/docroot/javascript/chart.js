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

function drawElapsedBuildTimeGraph() {
	fullSamples = {};
	incrementalSamples = {};
	
	var includeFull = document.getElementById("chkFullBuilds").checked;
	var includeIncremental = document.getElementById("chkIncrementalBuilds").checked;
	
	var minX = buildData[0].startDate.time;
	var maxX = buildData[0].startDate.time;
	var broken = 0;
	
	var maxElapsedTime = 0;
	
	for (var i=0; i < buildData.length; i++) {
		if (buildData[i].updateType == "Full" && !includeFull) {
			continue;
		} else if (buildData[i].updateType != "Full" && !includeIncremental) {
			continue;
		}
		
		var x = buildData[i].startDate.time;
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
		
		if (elapsed > maxElapsedTime) {
			maxElapsedTime = elapsed;
		}
	}

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
function addSample(sampleMap, key, sample)
{
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
	
	var delta = last.startDate.time - first.startDate.time;
	var inc = delta/(numTicks-1);
	
	var ticks = [];
	
	for (var i=0; i<numTicks; i++) {
		ticks[ticks.length] = createLabel(first.startDate.time + i*inc);
	}
	
	return ticks;
}

function createLabel(time) {
	var date = new Date(time);
	return {label: (date.getMonth()+1) + "/" + date.getDate() + "/" + date.getFullYear(), v:time};
}

function loadChartForm() {
	var canvas = document.createElement("canvas");
	
	canvas.width = 800;
	canvas.height = 600;
	
	MochiKit.DOM.replaceChildNodes(MochiKit.DOM.getElement("myCanvas").parentNode, canvas);
	
	canvas.id = "myCanvas"
	
}

function drawCurrentChart() {
	var select = document.getElementById("chartSelector");
	var chartType = select.options[select.selectedIndex].value;
	eval(chartType + "()");
}

function registerChartHandlers() {
	var selector = document.getElementById("chartSelector");
	customAddEventListener(selector, "change", loadChartForm);
	
	var btnRedraw = document.getElementById("btnRedraw");
	customAddEventListener(btnRedraw, "click", drawCurrentChart);
	
	loadChartForm.apply(selector, null);
}

customAddEventListener(window, "load", registerChartHandlers);
