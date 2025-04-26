package io.github.hellomaker.ai.agent.rag.spliter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.util.List;

public class PdfAutoReader extends AbstractAutoSplitReader{

    public PdfAutoReader(Resource resourceRef) {
        super(resourceRef);
    }

    // 用于检测标题的字体大小阈值
    private static final double TITLE_FONT_SIZE_THRESHOLD = 14.0;
    private static final double SUBTITLE_FONT_SIZE_THRESHOLD = 12.5;

    @Override
    public void convert(Resource resourceRef, StringBuilder markdown, PPointBuilder pPointBuilder) throws IOException {
        pPointBuilder.addPoint(false, false, 0, 0d, 0, 0);

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(resourceRef.getInputStream()))) {

            PDFTextStripper stripper = new PDFTextStripper() {
                private TextPosition lastPosition;
                private final StringBuilder currentParagraph = new StringBuilder();
                private Float lastFontSize = null;
                private boolean currentIsBold = false;
                private boolean currentIsCenter = false;
                private boolean isNextParagraph = false;

                @Override
                protected void startPage(PDPage page) throws IOException {
                    super.startPage(page);
                    // 重置状态
                    lastPosition = null;
                    currentParagraph.setLength(0);
                }

                @Override
                protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                    if (text.isEmpty()) {
                        return;
                    }

                    isNextParagraph = false;
                    currentIsBold = true;
                    Float currentFontSize = null;
                    currentIsCenter = false;
                    boolean isMoreLine = false;

                    if (!textPositions.isEmpty()) {
                        isMoreLine = isMoreLine(textPositions.getFirst());
                        if (lastFontSize != null && textPositions.getFirst().getFontSizeInPt() == lastFontSize) {
                            lastFontSize = textPositions.getLast().getFontSizeInPt();
                            currentParagraph.append(text);
                            processCurrentParagraph();
                            return;
                        }
                    }

                    boolean isSameSize = true;
                    for (TextPosition position : textPositions) {
                        // 更新当前文本属性
//                        updateTextAttributes(position);

                        currentParagraph.append(position.getUnicode());
                        lastPosition = position;

                        if (!isBoldFont(position.getFont())) {
                            currentIsBold = false;
                        }
                        float fontSizeInPt = position.getFontSizeInPt();
                        lastFontSize = fontSizeInPt;
                        if (isSameSize) {
                            if (currentFontSize == null) {
                                currentFontSize = fontSizeInPt;
                            } else if (currentFontSize != fontSizeInPt) {
                                currentFontSize = null;
                                isSameSize = false;
                            }
                        }
                    }

                    if (textPositions.size() == 1) {
                        TextPosition position = textPositions.getFirst();
                        // 居中检测（简化版，实际需要更复杂的布局分析）
                        PDRectangle pageSize = getCurrentPage().getMediaBox();
                        float textWidth = position.getWidth();
                        float xPosition = position.getTextMatrix().getTranslateX();
                        currentIsCenter = Math.abs((pageSize.getWidth() - textWidth)/2 - xPosition) < 20;
                    }

                    if (
//                            isMoreLine ||
                            currentIsBold || currentIsCenter || (currentFontSize != null && currentFontSize > pPointBuilder.getMostMuchSize())) {
                        isNextParagraph = true;
                    }

                    for (TextPosition position : textPositions) {
                        pPointBuilder.putSizeInner(position.getFontSizeInPt(), position.getUnicode().length());
                    }

                    processCurrentParagraph();
                }

                private boolean isMoreLine(TextPosition current) {
                    if (lastPosition == null) return false;

                    float lastY = lastPosition.getTextMatrix().getTranslateY();
                    float currentY = current.getTextMatrix().getTranslateY();
                    float lineHeight = lastPosition.getHeight();

                    // Y坐标变化超过行高1.5倍，或字体属性变化
                    return Math.abs(lastY - currentY) > (lineHeight * 1.5f) ||
                            (lastFontSize != null && Math.abs(lastFontSize - current.getFontSizeInPt()) > 0.5);
                }

                private boolean shouldStartNewParagraph(TextPosition current) {
                    if (lastPosition == null) return false;

                    float lastY = lastPosition.getTextMatrix().getTranslateY();
                    float currentY = current.getTextMatrix().getTranslateY();
                    float lineHeight = lastPosition.getHeight();

                    // Y坐标变化超过行高1.5倍，或字体属性变化
                    return Math.abs(lastY - currentY) > (lineHeight * 1.5f) ||
                            Math.abs(lastFontSize - current.getFontSizeInPt()) > 0.5 ||
                            currentIsBold != isBoldFont(current.getFont());
                }

                private void updateTextAttributes(TextPosition position) {
                    lastFontSize = position.getFontSizeInPt();
                    currentIsBold = isBoldFont(position.getFont());
                    // 居中检测（简化版，实际需要更复杂的布局分析）
                    PDRectangle pageSize = getCurrentPage().getMediaBox();
                    float textWidth = position.getWidth();
                    float xPosition = position.getTextMatrix().getTranslateX();
                    currentIsCenter = Math.abs((pageSize.getWidth() - textWidth)/2 - xPosition) < 20;
                }

                private void processCurrentParagraph() {
                    if (currentParagraph.length() == 0) return;

                    String text = currentParagraph.toString().trim();
                    if (text.isEmpty()) {
                        currentParagraph.setLength(0);
                        return;
                    }

                    // 确保段落前有空行（表格后除外）
//                    if (!lastWasTable && markdown.length() > 0) {
//                        markdown.append("\n");
//                    }
//                    lastWasTable = false;

                    // 记录字体大小分布
//                    pPointBuilder.putSizeInner(currentFontSize, text.length());

                    // 标题检测
                    if (isNextParagraph) {
//                        int titleLevel = determineTitleLevel(currentFontSize, currentIsBold);
                        pPointBuilder.addPoint(currentIsBold, currentIsCenter,
                                null, Double.valueOf(lastFontSize),
                                markdown.length(), markdown.length() + text.length() + 4);

                        // 添加Markdown标题
                        markdown
                                .append("\n")
                                .append("#")
                                .append(" ")
//                                .append("\n")
                                .append(text)
//                                .append("\n\n")
                                .append("\n")
                        ;
                    }
                    // 处理列表（简化检测）
                    else if (text.startsWith("•") || text.startsWith("-") || text.startsWith("*")) {
                        markdown.append("- ").append(text.substring(1).trim()).append("\n");
                    }
                    // 普通段落
                    else {
                        markdown.append(text).append(" ");
                    }

                    currentParagraph.setLength(0);
                }

                @Override
                protected void endPage(PDPage page) throws IOException {
                    processCurrentParagraph(); // 处理最后一节
                    super.endPage(page);
                }
            };

            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            System.out.println(text);

            // 处理表格（简化版，实际PDF表格解析更复杂）
//            detectAndProcessTables(document, markdown);


//            return markdown.toString();
//        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private int determineTitleLevel(double fontSize, boolean isBold) {
//        if (fontSize > 18.0) return 1;
//        if (fontSize > 16.0) return 2;
//        if (fontSize > TITLE_FONT_SIZE_THRESHOLD || isBold) return 3;
//        if (fontSize > SUBTITLE_FONT_SIZE_THRESHOLD) return 4;
//        return 5; // 小标题
//    }

    private boolean isBoldFont(PDFont font) {
        if (font == null) return false;
        String fontName = font.getName().toLowerCase();
        return fontName.contains("bold") || fontName.contains("black");
    }

    // 简化版表格检测（实际PDF表格解析需要更复杂的实现）
    private void detectAndProcessTables(PDDocument document, StringBuilder markdown) {
        // PDF表格检测通常需要专门的库或算法
        // 这里只是示例，实际使用时可能需要集成pdfbox-layout或tabula等工具
//        markdown.append("\n<!-- PDF表格需要专用解析器提取 -->\n\n");
    }

}
