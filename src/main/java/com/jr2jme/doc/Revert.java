package com.jr2jme.doc;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.ObjectId;

import java.util.List;

/**
 * Created by JR2JME on 2014/05/02.
 */
public class Revert {
    String title;
    int rving;
    List<Integer> rvted;
    String ed;
    List<String> edrvted;

    public Revert(String title,int rving,List<Integer> rvted,String ed,List<String> edrvted){
        this.title=title;
        this.rving=rving;
        this.rvted=rvted;
        this.ed=ed;
        this.edrvted=edrvted;

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
