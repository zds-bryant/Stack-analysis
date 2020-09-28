function healthAnalysis() {
    var formData = new FormData();
    formData.append("processName", currentProcessName);
    postClient(formData, "/health", healthAnalysisCallbackSuccessFunction);
}

function healthAnalysisCallbackSuccessFunction(data) {
    showhealthAnalysis(data);
}

function showhealthAnalysis(data) {
    var healthReport = data.healthReport;
    var titleData = data.titleData;
    if (healthReport.length == 0) {
        titleData = "此堆栈没有发现风险"
    }
    var analysisAreaConent = resetAnalysisArea(titleData);
    analysisAreaConent.id = "healthAnalysis";
    var healthReportPanel = addHealthPanel(analysisAreaConent);
    for (var i = 0; i < healthReport.length; i++) {
        addReportItem(healthReport[i], healthReportPanel, i)
    }
}

function addHealthPanel(analysisAreaConent) {
    var div = document.createElement("div");
    // div.addEventListener("mousedown",operateScroll.bind(this, div))
    div.id = "reportPanel";
    analysisAreaConent.appendChild(div);
    return div;
}

// function operateScroll(div){
//     div.addEventListener("mousemove", printLog)
//     div.addEventListener("mouseup",clearMouseEvent.bind(this, div))
// }
//
// function clearMouseEvent(div){
//     div.removeEventListener("mousedown", operateScroll.bind(this, div));
//     div.removeEventListener("mousemove", printLog.bind(this,div))
// }
//
// function printLog(div){
//     var scrollLeft = Element.documentElement.scrollLeft
//     console.log(scrollLeft)
//     console.log(event.clientX)
//     console.log(document.body.scrollWidth)
//     console.log("12346");
// }

function addReportItem(reportData, healthReportPanel, serialNumber) {
    var reportItem = document.createElement("div");
    reportItem.className = "item";
    var alpha = ((serialNumber + 1) % 2) * 0.5 + 0.1;
    reportItem.style.backgroundColor = "rgba(188,143,143," + alpha + ")";
    reportItem.innerHTML = (serialNumber + 1) + "、" + reportData["tip"];
    healthReportPanel.appendChild(reportItem);

    if (reportData.hasOwnProperty("stackData")) {
        var reportStack = document.createElement("div");
        reportStack.className = "stack";
        reportStack.innerHTML = reportData["stackData"];
        healthReportPanel.appendChild(reportStack);
    }

}
