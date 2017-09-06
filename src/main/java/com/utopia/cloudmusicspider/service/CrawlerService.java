package com.utopia.cloudmusicspider.service;

import com.utopia.cloudmusicspider.model.SongModel;
import com.utopia.cloudmusicspider.model.WebPageModel;
import com.utopia.cloudmusicspider.model.WebPageModel.PageType;
import com.utopia.cloudmusicspider.model.WebPageModel.CrawledStatus;
import com.utopia.cloudmusicspider.repository.WebPageModelRepository;
import com.utopia.cloudmusicspider.repository.SongModelRepository;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by utopia on 2017/8/30.
 * Copyright © 2017 utopia. All rights reserved.
 */


@Service
public class CrawlerService {

    private final CacheManager cacheManager;
    private final String cacheName = "com.utopia.SongModels";

    private static final Integer MAX_THREADS = 20;
    @Autowired
    SongModelRepository songModelRepository;
    @Autowired
    WebPageModelRepository webPageModelRepository;

    public CrawlerService() {
        cacheManager = CacheManager.getInstance();
    }

    public WebPageModel savePage(WebPageModel webPageModel) {
        WebPageModel result = webPageModelRepository.findOne(webPageModel.getUrl());
        return result == null ? webPageModelRepository.saveAndFlush(webPageModel) : result;
    }

    public SongModel saveSongModel(SongModel songModel) {
        SongModel result = songModelRepository.findOne(songModel.getUrl());
        if(result == null) {
            result = songModelRepository.saveAndFlush(songModel);
        } else {
            result.setCommentCount(songModel.getCommentCount());
            result = songModelRepository.saveAndFlush(result);
        }
        return result;
    }

    public WebPageModel update(WebPageModel WebPageModel) {
        return webPageModelRepository.save(WebPageModel);
    }

    public void reset() {
        webPageModelRepository.resetStatus(CrawledStatus.notCrawled);
    }

    public synchronized WebPageModel getUnCrawlPage() {

        WebPageModel webPageModel = webPageModelRepository.findTopByStatus(CrawledStatus.notCrawled);
        webPageModel.setStatus(CrawledStatus.crawled);
        update(webPageModel);
        return webPageModel;
    }

    private void init(String category) {
        String basePlayListUrl = "http://music.163.com/#/discover/playlist/?order=hot&cat=";

        String url = basePlayListUrl + category;
        PageType pageType = PageType.playLists;
        WebPageModel webPageModel = new WebPageModel(url, pageType, category);
        savePage(webPageModel);
    }

    @Async
    public void init() {
        webPageModelRepository.deleteAll();

        List<String> categoryList = new ArrayList<>(Arrays.asList(
                "全部", "华语", "欧美", "日语", "韩语", "粤语", "小语种", "流行",
                "摇滚", "民谣", "电子", "舞曲", "说唱", "轻音乐", "爵士", "乡村",
                "R&B/Soul", "古典", "民族", "英伦", "金属", "朋克", "蓝调",
                "雷鬼", "世界音乐", "拉丁", "另类/独立", "New Age", "古风",
                "后摇", "Bossa Nova", "清晨", "夜晚", "学习", "工作", "午休",
                "下午茶", "地铁", "驾车", "运动", "旅行", "散步", "酒吧", "怀旧",
                "清新", "浪漫", "性感", "伤感", "治愈", "放松", "孤独", "感动", "兴奋",
                "快乐", "安静", "思念", "影视原声", "ACG", "校园", "游戏", "70后",
                "80后", "90后", "网络歌曲", "KTV", "经典", "翻唱", "吉他", "钢琴",
                "器乐", "儿童", "榜单", "00后"));

        for (String title : categoryList) {
            init(title);
        }
    }

    @Async
    public void crawl() throws InterruptedException {
        //创建线程池，设置线程数。让 ExecutorService 中的某个线程执行这个 Runnable 线程。
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
        for(int i = 0; i < MAX_THREADS; i++) {
            executorService.execute(new CrawlerThread(this));
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        Ehcache ehcache = cacheManager.getEhcache(cacheName);
        ehcache.removeAll();
    }

    @Async
    public void update() throws InterruptedException {
        List<SongModel> WebPageModels = songModelRepository.findByCommentCountGreaterThan(5000L);
        WebPageModels.forEach(s -> {
            WebPageModel p = webPageModelRepository.findOne(s.getUrl());
            p.setStatus(CrawledStatus.notCrawled);
            webPageModelRepository.save(p);

        });
        crawl();
    }

}