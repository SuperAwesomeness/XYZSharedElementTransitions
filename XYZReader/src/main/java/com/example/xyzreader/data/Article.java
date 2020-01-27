package com.example.xyzreader.data;

public class Article {

    private String imagePath;
    private float aspectRatio;
    private String title;
    private String byline;
    private String[] articleBody;

    public String[] getArticleBody() {
        return articleBody;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public void setArticleBody(String[] articleBody) {
        this.articleBody = articleBody;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getByline() {
        return byline;
    }

    public void setByline(String byline) {
        this.byline = byline;
    }

}
