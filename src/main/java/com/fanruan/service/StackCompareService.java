package com.fanruan.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * @description: 栈文本比较引擎
 */
public class StackCompareService {

    List<String> aStack;
    List<String> bStack;
    List<BlockInf> aStackSameBlockList = new ArrayList<>();
    List<BlockInf> bStackSameBlockList = new ArrayList<>();

    List<String> aResultStack = new ArrayList<>();
    List<String> bResultStack = new ArrayList<>();

    public StackCompareService(List<String> aStack, List<String> bStack) {
        this.aStack = aStack;
        this.bStack = bStack;
        init();
        sortStackSameBlockList(aStackSameBlockList);
        sortStackSameBlockList(bStackSameBlockList);
        getResultStackBlock();
    }


    public String getAResultStack() {
        String temp = "";
        for (String s : aResultStack) {
            temp = temp + s;
        }
        return temp;
    }

    public String getBResultStack() {
        String temp = "";
        for (String s : bResultStack) {
            temp = temp + s;
        }
        return temp;
    }


    private void getResultStackBlock() {
        BlockInf aSameBlock = null;
        BlockInf bSameBlock = null;
        for (int i = 0; i < aStackSameBlockList.size(); i++) {
            aSameBlock = aStackSameBlockList.get(i);
            bSameBlock = bStackSameBlockList.get(i);
            List<String> aSameLengthBlock = new ArrayList<>();
            List<String> bSameLengthBlock = new ArrayList<>();
            buildSameLengthBlock(aSameLengthBlock, bSameLengthBlock, i);
            aResultStack.add(renderBackGround(aSameLengthBlock, 0));
            aResultStack.add(renderBackGround(aStack.subList(aSameBlock.getStartLine(), aSameBlock.getEndLine()), 1));
            bResultStack.add(renderBackGround(bSameLengthBlock, 0));
            bResultStack.add(renderBackGround(bStack.subList(bSameBlock.getStartLine(), bSameBlock.getEndLine()), 1));
        }
        aResultStack.add(renderBackGround(aStack.subList(aSameBlock.getEndLine(), aStack.size()), 0));
        bResultStack.add(renderBackGround(bStack.subList(bSameBlock.getEndLine(), bStack.size()), 0));
    }

    private String renderBackGround(List<String> lines, int flag) {
        String renderData = "";
        if (flag == 0) {
            renderData = "<div>";
            for (String line : lines) {
                renderData = renderData + line + "<br/>";
            }
            renderData = renderData + "</div>";
        } else {
            renderData = "<div class='sameSnippet'>";
            for (String line : lines) {
                renderData = renderData + line + "<br/>";
            }
            renderData = renderData + "</div>";
        }
        return renderData;
    }

    private void buildSameLengthBlock(List<String> aSameLengthBlock, List<String> bSameLengthBlock, int pos) {
        BlockInf aSameBlock = aStackSameBlockList.get(pos);
        BlockInf bSameBlock = bStackSameBlockList.get(pos);
        List<String> aBlockList = null;
        List<String> bBlockList = null;
        if (pos == 0) {
            aBlockList = aStack.subList(0, aSameBlock.getStartLine());
            bBlockList = bStack.subList(0, bSameBlock.getStartLine());
        } else {
            aBlockList = aStack.subList(aStackSameBlockList.get(pos - 1).getEndLine(), aSameBlock.getStartLine());
            bBlockList = bStack.subList(bStackSameBlockList.get(pos - 1).getEndLine(), bSameBlock.getStartLine());
        }
        aSameLengthBlock.addAll(aBlockList);
        bSameLengthBlock.addAll(bBlockList);
        if (aBlockList.size() > bBlockList.size()) {
            for (int i = 0; i < aBlockList.size() - bBlockList.size(); i++) {
                bSameLengthBlock.add("-");
            }
        } else {
            for (int i = 0; i < bBlockList.size() - aBlockList.size(); i++) {
                aSameLengthBlock.add("-");
            }
        }
    }

    private void sortStackSameBlockList(List<BlockInf> stackSameBlockList) {
        Collections.sort(stackSameBlockList, new Comparator<BlockInf>() {
            @Override
            public int compare(BlockInf o1, BlockInf o2) {
                if (o1.getStartLine() == o2.getStartLine())
                    return 0;
                return o1.getStartLine() > o2.getStartLine() ? 1 : -1;
            }
        });
    }

    private void init() {
        findStackSameBlock(0, aStack.size(), 0, bStack.size());
    }

    private void findStackSameBlock(int aStartPos, int aEndPos, int bStartPos, int bEndPos) {
        List<String> LCS = findLCS(aStack.subList(aStartPos, aEndPos), bStack.subList(bStartPos, bEndPos));
        if (LCS.size() == 0) {
            return;
        }
        BlockInf aBlockInf = getBlockInf(aStack, aStartPos, LCS);
        BlockInf bBlockInf = getBlockInf(bStack, bStartPos, LCS);
        aStackSameBlockList.add(aBlockInf);
        bStackSameBlockList.add(bBlockInf);
        findStackSameBlock(aStartPos, aBlockInf.getStartLine(), bStartPos, bBlockInf.getStartLine());
        findStackSameBlock(aBlockInf.getEndLine(), aEndPos, bBlockInf.getEndLine(), bEndPos);

    }

    private BlockInf getBlockInf(List<String> stack, int startPos, List<String> LCS) {
        for (int i = startPos; i < stack.size(); i++) {
            if (stack.get(i).equals(LCS.get(0))) {
                int j = 0;
                for (; j < LCS.size(); j++) {
                    if (!stack.get(i + j).equals(LCS.get(j))) {
                        break;
                    }
                }
                if (j == LCS.size()) {
                    return new BlockInf(i, i + j, LCS.size(), 0);
                }
            }
        }
        return null;
    }

    private List<String> findLCS(List<String> list1, List<String> list2) {
        int maxLineCount = 0;
        List<String> maxLineList = new ArrayList<>();
        for (int i = 0; i < list1.size(); i++) {
            for (int j = 0; j < list2.size(); j++) {
                if (list1.get(i).equals(list2.get(j))) {
                    int count = 0;
                    List<String> list = new ArrayList<>();
                    while (i + count < list1.size() && j + count < list2.size()) {
                        if (list1.get(i + count).equals(list2.get(j + count))) {
                            list.add(list1.get(i + count));
                            count = count + 1;
                        } else {
                            if (count > maxLineCount) {
                                maxLineCount = count;
                                maxLineList = list;
                            }
                            break;
                        }
                    }
                    if (i + count >= list1.size() || j + count >= list2.size()) {
                        if (count > maxLineCount) {
                            maxLineCount = count;
                            maxLineList = list;
                        }
                    }
                }
            }
        }
        return maxLineList;
    }

    class BlockInf {
        int startLine;
        int endLine;
        int countLine;
        int mask;

        BlockInf(int startLine, int endLine, int countLine, int mask) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.countLine = countLine;
            this.mask = mask;
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public int getCountLine() {
            return countLine;
        }

        public void setCountLine(int countLine) {
            this.countLine = countLine;
        }

        public int getMask() {
            return mask;
        }

        public void setMask(int mask) {
            this.mask = mask;
        }

        @Override
        public String toString() {
            return startLine + "_" + endLine + "_" + countLine;
        }
    }
}


