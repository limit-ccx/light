package com.zhiguang.config;

import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private String provider = "deepseek";
    private String apiKey;
    private String model = "deepseek-chat";
    private String baseUrl = "https://api.deepseek.com/v1";
    private int maxContentLength = 10000;
    private int timeoutSeconds = 60;

    /** 每用户每日 AI 调用配额（计入润色/写作助手/一键生成/流式等所有 LLM 端点）；<=0 表示不限制 */
    private int dailyQuota = 50;

    /**
     * 润色模式配置（prompt / temperature / max_tokens）。
     * 代码内为默认值，可通过 ai.prompts.<mode>.* 覆盖而无需改代码；
     * 润色/精简等高忠实性任务 temperature 0.4，扩写保留 0.7。
     */
    private Map<String, PromptSetting> prompts = defaultPrompts();

    /**
     * 真实 Embedding 服务配置（OpenAI 兼容 /embeddings 协议）。
     * <p>
     * DeepSeek 不提供 embeddings API，此前"chat 模型输出 512 维 JSON 数组"
     * 是伪向量（语义检索质量不可靠）。默认指向 SiliconFlow 的 BAAI/bge-m3
     * （有免费额度，1024 维）；智谱/阿里 DashScope/OpenAI 同协议可平滑替换。
     * api-key 未配置时自动降级回伪向量路径（仅限开发），并打 WARN。
     * <p>
     * ⚠️ dimension 必须与 vector.postgres.dimension 一致；
     * 更换 embedding 模型后必须重建全量向量（管理端 /api/vectors/admin/rebuild），
     * 不同模型的向量混在同一表中相似度计算无意义。
     */
    private EmbeddingSetting embedding = new EmbeddingSetting();

    /**
     * 多平台模型配置（管理端 Agent 的模型选择）。密钥一律经环境变量注入，代码与仓库零明文：
     * DEEPSEEK_API_KEY / GLM_API_KEY / MIMO_API_KEY / QWEN_API_KEY。
     * model=标准模型，reasoningModel=深度思考模型（思考强度=deep 时切换）。
     */
    private Map<String, ProviderSetting> providers = defaultProviders();

    static Map<String, ProviderSetting> defaultProviders() {
        Map<String, ProviderSetting> map = new LinkedHashMap<>();
        map.put("deepseek", new ProviderSetting(
                "https://api.deepseek.com/v1", "${DEEPSEEK_API_KEY:}", "deepseek-chat", "deepseek-reasoner"));
        map.put("glm", new ProviderSetting(
                "https://open.bigmodel.cn/api/paas/v4", "${GLM_API_KEY:}", "glm-4-flash", "glm-z1-flash"));
        map.put("qwen", new ProviderSetting(
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "${QWEN_API_KEY:}", "qwen-plus", "qwen-plus"));
        map.put("mimo", new ProviderSetting(
                "${MIMO_BASE_URL:}", "${MIMO_API_KEY:}", "MiMo-7B-RL", "MiMo-7B-RL"));
        return map;
    }

    @Data
    public static class ProviderSetting {
        private String baseUrl;
        private String apiKey;
        private String model;
        private String reasoningModel;

        public ProviderSetting() {
        }

        public ProviderSetting(String baseUrl, String apiKey, String model, String reasoningModel) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.reasoningModel = reasoningModel;
        }
    }

    @Data
    public static class EmbeddingSetting {
        /** 留空则降级为 LLM 伪向量（开发用） */
        private String apiKey = "";
        /** OpenAI 兼容服务地址 */
        private String baseUrl = "https://api.siliconflow.cn/v1";
        private String model = "BAAI/bge-m3";
        /** 须与 vector.postgres.dimension 一致（bge-m3 = 1024） */
        private int dimension = 1024;
    }

    static Map<String, PromptSetting> defaultPrompts() {
        Map<String, PromptSetting> map = new LinkedHashMap<>();
        map.put("polish", new PromptSetting(
                "你是一位专业的中文写作编辑。请对以下文章内容进行润色优化，提升表达的流畅性、准确性和文学性，但保持原意不变。直接输出润色后的文本，不要添加任何解释说明。",
                0.4, 4096));
        map.put("formal", new PromptSetting(
                "你是一位专业的公文写作编辑。请将以下内容改写为更加正式、规范的书面语风格，保持原意不变。直接输出改写后的文本，不要添加任何解释说明。",
                0.4, 4096));
        map.put("casual", new PromptSetting(
                "你是一位轻松风格的文案编辑。请将以下内容改写为更加轻松、亲切、口语化的风格，保持原意不变。直接输出改写后的文本，不要添加任何解释说明。",
                0.4, 4096));
        map.put("concise", new PromptSetting(
                "你是一位精简写作专家。请将以下内容精简提炼，去除冗余表述，保留核心信息。直接输出精简后的文本，不要添加任何解释说明。",
                0.4, 4096));
        map.put("expand", new PromptSetting(
                "你是一位内容扩写专家。请在保持原意的基础上，对以下内容进行合理扩写，增加细节和论述，使内容更加丰富饱满。直接输出扩写后的文本，不要添加任何解释说明。",
                0.7, 4096));
        return map;
    }

    @PostConstruct
    public void warnIfApiKeyMissing() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI api-key 未配置（应通过环境变量 AI_API_KEY 注入），所有 AI 功能将不可用");
        }
    }

    /**
     * LLM 调用统一使用的 WebClient 单例。
     * 复用底层连接池；此前每次调用都 build 新实例，连接无法复用且无连接超时。
     */
    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds + 10L));
        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Data
    public static class PromptSetting {

        private String prompt;
        private double temperature;
        private int maxTokens;

        public PromptSetting() {
        }

        public PromptSetting(String prompt, double temperature, int maxTokens) {
            this.prompt = prompt;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }
    }
}
