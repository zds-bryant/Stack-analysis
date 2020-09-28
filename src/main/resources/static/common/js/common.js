//===========================================================================================================
//==================================================常用函数============================================
//===========================================================================================================
//post请求函数
function postClient(formData, url, successFuntion) {
    refreshFilterOptions(formData);
    formData.append("sessionId", sessionId);
    $.ajax({
        type: "post",
        url: url,
        data: formData,
        processData: false,
        contentType: false,
        success: function(data) {
            var jsonData = JSON.parse(data);
            if (jsonData.hasOwnProperty("message") && jsonData.hasOwnProperty("stackTrace")) {
                errorCallBackFunction(jsonData.message, jsonData.stackTrace);
            } else {
                init();
                successFuntion(jsonData);
            }
        },
        error: function(data) {
            errorCallBackFunction("服务器异常", null)
        }
    });
}
//处理异常信息
function errorCallBackFunction(message, stackTrace) {
    endLoad();
    var temp = (stackTrace == null ? [] : ['查看详情']);
    layui.use('layer', function() {
        layer.confirm(message, {
            title: "error",
            shade: [0.1, '#000'],
            btn: temp
        }, function() {
            var h = document.getElementById("wholeBox").clientHeight;
            var w = document.getElementById("wholeBox").clientWidth;
            var htmlContent = "<div style='white-space: pre;'>" + stackTrace + "</div>";
            layer.open({
                type: 0,
                title: name,
                shadeClose: true,
                area: [w * 0.7 + "px", h * 0.7 + "px"],
                closeBtn: 0,
                content: htmlContent
            })
        });
    });
}
//展示分析界面前初始化
function init() {
    if (currentAnalysisName == "healthItem") {
        $('#filterBar').css("visibility", "hidden");
    } else {
        $('#filterBar').css("visibility", "visible");
    }
    $('.findKeyWordButton').css("visibility", "hidden");
    document.onkeydown = function() {};
}
//刷新过滤控件的值
function refreshFilterOptions(formData) {
    var state = "";
    var group = "";
    var pattern = "";
    var allThead = "";
    var block = "";
    if (stateFilterWidget != null) {
        state = stateFilterWidget.element[0].getElementsByTagName("div")[0].innerText;
    }
    if (groupFilterWidget != null) {
        group = groupFilterWidget.element[0].getElementsByTagName("div")[0].innerText;
    }
    if (patternFilterWidget != null) {
        pattern = patternFilterWidget.element[0].getElementsByTagName("div")[0].getElementsByTagName("div")[0].getElementsByTagName("input")[0].value;
    }

    if (blockFilterWidget != null) {
        allThead = blockAllThreadFilter;
        block = blockFilterWidget.element[0].getElementsByTagName("div")[0].innerText;
    }
    formData.append("threadStateFilter", state);
    formData.append("threadGroupNameFilter", group);
    formData.append("stackPatternFilter", pattern);
    formData.append("blockAllThreadFilter", allThead);
    formData.append("blockFilter", block);
}
//
function refresh(analysisName, processName) {
    if (processName == null) {
        alert("请上传文件");
        return;
    }
    refreshProcess(processName);
    refreshAnalysis(analysisName);
    if (analysisName == 'summaryItem') {
        summaryAnalysis();
    } else if (analysisName == "deadLockItem") {
        deadLockanalysis();
    } else if (analysisName == "blockItem") {
        blockAnalysis();
    } else if (analysisName == "groupItem") {
        groupAnalysis();
    } else if (analysisName == "healthItem") {
        healthAnalysis();
    } else if (analysisName == "parkWaitItem") {
        parkWaitAnalysis();
    } else if (analysisName == "sameSnippetItem") {
        sameStackAnalysis();
    } else if (analysisName == "blockNewItem") {
        blockNewAnalysis();
    } else if (analysisName == "threadListItem") {

        threadListAnalysis();
    }
}

function refreshProcess(processName) {
    currentProcessName = processName;
}

function refreshAnalysis(analysisName) {
    if (analysisName == currentAnalysisName) {
        return;
    } else {
        if (currentAnalysisName != null) {
            document.getElementById(currentAnalysisName).className = "";
        }
        document.getElementById(analysisName).className = "active";
        currentAnalysisName = analysisName;
    }
}
//重置分析区域
function resetAnalysisArea(title) {
    function resetAnalysisAreaHeight() {
        var rightBoxHeight = document.getElementById("rightBox").clientHeight;
        var filterBarHeight = document.getElementById("filterBar").clientHeight;
        document.getElementById("analysisArea").style.height = (rightBoxHeight - filterBarHeight) + "px";
    }
    var analysisArea = document.getElementById("analysisArea");
    if (analysisArea != null) {
        analysisArea.remove();
    }
    analysisArea = document.createElement("div");
    analysisArea.id = "analysisArea";
    var rightBox = document.getElementById("rightBox");
    rightBox.appendChild(analysisArea);
    resetAnalysisAreaHeight();
    if ("" != title && title != null) {
        //标题div
        var titleDiv = document.createElement("div");
        titleDiv.id = "title";
        titleDiv.innerHTML = title;
        analysisArea.appendChild(titleDiv);
    }
    //展示内容div
    var contentDiv = document.createElement("div");
    contentDiv.id = "content";
    analysisArea.appendChild(contentDiv);
    return contentDiv;
}


//===========================================================================================================
//==================================================和indexDB相关============================================
//===========================================================================================================
function createDB() {
    const request = indexedDB.open(sessionId, 1);
    request.addEventListener('success', e => {
        console.log('连接数据库成功');
    });
    request.addEventListener('error', e => {
        console.log('连接数据库失败');
    });
}

function createStore(storeName) {
    const request = indexedDB.open(sessionId, indexedDBVersion);
    request.addEventListener('upgradeneeded', e => {
        const db = e.target.result;
        const store = db.createObjectStore(storeName, {
            keyPath: 'threadName',
            autoIncrement: false
        });
        console.log('创建对象仓库成功');
    });
}

function addStackData(stackData) {
    const request = indexedDB.open(sessionId, indexedDBVersion);
    indexedDBVersion = indexedDBVersion + 1;
    request.addEventListener('success', e => {
        const db = e.target.result;
        const tx = db.transaction(currentProcessName, 'readwrite');
        const store = tx.objectStore(currentProcessName);
        //总体存
        store.add({
            "threadName": currentProcessName,
            "threadStack": JSON.stringify(stackData)
        });
        //分开存
        for (var threadName in stackData) {
            var threadStack = stackData[threadName];
            store.add({
                "threadName": threadName,
                "threadStack": threadStack
            });
        }
    });
}

function deleteDB() {
    const request = indexedDB.deleteDatabase(sessionId);
    request.addEventListener('success', e => {
        console.log('删除成功');
    });
}
//===========================================================================================================
//==================================================文本高亮相关============================================
//===========================================================================================================
//文字高亮
function setHeightKeyWord(text, keyword, color) {
    var replaceText = "<span class='fontSpan' style='color:" + color + ";'>$1</span>";
    var r = new RegExp("(" + keyword + ")", "ig");
    heightKeyWordText = text.replace(r, replaceText);
    return heightKeyWordText;
}
//通过关键字的位置来高亮文字
function setHeightKeyWordByKeyWordIndex(text, startIndex, endIndex) {
    var part0 = text.substring(0, startIndex);
    var part1 = text.substring(startIndex, endIndex);
    var part2 = text.substring(endIndex);
    var heightKeyWordText = part0 + "<font style='background-color: cadetblue;'>" + part1 + "</font>" + part2;
    return heightKeyWordText;
}
//根据全局过滤输入框的内容来高亮文本
function setHeightKeyWordFromInputBox(text, color) {
    color = (color == null ? "coral" : color);
    var filterInput = patternFilterWidget.element[0].getElementsByTagName("div")[0].getElementsByTagName("div")[0].getElementsByTagName("input")[0].value;
    if (filterInput != null && filterInput != "") {
        heightKeyWordText = setHeightKeyWord(text, filterInput, color);
        return heightKeyWordText;
    }
    return text;
}


//===========================================================================================================
//==================================================正在刷新页面相关函数======================================
//===========================================================================================================
function startLoad() {
    layui.use('layer', function() {
        loadIndex = layer.load(1, {
            time: 60 * 1000,
            shade: [0.1, '#000']
        });
    });
}

function endLoad() {

    layui.use('layer', function() {
        layer.close(loadIndex);
    });
}
//===========================================================================================================
//==================================================和跳转界面相关============================================
//===========================================================================================================
function jumpButtonPanel(movex, movey, jumpItem, state, gruop) {
    layui.use('layer', function() {
        layer.open({
            type: 1,
            title: name,
            shadeClose: true,
            shade: [0.1, '#000'],
            offset: [movey, movex],
            content: jumpItem
        })
    });
    setTimeout(function() {
        $(".jumpButton").mouseover(function() {
            $(this).css("cursor", "Pointer");
            $(this).css("color", "indianred");
        });
        $(".jumpButton").mouseout(function() {
            $(this).css("cursor", "default");
            $(this).css("color", "dimgray");
        });
        $(".jumpButton").click(function() {
            resetGroupFilterBox(gruop);
            resetStateFilterBox(state);

            if ($(this).text() == '阻塞分析') {
                $("#blockNewItem").click();
            } else if ($(this).text() == '相同片段') {
                $("#sameSnippetItem").click();
            } else if ($(this).text() == '线程明细') {
                $("#threadListItem").click();
            } else if ($(this).text() == '状态占比') {
                $("#summaryItem").click();
            }
            $(".layui-layer-shade").click();
        });
    }, 200);
}

//===========================================================================================================
//==================================================和过滤栏相关============================================
//===========================================================================================================
function createFilterBar() {
    var filterBar = document.createElement("div");
    filterBar.id = "filterBar";
    document.getElementById("rightBox").appendChild(filterBar);

    //这两个都是全局的变量
    patternFilterWidget = getFilterInput();
    filterBarWidget = getFilterBar(patternFilterWidget);
    addUpDownFindKeyWordButton();

    function getFilterBar(input) {
        return BI.createWidget({
            type: "bi.left",
            element: "#filterBar",
            items: [
                input
            ]
        });
    }

    function getFilterInput() {
        var searchFunc = debounce(refreshByInputText, 500);
        return BI.createWidget({
            type: "bi.vertical",
            items: [{
                type: "bi.clear_editor",
                cls: "bi-border filterInput",
                width: 300,
                watermark: "输入检索关键字",
                listeners: [{
                    eventName: "EVENT_CHANGE",
                    action: function() {
                        searchFunc()
                    }
                }, {
                    eventName: "EVENT_EMPTY",
                    action: function() {
                        refreshByInputText();
                    }
                }]
            }],
        })
    }

    function addUpDownFindKeyWordButton() {
        return BI.createWidget({
            type: "bi.right",
            element: "#filterBar",
            items: [{
                type: "bi.vertical_adapt",
                height: 26,
                items: [{
                    el: {
                        type: "bi.icon_button",
                        cls: "column-next-page-h-font findKeyWordButton downFindKeyWord",
                    }
                }]
            }, {
                type: "bi.vertical_adapt",
                height: 26,
                items: [{
                    el: {
                        type: "bi.icon_button",
                        cls: "column-pre-page-h-font findKeyWordButton upFindKeyWord",
                    }
                }]
            }]
        });
    }
}
//刷新状态过滤控件
function resetStateFilterBox(state) {
    if (state == null)
        return;
    //删除原有的控件
    if (stateFilterWidget != null) {
        stateFilterWidget.destroy();
    }
    //新建控件
    var filterLabel = getFilterLabel(state, "state");
    stateFilterWidget = filterLabel;
    filterBarWidget.addItem(filterLabel);
}
//刷新分组过滤控件
function resetGroupFilterBox(group) {
    if (group == null)
        return;
    //删除原有的控件
    if (groupFilterWidget != null) {
        groupFilterWidget.destroy();
    }
    //新建控件
    var filterLabel = getFilterLabel(group, "group");
    groupFilterWidget = filterLabel;
    filterBarWidget.addItem(filterLabel);
}
//刷新阻塞过滤控件
function resetBlockFilterBox(block) {
    //删除原有的控件
    if (blockFilterWidget != null) {
        blockFilterWidget.destroy();
    }
    //新建控件
    var filterLabel = getFilterLabel(block, "block");
    blockFilterWidget = filterLabel;
    filterBarWidget.addItem(filterLabel);
}
//
function refreshByInputText() {
    var newPattern = patternFilterWidget.element[0].getElementsByTagName("div")[0].getElementsByTagName("div")[0].getElementsByTagName("input")[0].value;
    if (newPattern != oldPattern) {
        refresh(currentAnalysisName, currentProcessName);
    }
    oldPattern = newPattern;
}

function getFilterLabel(text, stateOrgruopOrBlock) {
    return BI.createWidget({
        type: "bi.left",
        cls: "filterLabel",
        items: [{
            el: {
                type: "bi.label",
                text: text,
                height: 26,
                css: {
                    padding: "0 5px"
                }
            }
        }, {
            type: "bi.vertical_adapt",
            height: 26,
            items: [{
                el: {
                    type: "bi.icon_button",
                    cls: "close-ha-font",
                    css: {
                        width: 24
                    },
                    listeners: [{
                        eventName: "EVENT_CHANGE",
                        action: function() {
                            this._parent._parent._parent.destroy();
                            if (stateOrgruopOrBlock == "state") {
                                stateFilterWidget = null;
                            } else if (stateOrgruopOrBlock == "group") {
                                groupFilterWidget = null;
                            } else if (stateOrgruopOrBlock == "block") {
                                blockFilterWidget = null;
                            }
                            refresh(currentAnalysisName, currentProcessName);
                        }
                    }]
                }
            }]
        }]
    });
}

//在过滤栏的最右边添加查找关键字按钮
function addUpAndDownKeyWordButton(filterBar) {
    var downI = document.createElement("i");
    downI.className = "fa fa-angle-down findKeyWordButtonDefualt findKeyWordButton";
    downI.id = "downFindKeyWord";
    downI.style.right = "20px";

    var upI = document.createElement("i");
    upI.className = "fa fa-angle-up findKeyWordButtonDefualt findKeyWordButton";
    upI.id = "upFindKeyWord";
    upI.style.right = "60px";

    filterBar.appendChild(downI);
    filterBar.appendChild(upI);
}
//===========================================================================================================
//==================================================和文件上传相关============================================
//===========================================================================================================
//文件上传成功的回调函数
function upLoadFileCallbackFunction(data) {
    $('#refreshUploadItem').css("visibility", "visible");
    //删除上传按钮
    $("#upLoadFileButton").remove();
    //添加过滤栏
    createFilterBar();
    var jsonObject = data.fileList;
    var endFileName = jsonObject.endfilename;
    createStore(endFileName);
    var stackData = data.processEntityJsonObject;
    addStackData(stackData);
    refresh("summaryItem", endFileName);
    //在左上角显示文件的名称
    document.getElementById("showFileName").innerHTML = endFileName;
}
//点击上传文件
function upLoadFile() {
    var inputFile = document.createElement("input");
    inputFile.type = "file";
    inputFile.onchange = function(inputFile) {
        var file;
        if (inputFile.path == null) {
            file = inputFile.target.files[0];
        } else {
            file = inputFile.path[0].files[0];
        }
        var formData = new FormData();
        formData.append("file", file);
        postClient(formData, "/uploadFile", upLoadFileCallbackFunction);
    };
    inputFile.click();
}
//===========================================================================================================
//==================================================其他=====================================================
//===========================================================================================================
//执行准备工作
function runPreparation() {
    //文件没有提交时隐藏左下角的刷新按钮
    $('#refreshUploadItem').css("visibility", "hidden");
    //页面关闭后，发送请求给后端
    window.onbeforeunload = function() {
        deleteDB();
        var formData = new FormData();
        formData.append("sessionId", sessionId);
        $.ajax({
            type: "post",
            url: "/leave",
            data: formData,
            processData: false,
            contentType: false
        });
    };
    //设置提交按钮margin大小
    var h = $("#rightBox")[0].clientHeight - $(".fa-upload")[0].clientHeight;
    var w = $("#rightBox")[0].clientWidth - $(".fa-upload")[0].clientWidth;
    $(".fa-upload").css("margin", Math.max(h / 2, 0) * 0.7 + "px " + Math.max(w / 2, 0) * 0.7 + "px");
    //拖拽到上传区域时，显示阴影
    document.getElementById("upLoadFileButton").ondragenter = function() {
        document.getElementById("upLoadFileButton").className = "upLoadFileButtonDragOver";
    }
    document.getElementById("upLoadFileButton").ondragleave = function() {
        document.getElementById("upLoadFileButton").className = " ";
    }
}
//当点击重新提交按钮时，重新刷新页面
function refreshUploadVeiw() {
    location.reload();
}
//去抖动函数
function debounce(fn, delay) {
    var timer
    return function() {
        var context = this
        var args = arguments
        clearTimeout(timer)
        timer = setTimeout(function() {
            fn.apply(context, args)
        }, delay)
    }
}
