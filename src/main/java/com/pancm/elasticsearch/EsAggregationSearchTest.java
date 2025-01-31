package com.pancm.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.aggregations.pipeline.BucketSelectorPipelineAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pancm
 * @Title: pancm_project
 * @Description:ES聚合查询测试用例
 * @Version:1.0.0
 * @Since:jdk1.8
 * @date 2019/4/2
 */
public class EsAggregationSearchTest {


    private static String elasticIp = "192.168.77.130";

    private static int elasticPort = 9200;

    private static Logger logger = LoggerFactory.getLogger(EsHighLevelRestSearchTest.class);

    private static RestHighLevelClient client = null;

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {
            init();
            createIndex();
            bulk();
            groupbySearch();
            avgSearch();
            maxSearch();
            sumSearch();
            avgGroupSearch();
            maxGroupSearch();
            sumGroupSearch();
            topSearch();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }

    }


    /*
     * 初始化服务
     */
    private static void init() {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(elasticIp, elasticPort));
        client = new RestHighLevelClient(restClientBuilder);
    }

    /*
     * 关闭服务
     */
    private static void close() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                client = null;
            }
        }
    }

    /**
     * 创建索引
     *
     * @throws IOException
     */
    private static void createIndex() throws IOException {

        // 类型
        String type = "_doc";
        String index = "student";
        // setting 的值
        Map<String, Object> setmapping = new HashMap<>();
        // 分区数、副本数、缓存刷新时间
        setmapping.put("number_of_shards", 10);
        setmapping.put("number_of_replicas", 1);
        setmapping.put("refresh_interval", "5s");
        Map<String, Object> keyword = new HashMap<>();
        //设置类型
        keyword.put("type", "keyword");
        Map<String, Object> lon = new HashMap<>();
        //设置类型
        lon.put("type", "long");
        Map<String, Object> date = new HashMap<>();
        //设置类型
        date.put("type", "date");
        date.put("format", "yyyy-MM-dd");

        Map<String, Object> date2 = new HashMap<>();
        //设置类型
        date2.put("type", "date");
        date2.put("format", "yyyy-MM-dd HH:mm:ss.SSS");
        Map<String, Object> jsonMap2 = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        //设置字段message信息
        properties.put("uid", lon);
        properties.put("grade", lon);
        properties.put("class", lon);
        properties.put("age", lon);
        properties.put("name", keyword);
        properties.put("createtm", date);
        properties.put("updatetm", date2);
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);
        jsonMap2.put(type, mapping);

        GetIndexRequest getRequest = new GetIndexRequest();
        getRequest.indices(index);
        getRequest.types(type);
        getRequest.local(false);
        getRequest.humanReadable(true);
        boolean exists2 = client.indices().exists(getRequest, RequestOptions.DEFAULT);
        //如果存在就不创建了
        if (exists2) {
            System.out.println(index + "索引库已经存在!");
            return;
        }
        // 开始创建库
        CreateIndexRequest request = new CreateIndexRequest(index);
        try {
            // 加载数据类型
            request.settings(setmapping);
            //设置mapping参数
            request.mapping(type, jsonMap2);
            //设置别名
            request.alias(new Alias("pancm_alias"));
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            boolean falg = createIndexResponse.isAcknowledged();
            if (falg) {
                System.out.println("创建索引库:" + index + "成功！");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * 批量操作示例
     *
     * @throws InterruptedException
     */
    private static void bulk() throws IOException {
        // 类型
        String type = "_doc";
        String index = "student";

        BulkRequest request = new BulkRequest();
        int k = 10;
        List<Map<String, Object>> mapList = new ArrayList<>();
        LocalDateTime ldt = LocalDateTime.now();
        for (int i = 1; i <= k; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("uid", i);
            map.put("age", i);
            map.put("name", "虚无境" + (i % 3));
            map.put("class", i % 10);
            map.put("grade", 400 + i);
            map.put("createtm", ldt.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            map.put("updatetm", ldt.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            if (i == 5) {
                map.put("updatetm", "2019-11-31 21:04:55.268");
            }
            mapList.add(map);
        }


        for (int i = 0; i < mapList.size(); i++) {
            Map<String, Object> map = mapList.get(i);
            String id = map.get("uid").toString();
            // 可以进行修改/删除/新增 操作
            //docAsUpsert 为true表示存在更新，不存在插入，为false表示不存在就是不做更新
            request.add(new UpdateRequest(index, type, id).doc(map, XContentType.JSON).docAsUpsert(true).retryOnConflict(5));
        }

        client.bulk(request, RequestOptions.DEFAULT);
        System.out.println("批量执行成功！");
    }

    /**
     * @return void
     * @Author pancm
     * @Description 多个聚合条件测试
     * SQL: select age, name, count(*) as count1 from student group by age, name;
     * @Date 2019/7/3
     * @Param []
     **/
    private static void groupbySearch() throws IOException {
        String buk = "group";
        AggregationBuilder aggregation = AggregationBuilders.terms("age").field("age");
        AggregationBuilder aggregation2 = AggregationBuilders.terms("name").field("name");
        //根据创建时间按天分组
        AggregationBuilder aggregation3 = AggregationBuilders.dateHistogram("createtm")
                .field("createtm")
                .format("yyyy-MM-dd")
                .dateHistogramInterval(DateHistogramInterval.DAY);

        aggregation2.subAggregation(aggregation3);
        aggregation.subAggregation(aggregation2);
        agg(aggregation, buk);
    }

    /**
     * @return void
     * @Author pancm
     * @Description 平均聚合查询测试用例
     * @Date 2019/4/1
     * @Param []
     **/
    private static void avgSearch() throws IOException {

        String buk = "t_grade_avg";
        //直接求平均数
        AggregationBuilder aggregation = AggregationBuilders.avg(buk).field("grade");
        logger.info("求班级的平均分数:");
        agg(aggregation, buk);

    }

    private static void maxSearch() throws IOException {
        String buk = "t_grade";
        AggregationBuilder aggregation = AggregationBuilders.max(buk).field("grade");
        logger.info("求班级的最高分数:");
        agg(aggregation, buk);
    }

    private static void sumSearch() throws IOException {
        String buk = "t_grade";
        AggregationBuilder aggregation = AggregationBuilders.sum(buk).field("grade");
        logger.info("求班级的总分数:");
        agg(aggregation, buk);
    }

    /**
     * @return void
     * @Author pancm
     * @Description 平均聚合查询测试用例
     * @Date 2019/4/1
     * @Param []
     **/
    private static void avgGroupSearch() throws IOException {


        String agg = "t_class_avg";
        String buk = "t_grade";
        //terms 就是分组统计 根据student的grade成绩进行分组并创建一个新的聚合
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(agg).field("class");
        aggregation.subAggregation(AggregationBuilders.avg(buk).field("grade"));

        logger.info("根据班级求平均分数:");
        agg(aggregation, agg, buk);

    }


    private static void maxGroupSearch() throws IOException {

        String agg = "t_class_max";
        String buk = "t_grade";
        //terms 就是分组统计 根据student的grade成绩进行分组并创建一个新的聚合
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(agg).field("class");
        aggregation.subAggregation(AggregationBuilders.max(buk).field("grade"));
        logger.info("根据班级求最大分数:");
        agg(aggregation, agg, buk);
    }


    private static void sumGroupSearch() throws IOException {
        String agg = "t_class_sum";
        String buk = "t_grade";
        //terms 就是分组统计 根据student的grade成绩进行分组并创建一个新的聚合
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(agg).field("class");
        aggregation.subAggregation(AggregationBuilders.sum(buk).field("grade"));

        logger.info("根据班级求总分:");
        agg(aggregation, agg, buk);
    }


    protected static void agg(AggregationBuilder aggregation, String buk) throws IOException {
        SearchResponse searchResponse = search(aggregation);
        if (RestStatus.OK.equals(searchResponse.status())) {
            // 获取聚合结果
            Aggregations aggregations = searchResponse.getAggregations();

            if (buk.contains("avg")) {
                //取子聚合
                Avg ba = aggregations.get(buk);
                logger.info(buk + ":" + ba.getValue());
                logger.info("------------------------------------");
            } else if (buk.contains("max")) {
                //取子聚合
                Max ba = aggregations.get(buk);
                logger.info(buk + ":" + ba.getValue());
                logger.info("------------------------------------");

            } else if (buk.contains("min")) {
                //取子聚合
                Min ba = aggregations.get(buk);
                logger.info(buk + ":" + ba.getValue());
                logger.info("------------------------------------");
            } else if (buk.contains("sum")) {
                //取子聚合
                Sum ba = aggregations.get(buk);
                logger.info(buk + ":" + ba.getValue());
                logger.info("------------------------------------");
            } else if (buk.contains("top")) {
                //取子聚合TopHits
                TopHits ba = aggregations.get(buk);
                logger.info(buk + ":" + ba.getHits().getTotalHits());
                logger.info("------------------------------------");
            } else if (buk.contains("group")) {
                Map<String, Object> map = new HashMap<>();
                List<Map<String, Object>> list = new ArrayList<>();
                agg(map, list, aggregations);
                logger.info("聚合查询结果:" + list);
                logger.info("------------------------------------");
            }

        }
    }

    private static void agg(Map<String, Object> map, List<Map<String, Object>> list, Aggregations aggregations) {
        aggregations.forEach(aggregation -> {
            String name = aggregation.getName();
            Terms genders = aggregations.get(name);
            for (Terms.Bucket entry : genders.getBuckets()) {
                String key = entry.getKey().toString();
                long t = entry.getDocCount();
                map.put(name, key);
                map.put(name + "_" + "count", t);

                //判断里面是否还有嵌套的数据
                List<Aggregation> list2 = entry.getAggregations().asList();
                if (list2.isEmpty()) {
                    Map<String, Object> map2 = new HashMap<>();
                    BeanUtils.copyProperties(map, map2);
                    list.add(map2);
                } else {
                    agg(map, list, entry.getAggregations());
                }
            }
        });
    }

    private static void agg(List<Map<String, Object>> list, Aggregations aggregations) {
        aggregations.forEach(aggregation -> {
            String name = aggregation.getName();
            Terms genders = aggregations.get(name);
            for (Terms.Bucket entry : genders.getBuckets()) {
                String key = entry.getKey().toString();
                long t = entry.getDocCount();
                Map<String, Object> map = new HashMap<>();
                map.put(name, key);
                map.put(name + "_" + "count", t);
                //判断里面是否还有嵌套的数据
                List<Aggregation> list2 = entry.getAggregations().asList();
                if (list2.isEmpty()) {
                    list.add(map);
                } else {
                    agg(list, entry.getAggregations());
                }
            }
        });
        System.out.println(list);
    }


    private static SearchResponse search(AggregationBuilder aggregation) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("student");
        searchRequest.types("_doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //不需要解释
        searchSourceBuilder.explain(false);
        //不需要原始数据
        searchSourceBuilder.fetchSource(false);
        //不需要版本号
        searchSourceBuilder.version(false);
        searchSourceBuilder.aggregation(aggregation);
        logger.info("查询的语句:" + searchSourceBuilder.toString());
        searchRequest.source(searchSourceBuilder);
        // 同步查询
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse;
    }

    /**
     * @return void
     * @Author pancm
     * @Description 进行聚合
     * @Date 2019/4/2
     * @Param []
     **/
    protected static void agg(AggregationBuilder aggregation, String agg, String buk) throws IOException {

        // 同步查询
        SearchResponse searchResponse = search(aggregation);

        //4、处理响应
        //搜索结果状态信息
        if (RestStatus.OK.equals(searchResponse.status())) {
            // 获取聚合结果
            Aggregations aggregations = searchResponse.getAggregations();


            //分组
            Terms byAgeAggregation = aggregations.get(agg);
            logger.info(agg + " 结果");
            logger.info("name: " + byAgeAggregation.getName());
            logger.info("type: " + byAgeAggregation.getType());
            logger.info("sumOfOtherDocCounts: " + byAgeAggregation.getSumOfOtherDocCounts());

            logger.info("------------------------------------");
            for (Terms.Bucket buck : byAgeAggregation.getBuckets()) {
                logger.info("key: " + buck.getKeyAsNumber());
                logger.info("docCount: " + buck.getDocCount());
                logger.info("docCountError: " + buck.getDocCountError());


                if (agg.contains("avg")) {
                    //取子聚合
                    Avg ba = buck.getAggregations().get(buk);
                    logger.info(buk + ":" + ba.getValue());
                    logger.info("------------------------------------");
                } else if (agg.contains("max")) {
                    //取子聚合
                    Max ba = buck.getAggregations().get(buk);
                    logger.info(buk + ":" + ba.getValue());
                    logger.info("------------------------------------");

                } else if (agg.contains("min")) {
                    //取子聚合
                    Min ba = buck.getAggregations().get(buk);
                    logger.info(buk + ":" + ba.getValue());
                    logger.info("------------------------------------");
                } else if (agg.contains("sum")) {
                    //取子聚合
                    Sum ba = buck.getAggregations().get(buk);
                    logger.info(buk + ":" + ba.getValue());
                    logger.info("------------------------------------");
                }
            }
        }
    }

    /**
     * @return void
     * @Author pancm
     * @Description having
     * @Date 2020/8/21
     * @Param []
     **/
    private static void havingSearch() throws IOException {
        String index = "";
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.indices(index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
        String alias_name = "nas_ip_address_group";
        String group_name = "nas_ip_address";
        String query_name = "acct_start_time";
        String query_type = "gte,lte";
        String query_name_value = "2020-08-05 13:25:55,2020-08-20 13:26:55";
        String[] query_types = query_type.split(",");
        String[] query_name_values = query_name_value.split(",");

        for (int i = 0; i < query_types.length; i++) {
            if ("gte".equals(query_types[i])) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery(query_name).gte(query_name_values[i]));
            }
            if ("lte".equals(query_types[i])) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery(query_name).lte(query_name_values[i]));
            }
        }

        AggregationBuilder aggregationBuilder = AggregationBuilders.terms(alias_name).field(group_name).size(Integer.MAX_VALUE);

        //声明BucketPath，用于后面的bucket筛选
        Map<String, String> bucketsPathsMap = new HashMap<>(8);
        bucketsPathsMap.put("groupCount", "_count");
        //设置脚本
        Script script = new Script("params.groupCount >= 1000");
        //构建bucket选择器
        BucketSelectorPipelineAggregationBuilder bs =
                PipelineAggregatorBuilders.bucketSelector("having", bucketsPathsMap, script);
        aggregationBuilder.subAggregation(bs);
        sourceBuilder.aggregation(aggregationBuilder);
        //不需要解释
        sourceBuilder.explain(false);
        //不需要原始数据
        sourceBuilder.fetchSource(false);
        //不需要版本号
        sourceBuilder.version(false);
        sourceBuilder.query(boolQueryBuilder);
        searchRequest.source(sourceBuilder);


        System.out.println(sourceBuilder);
        // 同步查询
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        // 查询条数
        long count = searchResponse.getHits().getHits().length;
        Aggregations aggregations = searchResponse.getAggregations();
//        agg(aggregations);
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        agg(list, aggregations);
//        System.out.println(map);
        System.out.println(list);
    }


    /**
     * @return void
     * @Author pancm
     * @Description 去重
     * @Date 2020/8/26
     * @Param []
     **/
    private static void distinctSearch() throws IOException {
        String buk = "group";
        String distinctName = "name";
        AggregationBuilder aggregation = AggregationBuilders.terms("age").field("age");
        CardinalityAggregationBuilder cardinalityBuilder = AggregationBuilders.cardinality(distinctName).field(distinctName);
        //根据创建时间按天分组
//        AggregationBuilder aggregation3 = AggregationBuilders.dateHistogram("createtm")
//                .field("createtm")
//                .format("yyyy-MM-dd")
//                .dateHistogramInterval(DateHistogramInterval.DAY);
//
//        aggregation2.subAggregation(aggregation3);
        aggregation.subAggregation(cardinalityBuilder);
        agg(aggregation, buk);
    }


    private static void topSearch() throws IOException {


    }


}
