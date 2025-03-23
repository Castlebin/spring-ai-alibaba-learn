package com.heller.lj;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(EmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        return args -> {
            initRAGStore(embeddingStore, embeddingModel);
        };
    }

    /**
     * RAG 数据库的初始化，从文件系统中加载了原始知识库数据，存放到向量数据库中
     * 简单的演示了一下向量数据库的使用，使用的是 InMemoryEmbeddingStore （内存中）
     */
    public void initRAGStore(EmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        // 从文件系统中加载原始知识库数据
        List<Document> documents = getKnowledge();
        // 使用分割器将文档 分割 成多个文本片段
        List<TextSegment> segments = splitDocuments(documents);

        // 0. 利用 向量模型（Embedding 模型） 将知识库转化为 Embedding 向量，存放到向量数据库中
        // EMBEDDING_STORE.addAll(EMBEDDING_MODEL.embedAll(segments).content());  (可以直接使用这句将全部数据转化为向量并添加到向量数据库中)
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
    }

    /**
     * 使用文件系统作为知识库数据来源
     */
    private static List<Document> getKnowledge() {
        // 使用 ClassPathDocumentLoader 来加载文档，使用 TextDocumentParser 来解析文档
        DocumentParser documentParser = new TextDocumentParser();
        List<Document> documents = ClassPathDocumentLoader.loadDocumentsRecursively("docs/", documentParser);
        return documents;
    }

    private static List<TextSegment> splitDocuments(List<Document> documents) {
        // 使用 某个文本分割器 来将文档分割成多个文本片段 （分块  chunk ）
        // 这里我们使用的是 DocumentByRegexSplitter 来分割文档
        DocumentByRegexSplitter splitter = new DocumentByRegexSplitter(
                "\\n\\d+\\.", // 正则表达式
                "\n",  // 保留换行符
                120, // 每个文本片段的最大长度
                10, // 允许重叠的最大字符数
                new DocumentByCharacterSplitter(100, 20) // 子分割器
        );

        List<TextSegment> result = new ArrayList<>();
        for (Document document : documents) {
            List<TextSegment> segments = splitter.split(document);
            result.addAll(segments);
        }
        return result;
    }

}
