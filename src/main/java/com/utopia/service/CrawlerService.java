package com.utopia.service;

import com.utopia.model.SongModel;
import com.utopia.model.WebPageModel;
import com.utopia.repository.SongRepository;
import com.utopia.repository.WebPageRepository;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

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
    private final String cacheName = "com.utopia.Songs";
    //最大线程数
    private static final Integer MAX_THREADS = 20;
    @Autowired
    SongRepository songRepository;
    @Autowired
    WebPageRepository webPageRepository;

    public CrawlerService(){
        cacheManager = CacheManager.getInstance();
    }

    public WebPageModel savePage(WebPageModel webPageModel){
        WebPageModel result = webPageRepository.findOne(webPageModel.getUrl());
        return result == null ? webPageRepository.saveAndFlush(webPageModel) : result;
    }

    public SongModel saveSong(SongModel songModel){
        SongModel result = songRepository.findOne(songModel.getUrl());
        if (result == null){
            result = songRepository.saveAndFlush(songModel);
        } else {
            result.setCommentCount(songModel.getCommentCount());
            result = songRepository.saveAndFlush(result);
        }
        return result;
    }

    public WebPageModel update(WebPageModel webPageModel) {
        return webPageRepository.save(webPageModel);
    }

    public void reset() {
        webPageRepository.resetStatus(WebPageModel.CrawledStatus.notCrawled);
    }

    public synchronized WebPageModel getUnCrawlPage() {
        // 返回未爬页面
        return null;
    }

    private void init(String catalog) {
        // 根据catalog初始化爬虫队列
    }

    @Async
    public void init() {
        webPageRepository.deleteAll();
        init("全部");
        init("华语");
        init("欧美");
        init("日语");
        init("韩语");
        init("粤语");
        init("小语种");
        init("流行");
        init("摇滚");
        init("民谣");
        init("电子");
        init("舞曲");
        init("说唱");
        init("轻音乐");
        init("爵士");
        init("乡村");
        init("R&B/Soul");
        init("古典");
        init("民族");
        init("英伦");
        init("金属");
        init("朋克");
        init("蓝调");
        init("雷鬼");
        init("世界音乐");
        init("拉丁");
        init("另类/独立");
        init("New Age");
        init("古风");
        init("后摇");
        init("Bossa Nova");
        init("清晨");
        init("夜晚");
        init("学习");
        init("工作");
        init("午休");
        init("下午茶");
        init("地铁");
        init("驾车");
        init("运动");
        init("旅行");
        init("散步");
        init("酒吧");
        init("怀旧");
        init("清新");
        init("浪漫");
        init("性感");
        init("伤感");
        init("治愈");
        init("放松");
        init("孤独");
        init("感动");
        init("兴奋");
        init("快乐");
        init("安静");
        init("思念");
        init("影视原声");
        init("ACG");
        init("校园");
        init("游戏");
        init("70后");
        init("80后");
        init("90后");
        init("网络歌曲");
        init("KTV");
        init("经典");
        init("翻唱");
        init("吉他");
        init("钢琴");
        init("器乐");
        init("儿童");
        init("榜单");
        init("00后");
    }

    @Async
    public void crawl() throws InterruptedException {
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
        List<SongModel> webPageModels = songRepository.findByCommentCountGreaterThan(5000L);
        webPageModels.forEach(s -> {
            WebPageModel p = webPageRepository.findOne(s.getUrl());
            p.setStatus(WebPageModel.CrawledStatus.notCrawled);
            webPageRepository.save(p);
        });
        crawl();
    }

}