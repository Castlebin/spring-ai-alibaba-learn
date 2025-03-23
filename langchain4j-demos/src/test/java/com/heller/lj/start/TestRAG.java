package com.heller.lj.start;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * 测试 RAG
 * <p>
 * RAG 是什么？
 * RAG 是 Retrieval-Augmented Generation 的缩写，
 * 是一种结合了检索和生成的自然语言处理技术。它
 * 通过检索相关信息来增强生成模型的能力，从而提高生成文本的质量和准确性。
 * <p>
 * 具体过程是：
 * 0. 将知识库转化为 Embedding 向量，存放到向量数据库中。
 * 1. 检索：从一个大的知识库中检索出与输入相关的信息。
 * 2. 生成：将检索到的信息与用户的输入信息结合起来，使用生成模型生成最终的输出。
 */
public class TestRAG {

    private static QwenEmbeddingModel EMBEDDING_MODEL;
    private static InMemoryEmbeddingStore<TextSegment> EMBEDDING_STORE;

    @BeforeAll
    public static void init() {
        // 简单用作演示，这里我们使用的是 内存向量数据库
        // 当然也可以使用 Neo4j、ES、PostgreSQL 、Redis 等向量数据库，实际生产环境中使用
        // 向量数据库的使用方式都是一样的
        EMBEDDING_STORE = new InMemoryEmbeddingStore<>();

        // 使用 千问 的 Embedding 模型来做知识库的向量化 （ Embedding ）
        EMBEDDING_MODEL = QwenEmbeddingModel.builder()
                .apiKey("sk-2833a07601ef4c6bbed1fb41c50c2fda")
                .build();

        // 0. 利用 向量模型（Embedding 模型） 将知识库转化为 Embedding 向量，存放到向量数据库中
        List<String> knowledge = getKnowledge();
        for (String text : knowledge) {
            // 这里我们使用的都是文本信息，当然也可以是图片、音频等其他类型的信息
            TextSegment segment = TextSegment.from(text);
            Embedding embedding = EMBEDDING_MODEL.embed(segment).content();

            EMBEDDING_STORE.add(embedding, segment);
        }
    }

    /**
     * 简单演示了一下向量数据库的使用
     */
    @Test
    void test() {
        // 0. 将用户输入的文本转化为向量 （要使用同样的 Embedding 模型）
        String userInput = "退票需要多少钱？";
        Embedding userEmbedding = EMBEDDING_MODEL.embed(userInput).content();

        // 1. 检索：从知识库中检索出与用户输入相关的信息
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(userEmbedding)
                //.maxResults(3) // 返回前 3 个最相似的结果
                //.minScore(0.5) // 最小相似度分数 （小于它的将被过滤掉）
                .build();
        EmbeddingSearchResult<TextSegment> result = EMBEDDING_STORE.search(request);
        // 输出一下查询到的结果 (相似度匹配)
        result.matches().forEach(embeddingMatched -> {
                    System.out.println("匹配到的文本信息：" + embeddingMatched.embedded().text());
                    System.out.println("匹配到的相似度分数：" + embeddingMatched.score());
                    System.out.println("---------------------");
                }
        );
    }


    /**
     * 模拟一下我们原始的知识库数据，都是文本信息
     */
    private static List<String> getKnowledge() {
        List<String> knowledge = new ArrayList<>();
        knowledge.add(""" 
                火车票-退票如何收费
                根据铁路局规定，以发车时间为界，不同时间段申请退票，可能会收取部分退票费。
                
                1、发车前15天（不含）以上退票的，不收取退票费；
                
                2、48小时—15天（含），收取票面5%退票费；
                
                3、24小时—48小时，收取票面10%退票费；
                
                4、24小时以内，收取票面20%退票费；
                
                5、开车前48小时—15天期间内，改签或变更到站至距开车15天以上的其他列车，又在距开车15天前退票的，仍核收5%的退票费，改签后的客票发车是在春运期间铁路部门规定一律收取20%手续费，最终以铁路局实退为准。
                
                最终退款以铁路局实退为准（需收费情况下，四舍五入最低收取2元）。
                """);
        knowledge.add("""
                酒店退改规则
                【生效订单取消】
                          买家需要取消已生效酒店商品订单的（除住院、骨折、怀孕、身亡、不可抗力因素等特殊原因导致的订单取消，需按照《飞猪交易争议处理规范》的相关规定处理外），应按照商家设置的退订政策进行处理。
                
                【未按规定收取退订费用】
                
                          但若酒店承诺不收取退订费用的，则商家不得收取任何费用，若酒店明确退订费用的，则商家只能向买家收取与酒店相同的费用，不得收取额外费用。商家违反前述规定的，视为一般违规行为，每发生一次扣三分，并向买家退回多收的费用。
                """);
        knowledge.add("""
                房价退改的含义
                任意退退改含义：
                ①信用住订单：若是信用住订单酒店未操作买家入住前，客人均可免费取消或退款。若客人未入住，不可扣款。订单只需保留到订单最晚到店时间即可
                
                ②预付订单：若是预付订单入住当日12点后被会员取消，订单需酒店二次确认退款处理（拒绝/退款）才会退款会员，24小时超时未拒绝则会自动退款给会员。12点前被会员取消，仍会直接退款给会员
                
                不可退退改含义：
                接单后不可取消或退款，客人取消需要和酒店协商同意才可退款 。若客人未入住整晚保留房间，可按照担保正常扣除房费（信用住订单需操作未入住扣款，预付订单在离店时间3天后系统会自动扣款）
                
                限时退退改含义：
                若是信用住订单，根据酒店退改时间及扣款规则，如：入住日18点前免费取消，酒店未操作买家入住前，客人均可免费取消或退款；18点后取消扣首晚或者全额
                
                若是预付订单，根据酒店退改时间及扣款规则：（例如：入住当日18点前可取消）
                
                ①会员若在入住当日12点-18点之间订单需酒店二次确认退款处理（拒绝/退款）才会退款会员，24小时超时未拒绝则会自动退款给会员。12点前被会员取消，仍会直接退款给会员。
                
                ②会员若在入住当日18点后，看不到取消入口。需要和酒店协商取消
                
                阶梯退退改含义：
                可设置退改时间及扣款比例（列如：入住日18点前取消扣取订单总额10%的手续费，18点及以后取消，扣取全额房费）
                
                注:若是选择阶梯退的规则，扣款比例不能超过10%否则房型会被屏蔽
                """);
        return knowledge;
    }

}
