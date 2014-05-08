package com.jr2jme.st;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.ObjectId;

import java.util.Date;

/**
 * Created by Hirotaka on 2014/03/23.
 */
public class WikiText {
    String title;
    Date date;
    String name;
    String text;
    String comment;
    int revid;
    int version;
    public WikiText(){//forjackson dummy

    }
    public WikiText(String titlea, Date datea, String namea, String texta, int id, String com, int version){
        title=titlea;
        date=datea;
        name=namea;
        text=texta;
        revid=id;
        comment=com;
        this.version=version;
    }
    public void setDate(Date date) {
        this.date = date;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle(){
        return title;
    }

    public Date getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRevid(int revid) {
        this.revid = revid;
    }

    public int getRevid() {
        return revid;
    }

    public String getComment() {
        return comment;
    }

    private String id;
    @ObjectId
    @JsonProperty("_id")
    public String getId() {
        return id;
    }
    @ObjectId
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }
}
