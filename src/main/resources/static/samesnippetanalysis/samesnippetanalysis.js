function sameStackAnalysis() {
    startLoad();
    var formData = new FormData();
    formData.append("processName", currentProcessName);
    postClient(formData, "/sameStack", showSameStackAnalysis);
}

function showSameStackAnalysis(data) {
    //重置分析区域
    var analysisAreaContent = resetAnalysisArea(data.titleData);
    analysisAreaContent.id = "sameStackAnalysis";

    var similarStack = data.similarStack;
    var similarStackThreadData = data.similarStackThreadData;
    for (var i = 0; i < similarStack.length; i++) {
        var divSimilarStackPart = getDivSimilarStackPart(similarStack[i], similarStackThreadData[i], i);
        analysisAreaContent.appendChild(divSimilarStackPart);
    }
    endLoad();
    addSameStackAnalysisEvent(similarStack);
}

function addSameStackAnalysisEvent(similarStack) {
    $(".partBodyColumn1RowThreadName").click(function() {
        var threadName = $(this).html();
        if ("线程名称" == threadName)
            return;
        const request = indexedDB.open(sessionId);
        request.addEventListener('success', e => {
            const db = e.target.result;
            const tx = db.transaction(currentProcessName, 'readwrite');
            const store = tx.objectStore(currentProcessName);
            const reqGet = store.get(threadName);
            reqGet.addEventListener('success', e => {
                var originalStack = getOriginalStack(e.target.result.threadStack);
                var sameStackId = parseInt($(this).attr("sameStackId"));
                var formData = new FormData();
                formData.append("aStack", similarStack[sameStackId]);
                formData.append("bStack", originalStack);
                formData.append("threadName", threadName);
                postClient(formData, "/sameStack/compare", compareStackCallbackSuccessFunction);
            })
        });
    })
}

function getOriginalStack(stackData) {
    var lines = stackData.split("\n");
    var originalStack = "";
    for (var i = 1; i < lines.length; i++) {
        originalStack = originalStack + lines[i] + "<br/>";
    }
    return originalStack;
}

function leftScroll() {
    document.getElementById("rightPanel").scrollLeft = document.getElementById("leftPanel").scrollLeft;
}

function rightScroll() {
    document.getElementById("leftPanel").scrollLeft = document.getElementById("rightPanel").scrollLeft;
}

function compareStackCallbackSuccessFunction(data) {
    var jsonData = data;
    var aStack = jsonData.aResultStackList;
    var bStack = jsonData.bResultStackList;
    var w = window.screen.availWidth * 0.95;
    var h = window.screen.availHeight * 0.75;
    layui.use('layer', function() {
        layer.open({
            type: 1,
            title: jsonData.threadName,
            shadeClose: true,
            shade: 0.5,
            area: [w + "px", h + "px"],
            content: "<div id='comparePanel'>   <div id='leftPanel' onscroll='leftScroll()'>" + setHeightKeyWordFromInputBox(aStack) +
                "</div> <div id='rightPanel' onscroll='rightScroll()'>" + setHeightKeyWordFromInputBox(bStack) + "</div>  </div>"
        })
    });
}

function getDivSimilarStackPart(eachSimilarStack, eachSimilarStackThreadData, sameStackId) {
    var similarStackPart = getSimilarStackPart();
    var similarStackPartHead = getSimilarStackPartHead(eachSimilarStackThreadData.length);
    var similarStackPartBody = getSimilarStackPartBody();
    var similarStackPartBodyColumn0 = getSimilarStackPartBodyColumn0(eachSimilarStack);
    var similarStackPartBodyColumn1 = getSimilarStackPartBodyColumn1(eachSimilarStackThreadData, sameStackId);
    similarStackPartBody.appendChild(similarStackPartBodyColumn0);
    similarStackPartBody.appendChild(similarStackPartBodyColumn1);
    similarStackPart.appendChild(similarStackPartHead);
    similarStackPart.appendChild(similarStackPartBody);
    return similarStackPart;
}

function getSimilarStackPart() {
    var div = document.createElement("div");
    div.className = "part";
    return div;
}

function getSimilarStackPartHead(countThread) {
    var div = document.createElement("div");
    div.className = "partHead";
    div.innerHTML = "从" + countThread + "个线程中发现相似的堆栈";
    return div;
}

function getSimilarStackPartBody() {
    var div = document.createElement("div");
    div.className = "partBody";
    return div;
}

function getSimilarStackPartBodyColumn0(eachSimilarStack) {
    var div = document.createElement("div");
    div.className = "partBodyColumn0";
    div.innerHTML = setHeightKeyWordFromInputBox(eachSimilarStack);
    return div;
}

function getSimilarStackPartBodyColumn1(eachSimilarStackThreadData, sameStackId) {
    var div = document.createElement("div");
    div.className = "partBodyColumn1";

    var row = getSimilarStackPartBodyColumn1Row("线程名称", "相似度", "线程状态", -1);
    row.className = "partBodyColumn1Row partBodyColumn1RowHeader";
    div.appendChild(row);

    for (var i = 0; i < eachSimilarStackThreadData.length; i++) {
        var rowData = eachSimilarStackThreadData[i];
        var row = getSimilarStackPartBodyColumn1Row(rowData.threadName, rowData.cosDistance, rowData.state, sameStackId);
        div.appendChild(row);
    }
    return div;
}

function getSimilarStackPartBodyColumn1Row(threadName, cosDistance, state, sameStackId) {
    var row = document.createElement("div");
    row.className = "partBodyColumn1Row";

    var threadNameDiv = document.createElement("div");
    threadNameDiv.setAttribute("sameStackId", sameStackId);
    threadNameDiv.innerHTML = threadName;
    threadNameDiv.className = "partBodyColumn1RowThreadName";
    row.appendChild(threadNameDiv);


    var cosDistanceDiv = document.createElement("div");
    cosDistanceDiv.innerHTML = cosDistance;
    cosDistanceDiv.className = "partBodyColumn1RowCosDistance";
    row.appendChild(cosDistanceDiv);

    var stateDiv = document.createElement("div");
    stateDiv.innerHTML = state;
    stateDiv.className = "partBodyColumn1RowState";
    row.appendChild(stateDiv);

    return row;
}