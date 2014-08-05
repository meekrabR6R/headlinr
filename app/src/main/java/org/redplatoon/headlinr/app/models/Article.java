package org.redplatoon.headlinr.app.models;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;

/**
 * Created by nmiano on 8/4/14 11:25 PM for Headlinr
 */
public class Article {
    private String type, source, title, link, description, pubDate;
    private boolean isInvalid;

    public Article() {
        this.isInvalid = false;
        this.type = "";
        this.source = "";
        this.title = "No title";
        this.link = "";
        this.description = "No summary";
        this.pubDate = "";
    }

    public void setIsInvalid(boolean isInvalid) {
        this.isInvalid = isInvalid;
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    public void setProperties(String source, String title, String link, String description, String pubDate) {
        this.source = source;
        this.title = title;
        this.link = link;
        this.description = description;
        this.pubDate = pubDate;
    }

    public String getSource() {
        return source;
    }

    public String getPubDate() {
        return pubDate;
    }

    public Spanned getTitle() {
        if(title != null)
            return Html.fromHtml(title);
        else
            return null;
    }

    public String getUpperCaseTitle() {
        return title.toUpperCase();
    }

    public String getLink() {
        if(link != null)
            link = link.trim();
        return link;
    }

    public Spanned getDescription() {
        String localDesc;
        if(description != null)
            localDesc = description.trim();
        else
            localDesc = "No summary";
        return removeImageSpanObjects(localDesc);
    }

    public String getType() {
        return type;
    }

    public void setMetaData(String description) {
        this.type = description;
    }

    public String getMetaData() {
        return type + " / " + source + "\n" + pubDate;
    }

    private Spanned removeImageSpanObjects(String inStr) {
        SpannableStringBuilder spannedStr = (SpannableStringBuilder) Html
                .fromHtml(inStr.trim());
        Object[] spannedObjects = spannedStr.getSpans(0, spannedStr.length(),
                Object.class);
        for (int i = 0; i < spannedObjects.length; i++) {
            if (spannedObjects[i] instanceof ImageSpan) {
                ImageSpan imageSpan = (ImageSpan) spannedObjects[i];
                spannedStr.replace(spannedStr.getSpanStart(imageSpan),
                        spannedStr.getSpanEnd(imageSpan), "");
            }
        }
        return spannedStr;
    }
}
