package com.heller.lj.start;

import org.junit.jupiter.api.Test;

import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;

public class Test02 {

    /**
     * 使用 DashScope 的 Wanx 模型生成图片
     */
    // https://bailian.console.aliyun.com/#/model-market/detail/wanx2.1-t2i-plus?tabKey=sdk
    @Test
    void testWanx() { // Ollama
        WanxImageModel model = WanxImageModel.builder()
                .modelName("wanx2.1-t2i-plus")
                .apiKey("sk-2833a07601ef4c6bbed1fb41c50c2fda")
                .build();

        Response<Image> imageResponse = model.generate("一只可爱的猫咪在阳光下玩耍");
        System.out.println(imageResponse.content().url());
    }

}
