package com.heima.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.heima.admin.service.ReviewMediaArticleService;
import com.heima.common.aliyun.AliyunImageScanRequest;
import com.heima.common.aliyun.AliyunTextScanRequest;
import com.heima.common.common.contants.ESIndexConstants;
import com.heima.common.common.pojo.EsIndexEntity;
import com.heima.common.kafka.KafkaSender;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.crawler.core.parse.ZipUtils;
import com.heima.model.mappers.admin.AdChannelMapper;
import com.heima.model.mappers.app.*;
import com.heima.model.mappers.wemedia.WmNewsMapper;
import com.heima.model.mappers.wemedia.WmUserMapper;
import com.heima.model.media.pojos.WmNews;
import com.heima.model.media.pojos.WmUser;
import com.heima.model.user.pojos.ApUserMessage;
import com.heima.utils.common.Compute;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Slf4j
@Service
public class ReviewMediaArticleServiceImpl implements ReviewMediaArticleService {


    @Autowired
    private WmNewsMapper wmNewsMapper;

    @Autowired
    private AliyunTextScanRequest aliyunTextScanRequest;

    @Autowired
    private AliyunImageScanRequest aliyunImageScanRequest;

    @Autowired
    private ApAuthorMapper apAuthorMapper;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    @Autowired
    private AdChannelMapper adChannelMapper;

    @Autowired
    KafkaSender kafkaSender;

    @Autowired
    private JestClient jestClient;

    @Autowired
    private ApUserMessageMapper apUserMessageMapper;


    /**
     * 主图数量
     */
    private static final Integer MAIN_PICTURE_SIZE = 5;

    @Value("${FILE_SERVER_URL}")
    private String FILE_SERVER_URL;



    @Override
    public void autoReviewArticleByMedia(Integer newsId) {
        //根据文章id查询文章信息
        WmNews wmNews = wmNewsMapper.selectNewsDetailByPrimaryKey(newsId);
        if(wmNews != null){
            //判断当前状态是否等于4，如果等于4说明已通过人工审核，可直接保存数据
            if(wmNews.getStatus() == 4){
                reviewSuccessSaveAll(wmNews);
                return;
            }
            //审核通过后的待发布文章，判断发布时间
            if(wmNews.getStatus() == 8 && wmNews.getPublishTime() != null && wmNews.getPublishTime().getTime() < new Date().getTime()){
                reviewSuccessSaveAll(wmNews);
                return;
            }
            //待审核状态
            if(wmNews.getStatus() == 1){

                //审核文章内容与标题的匹配度
                String content = wmNews.getContent();
                String title  = wmNews.getTitle();
                double degree = Compute.SimilarDegree(content, title);
                if(degree <= 0){
                    updateWmNews(wmNews,(short)2,"标题与内容不匹配" );
                    return;
                }
                //审核文章内容信息，阿里接口
                List<String> images = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                JSONArray jsonArray = JSON.parseArray(content);
//                List<Map> jsonArray = JSON.parseObject(content, List.class);
                handlerTextAndImages(images,sb,jsonArray);
                try {
                    String response = aliyunTextScanRequest.textScanRequest(sb.toString());
                    if("review".equals(response)){
                        updateWmNews(wmNews,(short)3,"需要人工审核");
                        return;
                    }
                    if("block".equals(response)){
                        updateWmNews(wmNews, (short) 2,"审核失败");
                        return;
                    }
                    String imageResponse = aliyunImageScanRequest.imageScanRequest(images);
                    if("review".equals(imageResponse)){
                        updateWmNews(wmNews, (short) 3,"图片需要人工审核");
                        return;
                    }
                    if("block".equals(imageResponse)){
                        updateWmNews(wmNews, (short) 2,"图片审核失败");
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                //审核文章中图片的额信息，阿里接口

                if(wmNews.getPublishTime() !=null){
                    if(wmNews.getPublishTime().getTime() > new Date().getTime()){
                        //如果发布时间大于当前时间修改wmNewas.status 状态为8
                        updateWmNews(wmNews, (short) 8,"待发布");
                    }else {
                        //立即发布
                        reviewSuccessSaveAll(wmNews);
                    }
                }else {
                    //立即发布
                    reviewSuccessSaveAll(wmNews);
                }
            }

        }

    }

    /**
     * 处理文本内容。提取出文本和图片
     * @param images
     * @param sb
     * @param jsonArray
     */
    private void handlerTextAndImages(List<String> images, StringBuilder sb, JSONArray jsonArray) {
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            String type = (String) jsonObject.get("type");
            if(type.equals("image")){
                String value = (String) jsonObject.get("value");
                images.add(value);
            }
            if ("text".equals(type)){
                String value = (String) jsonObject.get("value");
                sb.append(value);
            }
        }

    }

    /**
     * 修改状态
     * @param wmNews
     * @param status
     * @param message
     */
    private void updateWmNews(WmNews wmNews, Short status, String message) {

        wmNews.setStatus(status);
        wmNews.setReason(message);

        wmNewsMapper.updateByPrimaryKeySelective(wmNews);
    }

    /**
     * 保存数据
     *
     * @param wmNews
     */
    private void reviewSuccessSaveAll(WmNews wmNews) {
        ApAuthor apAuthor = null;
        if(wmNews.getUserId() != null){
            WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
            if(wmUser != null && wmUser.getName() != null){
                apAuthor = apAuthorMapper.selectByAuthorName(wmUser.getName());
                if (apAuthor == null || apAuthor.getId() == null) {
                    apAuthor = new ApAuthor();
                    apAuthor.setCreatedTime(new Date());
                    apAuthor.setName(wmUser.getName());
                    apAuthor.setType(1);
                    apAuthor.setUserId(wmUser.getApUserId());
                    apAuthor.setWmUserId(Long.valueOf(wmUser.getId()));
                    apAuthorMapper.insert(apAuthor);
                }
            }
        }

        ApArticle apArticle = new ApArticle();
        if(apAuthor != null){
            apArticle.setAuthorId(apAuthor.getId().longValue());
            apArticle.setAuthorName(apAuthor.getName());
        }
        apArticle.setCreatedTime(new Date());
        if(wmNews.getChannelId() != null){
            AdChannel adChannel = adChannelMapper.selectByPrimaryKey(wmNews.getChannelId());
            apArticle.setChannelId(adChannel.getId());
            apArticle.setChannelName(adChannel.getName());
        }
        apArticle.setLayout(wmNews.getType());
        apArticle.setTitle(wmNews.getTitle());
        String images = wmNews.getImages();
        if(!StringUtils.isEmpty(images)){
            String[] split = images.split(",");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if(i != 0){
                    stringBuilder.append(",");
                }
                stringBuilder.append(FILE_SERVER_URL);
                stringBuilder.append(split[i]);
            }
            apArticle.setImages(stringBuilder.toString());
        }

        apArticleMapper.insert(apArticle);


        ApArticleContent apArticleContent = new ApArticleContent();
        apArticleContent.setArticleId(apArticle.getId());
        apArticleContent.setContent(ZipUtils.gzip(wmNews.getContent()));
        apArticleContentMapper.insert(apArticleContent);

        ApArticleConfig apArticleConfig = new ApArticleConfig();
        apArticleConfig.setArticleId(apArticle.getId());
        apArticleConfig.setIsComment(true);
        apArticleConfig.setIsDelete(false);
        apArticleConfig.setIsDown(false);
        apArticleConfig.setIsForward(true);
        apArticleConfigMapper.insert(apArticleConfig);



        EsIndexEntity esIndexEntity = new EsIndexEntity();
        esIndexEntity.setId(new Long(apArticle.getId()));
        esIndexEntity.setChannelId(new Long(wmNews.getChannelId()));
        esIndexEntity.setContent(ZipUtils.gzip(wmNews.getContent()));
        esIndexEntity.setPublishTime(new Date());
        esIndexEntity.setStatus(new Long(1));
        esIndexEntity.setTitle(wmNews.getTitle());
        esIndexEntity.setUserId(wmNews.getUserId());
        esIndexEntity.setTag("media");

        Index.Builder builder = new Index.Builder(esIndexEntity);
        builder.id(apArticle.getId().toString());
        builder.refresh(true);
        Index build = builder.index(ESIndexConstants.ARTICLE_INDEX).type(ESIndexConstants.DEFAULT_DOC).build();

        JestResult jestResult = null;

        try {
            jestResult = jestClient.execute(build);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("执行ES创建索引失败：message:{}", e.getMessage());
        }
        if (jestResult != null && !jestResult.isSucceeded()) {
            //throw new RuntimeException(result.getErrorMessage() + "插入更新索引失败!");
            log.error("插入更新索引失败：message:{}", jestResult.getErrorMessage());
        }

        //修改wmNews的状态为9
        wmNews.setArticleId(apArticle.getId());
        updateWmNews(wmNews, (short) 9, "审核成功");
        //通知用户 文章审核通过
        saveApUserMessage(wmNews, 108, "文章审核通过");




    }

    private void saveApUserMessage(WmNews wmNews, int i, String s) {
        ApUserMessage apUserMessage = new ApUserMessage();
        apUserMessage.setUserId(wmNews.getUserId());
        apUserMessage.setCreatedTime(new Date());
        apUserMessage.setIsRead(false);
        apUserMessage.setType(i);
        apUserMessage.setContent(s);
        apUserMessageMapper.insertSelective(apUserMessage);
    }
}

