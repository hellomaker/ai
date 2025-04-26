package io.github.hellomaker.ai.agent.rag.spliter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AdvancedPdfParser {

    private final StringBuilder markdown = new StringBuilder();
    private boolean lastWasTable = false;
    private final FontAnalysis fontAnalysis = new FontAnalysis();
    private static final double TITLE_FONT_THRESHOLD = 14.0;

    public String parsePdf(File pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new EnhancedTextStripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);
            return markdown.toString();
        }
    }

    private class EnhancedTextStripper extends PDFTextStripper {
        
        private TextPosition lastTextPos;
        private final StringBuilder currentBlock = new StringBuilder();
        private float currentFontSize;
        private boolean currentIsBold;
        private boolean currentIsCenter;

        private boolean isNextP;

        public EnhancedTextStripper() throws IOException {
            super();
            super.setLineSeparator("\n"); // 确保换行处理
        }

        @Override
        protected void startPage(PDPage page) {
            // 每页开始时重置状态
            lastTextPos = null;
            currentBlock.setLength(0);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            for (TextPosition pos : textPositions) {
                // 跳过空白字符（但保留换行符）
                if (pos.getUnicode().trim().isEmpty() && !pos.getUnicode().equals("\n")) {
                    continue;
                }

                // 检测是否需要开始新段落
                if (shouldStartNewBlock(pos)) {
                    flushCurrentBlock();
                }

                // 更新当前文本属性
                updateTextAttributes(pos);
                currentBlock.append(pos.getUnicode());
                lastTextPos = pos;
            }
        }

        private boolean shouldStartNewBlock(TextPosition currentPos) {
            if (lastTextPos == null || currentBlock.length() == 0) {
                return false;
            }

            // 垂直位置变化超过行高的1.2倍视为新段落
            float lastY = lastTextPos.getTextMatrix().getTranslateY();
            float currentY = currentPos.getTextMatrix().getTranslateY();
            float lineHeight = lastTextPos.getHeight();

            // 属性变化检测
            boolean fontChanged = Math.abs(currentFontSize - currentPos.getFontSizeInPt()) > 0.5;
            boolean styleChanged = currentIsBold != isBoldFont(currentPos.getFont());

            return (Math.abs(lastY - currentY) > (lineHeight * 1.2f)) 
                   || fontChanged 
                   || styleChanged;
        }

        private void updateTextAttributes(TextPosition pos) {
            currentFontSize = pos.getFontSizeInPt();
            currentIsBold = isBoldFont(pos.getFont());
            
            // 简单居中检测（实际需要更精确的页面布局分析）
            PDPage page = getCurrentPage();
            float pageWidth = page.getMediaBox().getWidth();
            float textWidth = pos.getWidth();
            float xPos = pos.getTextMatrix().getTranslateX();
            currentIsCenter = Math.abs((pageWidth - textWidth)/2 - xPos) < 30;
        }

        private void flushCurrentBlock() {
            String text = currentBlock.toString().trim();
            if (text.isEmpty()) {
                currentBlock.setLength(0);
                return;
            }

            // 记录字体统计
            fontAnalysis.recordText(currentFontSize, text.length());

            // 处理段落间距
            if (!lastWasTable && markdown.length() > 0) {
                markdown.append("\n");
            }
            lastWasTable = false;

            // 标题检测逻辑
            if (currentIsBold || currentIsCenter || currentFontSize >= TITLE_FONT_THRESHOLD) {
                int headingLevel = calculateHeadingLevel();
                markdown.append("#".repeat(headingLevel))
                       .append(" ")
                       .append(text)
                       .append("\n\n");
            } 
            // 列表项检测（简单实现）
            else if (text.matches("^[•▪♦▶⦿\\-*].*")) {
                markdown.append("- ").append(text.substring(1).trim()).append("\n");
            }
            // 代码块检测（简单实现）
            else if (text.startsWith("  ") || text.startsWith("\t")) {
                markdown.append("    ").append(text.trim()).append("\n");
            }
            // 普通段落
            else {
                markdown.append(text).append("\n\n");
            }

            currentBlock.setLength(0);
        }

        private int calculateHeadingLevel() {
            if (currentFontSize >= 20.0) return 1;
            if (currentFontSize >= 18.0) return 2;
            if (currentFontSize >= TITLE_FONT_THRESHOLD) return 3;
            if (currentIsBold && currentFontSize >= 12.0) return 4;
            return 5;
        }

        @Override
        protected void endPage(PDPage page) throws IOException {
            flushCurrentBlock(); // 确保处理最后一块内容
            super.endPage(page);
        }
    }

    // 字体分析工具类
    private static class FontAnalysis {
        private final Map<Double, Integer> sizeDistribution = new HashMap<>();
        private double dominantSize = 12.0;

        public void recordText(double fontSize, int length) {
            int count = sizeDistribution.getOrDefault(fontSize, 0) + length;
            sizeDistribution.put(fontSize, count);
            
            // 更新主导字体大小
            if (count > sizeDistribution.getOrDefault(dominantSize, 0)) {
                dominantSize = fontSize;
            }
        }

        public double getDominantSize() {
            return dominantSize;
        }
    }

    private static boolean isBoldFont(PDFont font) {
        if (font == null) return false;
        String name = font.getName().toLowerCase();
        return name.contains("bold") ||
               name.contains("black") ||
               name.contains("heavy");
    }

    public static void main(String[] args) {
        try {
            AdvancedPdfParser parser = new AdvancedPdfParser();
            String result = parser.parsePdf(new File("C:\\Users\\Administrator\\Downloads/EORTC_MSGERC指南系列文章和国内血液病IFD诊治原则_cdf993a3-7656-4db1-8620-861c23677cb3.pdf"));
            System.out.println("=== 解析结果 ===");
            System.out.println(result);
        } catch (IOException e) {
            System.err.println("解析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

}