package io.github.hellomaker.ai.agent.rag.spliter;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;

import java.net.MalformedURLException;
import java.util.*;

/**
 * @author xianzhikun
 */
public class EnhancedTikaDocumentReader extends TikaDocumentReader {

    public EnhancedTikaDocumentReader(Resource resource) {
        super(resource);
        this.resourceRef = resource;
    }

    private Resource resourceRef;
    TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

    @Override
    public List<Document> get() {
        if (resourceRef.getFilename().endsWith("docx")) {
//                List<Document> convert = convert();
//                for (Document document : convert) {
//                    document.getMetadata().put("source", resourceRef.getFilename());
//                }
//                return convert;
            DocxAutoReader docxReader = new DocxAutoReader(resourceRef);
            return docxReader.read();
        } else if (resourceRef.getFilename().endsWith("pdf")) {
            PdfAutoReader pdfReader = new PdfAutoReader(resourceRef);
            return pdfReader.read();
        }

        List<Document> documents = super.get();
        return tokenTextSplitter.apply(documents);
    }

    public static void main(String[] args) {
        try {
//            String file = "C:\\Users\\Administrator\\Downloads/iXLAB Inventory医用库存管理软件使用说明书.docx";
//            String file = "C:\\Users\\Administrator\\Downloads/阳朔出游行程安排.docx";
//            String file = "C:\\Users\\Administrator\\Downloads/EORTC_MSGERC指南系列文章和国内血液病IFD诊治原则_cdf993a3-7656-4db1-8620-861c23677cb3.pdf";
            String file = "C:\\Users\\Administrator\\Downloads/WS_T_791-2021_钩虫检测及虫种鉴定标准_钩蚴培养法_62698c85-0dcf-48ca-98a5-f39a145abd20.pdf";
            EnhancedTikaDocumentReader enhancedTikaDocumentReader =
                    new EnhancedTikaDocumentReader(new FileUrlResource(file));
            List<Document> documents = enhancedTikaDocumentReader.get();
            int index = 1;
            for (Document document : documents) {
                System.out.println("段落" + index++ + "=============\n" + document.getText());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}

