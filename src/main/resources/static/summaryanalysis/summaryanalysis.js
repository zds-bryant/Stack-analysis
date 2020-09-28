function summaryAnalysis() {
    var formData = new FormData();
    formData.append("processName", currentProcessName);
    postClient(formData, "/summary", summaryAnalysisCallbackSuccessFunction);
}

function summaryAnalysisCallbackSuccessFunction(data) {
    showSummaryAnalysis(data);
}

function showSummaryAnalysis(data) {
    var analysisAreaConent = resetAnalysisArea(data.titleData)
    analysisAreaConent.id = "summaryAnalysis";
    if (currentStateRatioFlag == 0) {
        showAllStateRatioSummaryAnalysis(data, analysisAreaConent);
    } else {
        showGroupStateRatioSummaryAnalysis(data, analysisAreaConent);
    }
    addToggleButtonSummaryAnalysis(data, analysisAreaConent);
}
//展示所有的线程
function showAllStateRatioSummaryAnalysis(data, analysisAreaConent) {
    var stateRatio = data.stateRatio;
    var legendData = stateRatio.legendData;
    var colorData = stateRatio.colorData;
    var seriesData = stateRatio.seriesData;
    var myChart = echarts.init(analysisAreaConent);
    myChart.clear();
    var option = {
        title: {
            text: '总体线程状态占比',
            left: 'center'
        },
        tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
            orient: 'vertical',
            right: 10,
            data: legendData
        },
        color: colorData,
        series: [{
            name: '访问来源',
            type: 'pie',
            radius: ['50%', '70%'],
            avoidLabelOverlap: false,
            label: {
                normal: {
                    show: false,
                    position: 'center'
                },
                emphasis: {
                    show: true,
                    textStyle: {
                        fontSize: '30',
                        fontWeight: 'bold'
                    }
                }
            },
            labelLine: {
                normal: {
                    show: false
                }
            },
            data: seriesData
        }]
    };
    myChart.setOption(option);
    myChart.on('click', function(param) {
        if (param.componentSubType == "pie") {
            //处理跳转控件
            //resetStateFilterBox(param.name);
            //弹出跳转菜单
            var jumpItem = "<div  class='jumpButton'>阻塞分析</div>" +
                "<div  class='jumpButton'>相同片段</div>" +
                "<div  class='jumpButton'>线程明细</div>";
            jumpButtonPanel(param.event.event.clientX + 10, param.event.event.clientY + 10, jumpItem, param.name, null);
            //refresh(currentAnalysisName, currentProcessName);
        }
    });
}

//分组展示
function showGroupStateRatioSummaryAnalysis(data, analysisAreaConent) {
    var jsonData = data.groupStateRatio.jsonData;
    var title = '分组线程状态占比';
    var myChart = echarts.init(analysisAreaConent);
    var option = {
        title: {
            text: title,
            left: 'center'
        },
        tooltip: {
            formatter: function(params, ticket, callback) {
                if (typeof(params.data.attributes) == 'undefined') {
                    return "";
                }
                var name = params.data.attributes.name;
                var group = params.data.attributes.group;
                var count = params.data.attributes.count;
                var proportion = params.data.attributes.proportion;
                return "名称：" + name + "<br/>" + "组名：" + group + "<br/>" + "总量：" + count + "<br/>" + "占比：" + proportion;
            }
        },
        series: {
            type: 'sunburst',
            nodeClick: false,
            highlightPolicy: 'ancestor',
            data: jsonData,
            radius: ['30%', '90%'],
            label: {
                rotate: 'radial'
            }
        }
    };
    myChart.setOption(option);
    myChart.on('click', function(param) {
        if (param.componentSubType == "sunburst") {
            if (param.event.target.parent.node.parentNode == null) {

            } else {
                var jumpItem = "<div  class='jumpButton'>阻塞分析</div>" +
                    "<div  class='jumpButton'>相同片段</div>" +
                    "<div  class='jumpButton'>线程明细</div>";
                //处理过滤控件
                var stateName = param.name;
                var groupName = param.event.target.parent.node.parentNode.name;
                if (groupName == "" || groupName == null) {
                    // resetGroupFilterBox(stateName);
                    // resetStateFilterBox(null);
                    jumpButtonPanel(param.event.event.clientX + 10, param.event.event.clientY + 10, jumpItem, null, stateName);
                } else {
                    // resetGroupFilterBox(groupName);
                    // resetStateFilterBox(stateName);
                    jumpButtonPanel(param.event.event.clientX + 10, param.event.event.clientY + 10, jumpItem, stateName, groupName);
                }
                //弹出跳转菜单
                //refresh(currentAnalysisName, currentProcessName);
            }
        }
    });
}

function addToggleButtonSummaryAnalysis(data, analysisAreaConent) {
    var toggleButton = document.createElement("div");
    toggleButton.id = "toggleButton";
    analysisAreaConent.appendChild(toggleButton);
    var widgetText = (currentStateRatioFlag == 0 ? "点击显示分组状态占比" : '点击显示总体状态占比');
    BI.createWidget({
        type: "bi.button",
        text: widgetText,
        css: {
            backgroundColor: "brown",
            border: "brown"
        },
        level: "common",
        height: 30,
        element: "#toggleButton",
        listeners: [{
            eventName: "EVENT_CHANGE",
            action: function() {
                var text = this.element[0].innerText;
                if ('点击显示分组状态占比' == this.element[0].innerText) {
                    this.element[0].innerText = '点击显示总体状态占比';
                    showGroupStateRatioSummaryAnalysis(data, analysisAreaConent);
                    currentStateRatioFlag = 1;
                } else {
                    this.element[0].innerText = '点击显示分组状态占比';
                    showAllStateRatioSummaryAnalysis(data, analysisAreaConent);
                    currentStateRatioFlag = 0;
                }
            }
        }]
    })
}