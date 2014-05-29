package com.jr2jme.st;

import com.jr2jme.doc.DeletedTerms;
import com.jr2jme.doc.WhoWrite;
import com.jr2jme.wikidiff.Levenshtein3;
import com.jr2jme.wikidiff.WhoWriteResult;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import net.java.sen.SenFactory;
import net.java.sen.StringTagger;
import net.java.sen.dictionary.Token;
import net.java.sen.filter.stream.CompositeTokenFilter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stmain {
    //static JacksonDBCollection<WhoWrite,String> coll2;
    //static DBCollection dbCollection3;
    //static DBCollection dbCollection4;
    public static void main(String[] args) {
        Set<String> AimingArticle = fileRead("input.txt");
        //System.out.println(args[0]);
        MongoClient mongo = null;
        try {
            mongo = new MongoClient("dragons.db.ss.is.nagoya-u.ac.jp", 27017);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        assert mongo != null;
        DB db = mongo.getDB("kondou_revDIFF");
        //DBCollection dbCollection = db.getCollection("wikitext");
        //DBCollection dbCollection2=db.getCollection("Editor_Term_Islam");
        //dbCollection3=db.getCollection("InsertedTerms_Islam");
        //dbCollection4=db.getCollection("DeletedTerms_Islam");
        DBCollection dbCollection5=db.getCollection("Revert");

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = null;
        BufferedInputStream stream = null;

        try {
            reader = factory.createXMLStreamReader(UnBzip2.unbzip2(args[0]));
            // 4. イベントループ
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Boolean inrev = false;
            Boolean incon = false;
            String comment = "";
            String title = "";
            String name = "";
            Date date = null;
            String text;

            int version = 0;
            int id = 0;
            Boolean isAimingArticle = false;
            assert reader != null;
            List<String> prev_text = new ArrayList<String>();
            int tail=0;
            int head;
            List<WhoWrite> prevdata = null;
            WhoWriteResult[] resultsarray= new WhoWriteResult[20];
            while(reader.hasNext()){
                // 4.1 次のイベントを取得
                int eventType=reader.next();
                // 4.2 イベントが要素の開始であれば、名前を出力する
                if (eventType == XMLStreamReader.START_ELEMENT) {
                    if ("title".equals(reader.getName().getLocalPart())) {
                        //System.out.println(reader.getElementText());
                        title = reader.getElementText();
                        //System.out.println(title);
                        if (AimingArticle.contains(title)) {
                            isAimingArticle = true;
                            version = 0;
                            prevdata = null;
                            tail=0;
                            prev_text = new ArrayList<String>();
                            resultsarray= new WhoWriteResult[20];
                            //System.out.println(reader.getElementText());
                        } else {
                            //System.out.println(reader.getElementText());
                            isAimingArticle = false;
                        }

                    }
                    if (isAimingArticle) {
                        if ("revision".equals(reader.getName().getLocalPart())) {
                            inrev = true;
                        }
                        if ("id".equals(reader.getName().getLocalPart())) {
                            if (inrev && !incon) {
                                id = Integer.valueOf(reader.getElementText());
                            }
                        }
                        if ("comment".equals(reader.getName().getLocalPart())) {
                            comment = reader.getElementText();

                        }
                        if ("timestamp".equals(reader.getName().getLocalPart())) {
                            //System.out.println(reader.getElementText());
                            try {
                                date = sdf.parse(reader.getElementText());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        if ("ip".equals(reader.getName().getLocalPart())) {
                            //System.out.println(reader.getElementText());
                            name = reader.getElementText();
                            incon = true;
                        }
                        if ("username".equals(reader.getName().getLocalPart())) {
                            //System.out.println(reader.getElementText());
                            name = reader.getElementText();
                            incon = true;
                        }
                        if ("text".equals(reader.getName().getLocalPart())) {
                            //System.out.println(reader.getElementText());
                            version++;
                            text = reader.getElementText();
                            StringTagger tagger = SenFactory.getStringTagger(null);
                            CompositeTokenFilter ctFilter = new CompositeTokenFilter();

                            ctFilter.readRules(new BufferedReader(new StringReader("名詞-数")));
                            tagger.addFilter(ctFilter);

                            ctFilter.readRules(new BufferedReader(new StringReader("記号-アルファベット")));
                            tagger.addFilter(ctFilter);
                            List<Token> tokens = new ArrayList<Token>();
                            try {
                                tokens=tagger.analyze(text, tokens);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            List<String> current_text = new ArrayList<String>(tokens.size()+1);
                            for(Token token:tokens){
                                String regex = "^[ -/:-@\\[-\\`\\{-\\~！”＃＄％＆’（）＝～｜‘｛＋＊｝＜＞？＿－＾￥＠「；：」、。・]+$";
                                Pattern p1 = Pattern.compile(regex);
                                Matcher m = p1.matcher(token.getSurface());
                                if(!m.find()) {
                                    current_text.add(token.getSurface());
                                }
                            }
                            //System.out.println(title+date+name+text+id+comment);
                            Levenshtein3 d = new Levenshtein3();
                            List<String> diff = d.diff(prev_text, current_text);
                            WhoWriteResult now=whowrite(title,name,prevdata,current_text,prev_text,diff,version);

                            int last;
                            if(tail>=20){
                                last=20;
                                head=tail+1;
                            }
                            else{
                                last=tail;
                                head=0;
                            }

                            List<String> edrvted=new ArrayList<String>();
                            List<Integer> rvted=new ArrayList<Integer>();
                            boolean isnotflag = true;
                            /*for(int ccc=last-2;ccc>=0;ccc--){
                                int index=(head+ccc)%20;
                                if(!resultsarray[index].isreverted()) {
                                    if (now.getText().equals(resultsarray[index].getText())) {//完全に戻していた場合
                                        int indext = 0;
                                        for (WhoWrite who : now.getWhoWritever().getWhowritelist()) {
                                            who.setEditor(resultsarray[index].getWhoWritever().getWhowritelist().get(indext).getEditor());
                                            indext++;
                                        }
                                        for (int cou = ccc + 1; cou < last; cou++) {
                                            int idx = (head + cou) % 20;
                                            rvted.add(resultsarray[idx].getInsertedTerms().getVersion());
                                            edrvted.add(resultsarray[idx].getInsertedTerms().getEditor());
                                        } //dbCollection5.insert(obj);
                                        isnotflag = false;
                                        resultsarray[index].reverted();
                                        break;
                                    }
                                }
                            }*/
                            if(isnotflag) {
                                for (int ccc = last - 1; ccc >= 0; ccc--) {//リバート検知
                                    int index = (head + ccc) % 20;
                                    if(!resultsarray[index].isreverted()) {
                                        if (now.contain(resultsarray[index])) {
                                            //System.out.println(now.version+":"+resultsarray[index].version);
                                            int dd = 0;
                                            int ad = 0;
                                            for (String type : diff) {
                                                if (type.equals("+")) {
                                                    //System.out.println(now.getInsertedTerms().getTerms().get(dd));
                                                    WhoWrite who = now.getWhoWritever().getWhowritelist().get(ad);
                                                    if (resultsarray[index].getDellist().size() <= dd) {
                                                        break;
                                                    }
                                                    if (who.getTerm().equals(resultsarray[index].getDelwordcount().get(dd))) {
                                                        who.setEditor(resultsarray[index].getDellist().get(dd));
                                                        dd++;

                                                    }
                                                    ad++;
                                                } else if (type.equals("|")) {
                                                    ad++;
                                                }
                                            }
                                            resultsarray[index].reverted();
                                            edrvted.add(resultsarray[index].getInsertedTerms().getEditor());
                                            rvted.add(resultsarray[index].getInsertedTerms().getVersion());
                                        }
                                    }
                                }
                            }
                            if(!edrvted.isEmpty()) {
                                BasicDBObject obj = new BasicDBObject();
                                obj.append("title", title).append("version", version).append("editor", name).append("rvted", rvted).append("edrvted", edrvted);
                                dbCollection5.insert(obj);
                            }
                            //BasicDBObject wikitext = new BasicDBObject();
                            //wikitext.append("title", title).append("name", name).append("date", name).append("revison", id).append("text", text).append("comment",comment).append("version",version);
                            //dbCollection.insert(wikitext);
                            //coll.insert(new WikiText(title, date, name, text, id, comment, version));
                            resultsarray[tail%20]=now;
                            tail++;
                            //coll2.insert(now.getWhoWritever().getWhowritelist());//ここは20140423現在使う あまりよくない
                            prevdata=now.getWhoWritever().getWhowritelist();
                            prev_text=current_text;

                            inrev = false;
                            incon = false;

                        }

                    }

                    //System.out.println(reader.getName().getLocalPart());
                }
            }
        } catch (IOException ex) {
            System.err.println(ex + " が見つかりません");
        } catch (XMLStreamException ex) {
            System.err.println(ex + " の読み込みに失敗しました");
        } finally {
            // 5. パーサ、ストリームのクローズ
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ex) {
                    ex.printStackTrace();
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            mongo.close();
        }
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        System.out.println(sdf1.format(new Date(now)));
    }

    private static WhoWriteResult whowrite(String title,String currenteditor,List<WhoWrite> prevdata,List<String> text,List<String> prevtext,List<String> delta,int ver) {//誰がどこを書いたか
        int a = 0;//この関数が一番重要
        int b = 0;
        WhoWriteResult whowrite = new WhoWriteResult(title, text, currenteditor, ver);
        for (String aDelta : delta) {//順番に見て，単語が残ったか追加されたかから，誰がどこ書いたか
            //System.out.println(delta.get(x));
            if (aDelta.equals("+")) {
                //System.out.println(text.get(a));
                whowrite.addaddterm(text.get(a));
                a++;
            } else if (aDelta.equals("-")) {
                whowrite.adddelterm(prevdata.get(b).getEditor(), prevtext.get(b));
                b++;
            } else if (aDelta.equals("|")) {
                //System.out.println(prevdata.getText_editor().get(b).getTerm());
                whowrite.remain(prevdata.get(b).getEditor(), text.get(a));
                a++;
                b++;
            }
        }
        whowrite.complete(prevdata);
        if(whowrite.getInsertedTerms().getTerms().size()!=0) {
            BasicDBObject obj = new BasicDBObject();
            obj.append("title", title).append("editor", whowrite.getInsertedTerms().getEditor()).append("terms", whowrite.getInsertedTerms().getTerms()).append("version", whowrite.getInsertedTerms().getVersion());
            //dbCollection3.insert(obj);
            //coll3.insert(whowrite.getInsertedTerms());
        }
        if(whowrite.getDeletedTerms().values().size()!=0) {
            for (DeletedTerms de : whowrite.getDeletedTerms().values()) {
                BasicDBObject obj = new BasicDBObject();
                obj.append("title", title).append("editorFrom", de.getEditorFrom()).append("editorTo", de.getEditorTo()).append("version", whowrite.getInsertedTerms().getVersion()).append("terms", de.getTerms()).append("total", de.getTotal()).append("delnum", de.getDelnum());
                //dbCollection3.insert(obj);
            }
        }
        return whowrite;



    }

    public static Set<String> fileRead(String filePath) {
        FileReader fr = null;
        BufferedReader br = null;
        Set<String> AimingArticle=new HashSet<String>();
        try {
            fr = new FileReader(filePath);
            br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                AimingArticle.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return AimingArticle;
    }
}


