function threadListAnalysis() {
    startLoad();
    var formData = new FormData();
    formData.append("processName", currentProcessName);
    postClient(formData, "/threadList", showThreadListAnalysis);
}
//展示阻塞分析
function showThreadListAnalysis(data) {
    //重置分析区域
    var analysisAreaContent = resetAnalysisArea(data.titleData);
    analysisAreaContent.id = "threadlist";

    const request = indexedDB.open(sessionId);
    request.addEventListener('success', e => {
        const db = e.target.result;
        const tx = db.transaction(currentProcessName, 'readwrite');
        const store = tx.objectStore(currentProcessName);
        const reqGet = store.get(currentProcessName);
        reqGet.addEventListener('success', e => {
            var stackData = JSON.parse(e.target.result.threadStack);
            for (var i = 0; i < data.threadName.length; i++) {
                addThreadItemThreadListAnalysis(analysisAreaContent, stackData[data.threadName[i]]);
            }
            var spanList = $(".fontSpan");
            keyWordLocation(spanList);
            endLoad();
            addEventthreadListAnalysis();
            lineNumber = 1;
        })
    });
}

function keyWordLocation(spanList) {
    var preIndex = null;
    var currentIndex = null;
    if (checkfindKeyWordWork(spanList)) {
        $('.findKeyWordButton').css("visibility", "visible");
        $(".downFindKeyWord").click(function() {
            if (currentIndex == -1)
                currentIndex = 0;
            currentIndex = (currentIndex == null ? 0 : (currentIndex == spanList.length ? spanList.length : currentIndex + 1));
            if (currentIndex == spanList.length)
                return;
            preIndex = renderBackgroundColor(spanList, currentIndex, preIndex);
            locationKeyWord(currentIndex);
        });
        $(".upFindKeyWord").click(function() {
            if (currentIndex == spanList.length)
                currentIndex = spanList.length - 1;
            currentIndex = (currentIndex == null ? 0 : (currentIndex == -1 ? -1 : currentIndex - 1));
            if (currentIndex == -1)
                return;
            preIndex = renderBackgroundColor(spanList, currentIndex, preIndex);
            locationKeyWord(currentIndex);
        });
        document.onkeydown = function(event) {
            var e = event || window.event || arguments.callee.caller.arguments[0];
            if (e && e.keyCode == 38 || e && e.keyCode == 37) {
                $(".upFindKeyWord").click(); //上,左
            }

            if (e && e.keyCode == 40 || e && e.keyCode == 39) { //下,右
                $(".downFindKeyWord").click();
            }
        };
    }
    //给关键字添加背景
    function renderBackgroundColor(spanList, currentIndex, preIndex) {
        spanList[currentIndex].className = "keyWordBackgroundColor";
        if (preIndex != null)
            spanList[preIndex].className = "";
        return currentIndex;
    }

    //定位到关键字
    function locationKeyWord(currentIndex) {
        $(spanList[currentIndex]).parent().click();
        var offsetTop = spanList[currentIndex].offsetTop;
        var threadListheight = document.getElementById("threadlist").clientHeight;
        document.getElementById("threadlist").scrollTop = (offsetTop - threadListheight * 0.5);
    }
}

function addThreadItemThreadListAnalysis(analysisAreaContent, text) {
    var threadItemDiv = document.createElement("div");
    threadItemDiv.className = "threadItem";
    analysisAreaContent.appendChild(threadItemDiv);
    //设置文本序号
    var lineNumberDiv = document.createElement("div");
    lineNumberDiv.className = "lineNumber";
    lineNumberDiv.innerHTML = getLineNumberText(text);
    threadItemDiv.appendChild(lineNumberDiv);
    //设置文本内容
    var contentDiv = document.createElement("div");
    //根据输入框中的信息对文本进行高亮处理
    var heightKeyWordText = setHeightKeyWordFromInputBox(text);
    contentDiv.innerHTML = heightKeyWordText;
    contentDiv.className = "content";
    threadItemDiv.appendChild(contentDiv);


}
//当点击一个线程的堆栈时，设置其背景
function addEventthreadListAnalysis() {
    $(".threadItem").click(function() {
        var threadItemList = $(".threadItem");
        for (var i = 0; i < threadItemList.length; i++) {
            threadItemList[i].style.backgroundColor = "rgba(255, 255, 255, 1)";
        }
        $(this).css("background-color", "rgba(0, 0, 0, 0.1)");
        // $(this).children(".content").css("background-color", "rgba(0, 0, 0, 0.1)");
    });
}
//上下查找关键字生效条件
function checkfindKeyWordWork(spanList) {
    if (spanList == null) {
        return false;
    }
    if (currentAnalysisName == "threadListItem" && spanList.length > 0) {
        return true;
    }
    return false;
}

function getLineNumberText(text) {
    var lines = text.split("\n");
    var lineNumberText = ""

    for (var i = 0; i < lines.length - 1; i++) {
        lineNumberText = lineNumberText + lineNumber++ + "\n";
    }
    return lineNumberText
}