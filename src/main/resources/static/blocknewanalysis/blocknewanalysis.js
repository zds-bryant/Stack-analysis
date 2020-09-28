function blockNewAnalysis() {
    startLoad();
    var formData = new FormData();
    formData.append("processName", currentProcessName);
    postClient(formData, "/blockNew", showblockNewAnalysis);
}
//展示阻塞分析
function showblockNewAnalysis(data) {
    if (data.histogramDistribution.histogramXAxisData.length == 0) {
        resetAnalysisArea("没有发现阻塞的线程");
        endLoad();
        return;
    }
    //重置分析区域
    var analysisAreaContent = resetAnalysisArea(data.titleData);

    analysisAreaContent.id = "blockNewAnalysis";
    //如果没有block控件，执行第一个分支，否则执行第二个分支
    if ("" == blockFilterWidget || blockFilterWidget == null) {
        //根据线程名的前缀，用柱状图展示线程的分布
        var blockNewDistributionArea = document.createElement("div");
        blockNewDistributionArea.id = "distribution";
        analysisAreaContent.appendChild(blockNewDistributionArea);
        showBlockNewDistributionUseHistogram(data.histogramDistribution.histogramXAxisData, data.histogramDistribution.histogramSeriesData, blockNewDistributionArea, data.treeSeriesData);
    } else {
        //用树状图展示线程的阻塞关系
        var threadDependenceShowArea = document.createElement("div");
        threadDependenceShowArea.id = "threadDependenceShowArea";
        analysisAreaContent.appendChild(threadDependenceShowArea);
        showThreadDependenceUseTree(data.treeSeriesData[0], threadDependenceShowArea, data.nullThreadStack);
    }
    endLoad();
}

//用柱状图展示线程阻塞的数量
function showBlockNewDistributionUseHistogram(xAxisData, seriesData, showArea, treeSeriesData) {
    var myChart = echarts.init(showArea);
    var option = {
        tooltip: {
            formatter: "阻塞数量：{c}"
        },
        xAxis: {
            type: 'category',
            data: xAxisData
        },
        yAxis: {
            type: 'value'
        },
        series: [{
            data: seriesData,
            type: 'bar',
            barWidth: '50%',
            color: 'rgba(256,128,0,0.6)',
            itemStyle: {
                normal: {
                    label: {
                        show: true,
                        position: 'top',
                        textStyle: {
                            color: 'black',
                            fontSize: 16
                        }
                    }
                }
            },
        }]
    };
    myChart.setOption(option);
    myChart.on("click", function(param) {
        //console.log(param.dataIndex);
        var text = treeSeriesData[param.dataIndex].name + " blocking " + treeSeriesData[param.dataIndex].allNodeName.length + " threads";

        var allThreadName = treeSeriesData[param.dataIndex].allNodeName;
        blockAllThreadFilter = window.btoa(JSON.stringify(allThreadName));
        resetBlockFilterBox(text);
        blockNewAnalysis();
    });
}
//展示依赖关系树
function showThreadDependenceUseTree(treeSeriesData, showArea, nullThreadStack) {
    var myChart = echarts.init(showArea);
    var option = {
        series: [{
            type: 'tree',
            expandAndCollapse: false,
            data: [treeSeriesData],

            itemStyle: {
                color: "darkgoldenrod",
                borderColor: "darkgoldenrod"
            },
            left: '10%',
            right: '20%',
            top: '5%',
            bottom: '5%',
            symbolSize: 10,
            label: {
                position: 'right',
                verticalAlign: 'middle',
                align: 'left',
                fontSize: 10,
            },
            animationDuration: 550,
            animationDurationUpdate: 750
        }]
    };
    myChart.setOption(option);
    //点击按钮时，弹出窗口显示该线程的堆栈信息
    myChart.on('click', function(param) {
        const request = indexedDB.open(sessionId);
        request.addEventListener('success', e => {
            const db = e.target.result;
            const tx = db.transaction(currentProcessName, 'readwrite');
            const store = tx.objectStore(currentProcessName);
            const reqGet = store.get(param.name);
            reqGet.addEventListener('success', e => {
                layui.use('layer', function() {
                    layer.open({
                        type: 1,
                        title: param.name,
                        shadeClose: true,
                        shade: 0.5,
                        area: ['70%', '70%'],
                        content: buildFormatData(e.target.result, nullThreadStack, param.value.lock)
                    })
                });
            })
        });
    });
    addNodeEvent(showArea, myChart, nullThreadStack, treeSeriesData);
}
//给树节点添加事件
function addNodeEvent(showArea, myChart, nullThreadStack, treeSeriesData) {
    //添加分析相似片段的按钮
    addSameStackButton(showArea, treeSeriesData);
    //添加一个div显示节点的信息
    var nodeInfoDiv = document.createElement("div");
    nodeInfoDiv.className = "nodeInfoDiv";
    showArea.getElementsByTagName("div")[0].appendChild(nodeInfoDiv);
    myChart.on('mousemove', function(handler, context) {
        showNodeInfo(nodeInfoDiv, handler, nullThreadStack);
    });
}
//添加分析相似片段的按钮
function addSameStackButton(showArea) {
    var div = document.createElement("div");
    div.id = "jumpMenu";
    var i = document.createElement("i");
    i.className = "fa fa-th-large";
    div.appendChild(i);
    showArea.getElementsByTagName("div")[0].appendChild(div);
    div.onclick = function(e) {
        var jumpItem = "<div  class='jumpButton'>状态占比</div>" +
            "<div  class='jumpButton'>相同片段</div>" +
            "<div  class='jumpButton'>线程明细</div>";

        jumpButtonPanel(e.clientX - 220, e.clientY + 20, jumpItem);
    }
}
//展示节点的简要信息
function showNodeInfo(nodeInfoDiv, nodeData, nullThreadStack) {
    nodeInfoDiv.innerHTML = "";
    //简要的信息
    var brieflyInfo = document.createElement("div");
    var name = nodeData.value.name;
    var waitLock = nodeData.value.waitLock;
    var dependenceName = nodeData.value.dependenceName;
    var lock = nodeData.value.lock;
    brieflyInfo.innerHTML = "线程名称：" + name + "<br/>" + "等待的锁：" + waitLock + "<br/>" + "依赖线程：" + dependenceName + "<br/>" + "持有的锁：" + lock;
    nodeInfoDiv.appendChild(brieflyInfo);
}

function buildFormatData(stackData, nullThreadStack, lock) {
    if (stackData == null) {
        return nullThreadStack[lock];
    }
    var lines = stackData.threadStack.split("\n");
    var formatData = "";
    for (var i = 0; i < lines.length; i++) {
        if ("" == lines[i]) {
            formatData = formatData + "<br/>";
            continue;
        }
        var pattern = /(- waiting to lock <)|(- locked <)|(- parking to wait for  <)|(Locked ownable synchronizers:)/g;
        var matcher = lines[i].match(pattern);
        var tempLine;
        if (matcher != null) {
            tempLine = lines[i].replace("\t", "    ").replace(" ", "&nbsp&nbsp") + "<br/>";
            tempLine = "<font color='red'>" + tempLine + "</font>";
        } else {
            tempLine = lines[i].replace("\t", "    ").replace(" ", "&nbsp&nbsp") + "<br/>";
        }
        formatData = formatData + tempLine;
    }
    return setHeightKeyWordFromInputBox(formatData);
}