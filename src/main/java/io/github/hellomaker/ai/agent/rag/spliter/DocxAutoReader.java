package io.github.hellomaker.ai.agent.rag.spliter;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDecimalNumber;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DocxAutoReader extends AbstractAutoSplitReader{


    public DocxAutoReader(Resource resourceRef) {
        super(resourceRef);
    }

    public DocxAutoReader(Resource resourceRef, int maxTruckSize, int minTruckSize) {
        super(resourceRef, maxTruckSize, minTruckSize);
    }

    private double defaultFontSize = 10.5;

    @Override
    public void convert(Resource resourceRef, StringBuilder markdown, PPointBuilder pPointBuilder) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(resourceRef.getInputStream())) {
//            Stack<PPoint> pStack = new Stack<>();

//            StringBuilder markdown = new StringBuilder();
            boolean lastWasTable = false;
            AtomicInteger imageIndex = new AtomicInteger(1);
            XWPFStyles styles = doc.getStyles();

//            pStack.push(new PPoint(false, false, 0, 0d, 0, 0));
            pPointBuilder.addPoint(false, false, 0, 0d, 0, 0);
            // 遍历所有文档元素（段落和表格）
            for (IBodyElement element : doc.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH:
                        XWPFParagraph para = (XWPFParagraph) element;
                        String text = para.getText().trim();
                        if (text.isEmpty()) {
                            continue;
                        }
                        // 确保段落前有空行（表格后除外）
                        if (!lastWasTable && markdown.length() > 0) {
                            markdown.append("\n");
                        }
                        lastWasTable = false;

                        boolean isCenter = false;
                        if (para.getAlignment() == ParagraphAlignment.CENTER) {
                            isCenter = true;
                        }

                        boolean isBold = false;
                        Double fontSize = null;

                        // 获取段落默认字体大小
                        List<XWPFRun> runs = para.getRuns();
                        if (runs != null && runs.size() == 1) {
                            fontSize = runs.getFirst().getFontSizeAsDouble();
                            isBold = runs.getFirst().isBold();
                        }
                        if (runs != null) {
                            runs.forEach(run -> {
                                Double fontSizeAsDouble = run.getFontSizeAsDouble();
                                int length = run.text().length();
                                double fontSizeInt = fontSizeAsDouble != null ? fontSizeAsDouble : defaultFontSize;
                                pPointBuilder.putSizeInner(fontSizeInt, length);
                            });
                        }

                        // 新增：标题检测与处理
                        BigInteger titleLevel = titleLevel(para, styles);
                        if (titleLevel != null || isCenter || isBold || (fontSize != null && fontSize > pPointBuilder.getMostMuchSize())) {
                            //# text\n\n = size + 4
//                            pStack.push(new PPoint(isBold, isCenter, titleLevel != null ? titleLevel.intValue() : null, fontSize, markdown.length(), markdown.length() + text.length() + 4));
                            pPointBuilder.addPoint(isBold, isCenter, titleLevel != null ? titleLevel.intValue() : null, fontSize, markdown.length(), markdown.length() + text.length() + 4);
//                            if (markdown.length() > maxPSize) {
//                                handleP(markdown, documents);
//                            } else if (markdown.length() > minPSize){
//                                handleP(markdown, documents);
//                            } else {
                            //新段落
                            markdown.append("#")
                                    .append(" ")
                                    .append(text)
                                    .append("\n\n");
//                            }
                        }
                        //处理列表
                        else if (para.getNumIlvl() != null) {
                            markdown.append("- ").append(text).append("\n");
                        }
                        // 处理代码块（特殊样式）
                        else if (isCodeBlock(para)) {
                            markdown.append("```\n").append(text).append("\n```\n\n");
                        }
                        // 普通段落
                        else {
                            markdown.append(text).append("\n\n");
                        }
                        break;

                    case TABLE:
                        XWPFTable table = (XWPFTable) element;

                        // 确保表格前有空行
                        if (markdown.length() > 0 && !markdown.substring(markdown.length()-2).equals("\n\n")) {
                            markdown.append("\n");
                        }

                        markdown.append(convertTableToMarkdown(table)).append("\n");
                        lastWasTable = true;
                        break;
                    // 在表格处理case中添加

                    default:
                        break;
                }
            }

//            System.out.println("markdown: " + markdown);
//            return markdown.toString();
//            return handleP(markdown, pStack);
        }
    }

    // 辅助方法：判断是否为代码块（可根据样式自定义）
    public boolean isCodeBlock(XWPFParagraph para) {
        String style = para.getStyle();
        return style != null && style.contains("Code");
    }

    // 辅助方法：表格转Markdown
    public String convertTableToMarkdown(XWPFTable table) {
        StringBuilder sb = new StringBuilder();

        // 表头
        XWPFTableRow headerRow = table.getRow(0);
        for (XWPFTableCell cell : headerRow.getTableCells()) {
            sb.append("| ").append(cell.getText().trim()).append(" ");
        }
        sb.append("|\n");

        // 分隔线
        for (int i = 0; i < headerRow.getTableCells().size(); i++) {
            sb.append("| --- ");
        }
        sb.append("|\n");

        // 数据行
        for (int i = 1; i < table.getNumberOfRows(); i++) {
            XWPFTableRow row = table.getRow(i);
            for (XWPFTableCell cell : row.getTableCells()) {
                sb.append("| ").append(cell.getText().trim()).append(" ");
            }
            sb.append("|\n");
        }

        return sb.toString();
    }

    public static BigInteger titleLevel(XWPFParagraph paragraph, XWPFStyles styles) {
        String styleId = paragraph.getStyleID();
        while(styleId != null) {
            final XWPFStyle style = styles.getStyle(styleId);
            if(style == null){
                break;
            }
            BigInteger isTitle = isTitleStyle(style);
            if(isTitle != null){
                return isTitle;
            }
            styleId = style.getBasisStyleID();
        }
        return null;
    }

    public static BigInteger getOutlineLvl(XWPFStyle xwpfStyle) {
        final CTStyle ctStyle = xwpfStyle.getCTStyle();
        if(ctStyle != null) {
            final CTPPrGeneral pPr = ctStyle.getPPr();
            if (pPr != null) {
                final CTDecimalNumber outlineLvl = pPr.getOutlineLvl();
                if (outlineLvl != null) {
                    return outlineLvl.getVal();
                }
            }
        }
        return null;
    }

    /**
     * 判断是否是标题样式
     * @param xwpfStyle 样式
     * @return
     */
    public static BigInteger isTitleStyle(XWPFStyle xwpfStyle){
        final BigInteger outlineLvl = getOutlineLvl(xwpfStyle);
        System.out.println("title level : " + outlineLvl);
        if(outlineLvl != null && BigInteger.ZERO.compareTo(outlineLvl) <= 0){
            return outlineLvl;
        }
//        String name = xwpfStyle.getName();
//        return name.startsWith("heading");
        return null;
    }
}
