package com.heima.es.pojo;


import java.util.Date;

public class SearchArticleVo {

    // 文章id
    private Long id;
    // 文章标题
    private String title;


    // 文章发布时间
    private Date publishTime;
    // 文章布局
    private Integer layout;
    // 封面
    private String images;
    // 作者id
    private Long authorId;
    // 作者名词
    private String authorName;
    //静态url
    private String staticUrl;
    //文章内容
    private String content;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public Integer getLayout() {
        return layout;
    }

    public String getImages() {
        return images;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getStaticUrl() {
        return staticUrl;
    }

    public String getContent() {
        return content;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public void setLayout(Integer layout) {
        this.layout = layout;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setStaticUrl(String staticUrl) {
        this.staticUrl = staticUrl;
    }

    public void setContent(String content) {
        this.content = content;
    }
}