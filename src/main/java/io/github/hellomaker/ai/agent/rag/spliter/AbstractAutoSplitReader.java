package io.github.hellomaker.ai.agent.rag.spliter;


import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.util.*;

public abstract class AbstractAutoSplitReader implements DocumentReader {

    public AbstractAutoSplitReader(Resource resourceRef) {
        this.resourceRef = resourceRef;
    }

    public AbstractAutoSplitReader(Resource resourceRef, int maxTruckSize, int minTruckSize) {
        this.resourceRef = resourceRef;
        this.maxTruckSize = maxTruckSize;
        this.minTruckSize = minTruckSize;
    }

    int maxTruckSize = 1700;
    int minTruckSize = 300;

    @Override
    public List<Document> get() {
        return read();
    }

    @Override
    public List<Document> read() {
        List<Document> convert;
        try {
            PPointBuilder pPointBuilder = new PPointBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            convert(resourceRef, stringBuilder, pPointBuilder);
            convert = handleP(stringBuilder, pPointBuilder.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Document document : convert) {
            document.getMetadata().put("source", resourceRef.getFilename());
        }
        return convert;
    }

    private Resource resourceRef;

    TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(minTruckSize, maxTruckSize,5, 10000, true);

    private double defaultFontSize = 10.5;
//    private int minPSize = 350;
//    private int crunkSize = 800;
//    private int maxPSize = 2000;

    public class PPoint {
        boolean isBold;
        boolean isCenter;

        Integer titleLevel;
        Double fontSize;
        int startIndex;
        int endIndex;

        public PPoint(boolean isBold, boolean isCenter, Integer titleLevel, Double fontSize, int startIndex, int endIndex) {
            this.isBold = isBold;
            this.isCenter = isCenter;
            this.titleLevel = titleLevel;
            this.fontSize = fontSize;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public Double getFontSize() {
            if (fontSize != null) {
                return fontSize;
            }
            if (titleLevel != null) {
                return switch (titleLevel) {
                    case 0 -> 22D;
                    case 1, 2 -> 16D;
                    case 3, 4 -> 14D;
                    case 5, 6, 7 -> 12D;
                    default -> defaultFontSize;
                };
            }
            return defaultFontSize;
        }
    }


    Map<Double, Long> sizeCountMap = new HashMap<>();
    private double mostMuchSize = defaultFontSize;
    private long mostMuchCount = 0;
    private void putSize(double size, int count) {
        if (!sizeCountMap.containsKey(size)) {
            sizeCountMap.put(size, (long) count);
        } else {
            sizeCountMap.put(size, sizeCountMap.get(size) + count);
        }
        if (sizeCountMap.get(size) > mostMuchCount) {
            mostMuchSize = size;
            mostMuchCount = sizeCountMap.get(size);
        }
    }

    public abstract void convert(Resource resourceRef, StringBuilder markdown, PPointBuilder pPointBuilder) throws IOException;

//    private void handleP(StringBuilder markdown, List<Document> documents) {
//        String string = markdown.toString();
//        if (!string.isEmpty()) {
//            documents.add(new Document(string));
//        }
//        markdown.setLength(0);
//    }

    public class PPointBuilder {

        Stack<PPoint> stack = new Stack<>();
        public void addPoint(boolean isBold, boolean isCenter, Integer titleLevel, Double fontSize, int startIndex, int endIndex) {
            stack.add(new PPoint(isBold, isCenter, titleLevel, fontSize, startIndex, endIndex));
        }

        public void putSizeInner(double size, int count) {
            putSize(size, count);
        }

        public Stack<PPoint> get() {
            return stack;
        }

        public double getMostMuchSize() {
            return mostMuchSize;
        }
    }

    private List<Document> handleP(StringBuilder markdown, Stack<PPoint> pStack) {
//        if (markdown.length() > maxPSize) {
//            smartSplit(markdown, documents);
//        } else if (pStack.isEmpty() && markdown.length() > 0) {
//            // 没有分段点但内容可提交
//            documents.add(new Document(markdown.toString()));
//            markdown.setLength(0);
//        }
        List<Document> documents = new ArrayList<>();
        if (pStack.getFirst().startIndex != 0) {
            pStack.addFirst(new PPoint(false, false, null, 0D, 0, 0));
        }
        int i = 0;
        double[] scores = new double[pStack.size()];
        for (PPoint pPoint : pStack) {
            double point = calculateWeight(pPoint);
            scores[i] = point;
            System.out.println("分段" + i + ": " + markdown.substring(pPoint.startIndex, pPoint.endIndex) + " , 分数：" + point);
            i++;
        }

        List<Segment> segments = segmentArraysOwn(scores, markdown.length(), pStack);
        for (Segment segment : segments) {
            int size = segment.end - segment.start;
            Document e = new Document(markdown.substring(pStack.get(segment.start).startIndex, segment.end >= pStack.size() ? markdown.length() : pStack.get(segment.end).startIndex));
            if (size > maxTruckSize) {
                documents.addAll(tokenTextSplitter.split(e));
            } else {
                documents.add(e);
            }
        }

        return documents;
    }

    static class Segment {
        int start;
        int end;
        int maxIndex;
        double maxValue;

        public Segment(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public void calculateMax(double[] a) {
            this.maxIndex = start;
            this.maxValue = a[start];

            for (int i = start + 1; i <= end; i++) {
                if (a[i] > this.maxValue) {
                    this.maxValue = a[i];
                    this.maxIndex = i;
                }
            }
        }
    }

    public List<Segment> segmentArraysOwn(double[] a, int closeValue, Stack<PPoint> pStack) {

        int n = a.length;
        if (n != pStack.size()) {
            throw new IllegalArgumentException("Arrays must be same length");
        }

        List<Integer> maxQueue = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            maxQueue.add(i);
        }

        maxQueue.sort((o1, o2) -> {
            // 首先比较数组a中的值，降序排列
            int valueCompare = Double.compare(a[o2], a[o1]);
            if (valueCompare == 0) {
                // 如果数值相同，比较索引，降序排列
                return o2.compareTo(o1);
            }
            return valueCompare;
        });

        List<Segment> segments = new ArrayList<>();
        int split = split(pStack, segments, closeValue, 0, n - 1, maxQueue, 0);
        segments.sort(Comparator.comparingInt(o -> o.start));
        if (split >= 0) {
            System.out.println("存在小于300的分段，手动合并");
            if (!segments.isEmpty()) {
                Segment first = segments.getFirst();
                if (first != null) {
                    first.start = 0;
                }
            } else {
                segments.add(new Segment(0, n));
            }
        }
        return segments;
    }

//    public int split(List<Segment> segments, int closeValue, int startIndex, int endIndex, List<Integer> maxQueue, int nowStep) {
////        if (startIndex == endIndex || startIndex == endIndex - 1) {
////            Segment e1 = new Segment(startIndex, endIndex + 1);
////            segments.add(e1);
////            return startIndex - 1;
////        }
//
//        Stack<int[]> stack = new Stack<>();
//        stack.add(new int[]{startIndex, endIndex, nowStep});
//        int res = endIndex;
//        while (!stack.isEmpty()) {
//            int[] pop = stack.peek();
//            startIndex = pop[0];
//            endIndex = pop[1];
//            nowStep = pop[2];
//            int closeIndex = endIndex;
//            if (endIndex < startIndex) {
//                res = endIndex;
//                stack.pop();
//                if (!stack.isEmpty()) {
//                    stack.peek()[1] = res;
//                }
//                break;
//            }
//            boolean nextStack = false;
//            while (nowStep < maxQueue.size() && closeIndex >= startIndex) {
//                Integer peek = maxQueue.get(nowStep);
//                if (peek > closeIndex || peek < startIndex) {
//                    nowStep++;
//                    continue;
//                }
//                int num = (closeIndex >= pStack.size() - 1 ? closeValue : pStack.get(closeIndex + 1).startIndex) - pStack.get(peek).startIndex;
//                if (num > 1700) {
////                    closeIndex = split(segments, closeValue, peek, closeIndex, maxQueue, nowStep + 1);
//                    stack.push(new int[]{peek, closeIndex, nowStep + 1});
//                    nextStack = true;
//                    nowStep++;
//                    break;
//                } else if (num < 300) {
//                } else {
//                    segments.add(new Segment(peek, closeIndex + 1));
//                    closeIndex = peek - 1;
//                }
//                nowStep++;
//            }
//            pop[1] = closeIndex;
//            pop[2] = nowStep;
//            if (!nextStack) {
//                if (closeIndex >= startIndex && (closeIndex >= pStack.size() - 1 ? closeValue : pStack.get(closeIndex + 1).startIndex) - pStack.get(startIndex).startIndex > 1700) {
//                    Segment seg = new Segment(startIndex, closeIndex + 1);
//                    segments.add(seg);
////            System.out.println("超过1700 的分段 ：" );
//                    System.out.printf("超过1700 的分段 ： Range: %d-%d \n", seg.start, seg.end);
//                    res = startIndex - 1;
//                } else {
//                    res = endIndex;
//                }
//
//                stack.pop();
//                if (!stack.isEmpty()) {
//                    stack.peek()[1] = res;
//                }
//            }
//        }
//
//        return res;
//    }


    public int split(Stack<PPoint> pStack, List<Segment> segments, int closeValue, int startIndex, int endIndex, List<Integer> maxQueue, int nowStep) {
//        if (startIndex == endIndex || startIndex == endIndex - 1) {
//            Segment e1 = new Segment(startIndex, endIndex + 1);
//            segments.add(e1);
//            return startIndex - 1;
//        }
        if (endIndex < startIndex) {
            return endIndex;
        }

        int closeIndex = endIndex;
        while (nowStep < maxQueue.size() && closeIndex >= startIndex) {
            Integer peek = maxQueue.get(nowStep);
            if (peek > closeIndex || peek < startIndex) {
                nowStep++;
                continue;
            }
            int num = (closeIndex >= pStack.size() - 1 ? closeValue : pStack.get(closeIndex + 1).startIndex) - pStack.get(peek).startIndex;
            if (num > maxTruckSize) {
                closeIndex = split(pStack, segments, closeValue, peek, closeIndex, maxQueue, nowStep + 1);
            } else if (num < minTruckSize) {
            } else {
                segments.add(new Segment(peek, closeIndex + 1));
                closeIndex = peek - 1;
            }
            nowStep++;
        }
        if (closeIndex >= startIndex && (closeIndex >= pStack.size() - 1 ? closeValue : pStack.get(closeIndex + 1).startIndex) - pStack.get(startIndex).startIndex > maxTruckSize) {
            Segment seg = new Segment(startIndex, closeIndex + 1);
            segments.add(seg);
//            System.out.println("超过1700 的分段 ：" );
            System.out.printf("超过1700 的分段 ： Range: %d-%d \n", seg.start, seg.end);
            return startIndex - 1;
        }
        return closeIndex;
    }

    // 权重计算系数配置
    private static final double FONT_WEIGHT = 0.6;   // 字体大小权重
    private static final double TITLE_WEIGHT = 0.25;  // 标题等级权重
    private static final double CENTER_WEIGHT = 0.1;  // 居中权重
    private static final double BOLD_WEIGHT = 0.05;   // 加粗权重

    // 计算段落权重值
    private double calculateWeight(PPoint point) {
        // 标准化字体大小 (基于历史统计的最大值)
        double normalizedFontSize = point.getFontSize() / mostMuchSize;

        // 标准化标题等级 (0级标题为1，默认段落为0)
        double normalizedTitle = point.titleLevel != null ?
                1.0 - (point.titleLevel / 7.0) : 0;

        return FONT_WEIGHT * normalizedFontSize +
                TITLE_WEIGHT * normalizedTitle +
                CENTER_WEIGHT * (point.isCenter ? 1 : 0) +
                BOLD_WEIGHT * (point.isBold ? 1 : 0);
    }

}
