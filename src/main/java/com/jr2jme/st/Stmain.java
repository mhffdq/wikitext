package com.jr2jme.st;

import com.jr2jme.doc.DeletedTerms;
import com.jr2jme.doc.InsertedTerms;
import com.jr2jme.doc.WhoWrite;
import com.jr2jme.wikidiff.Levenshtein3;
import com.jr2jme.wikidiff.WhoWriteResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import net.java.sen.SenFactory;
import net.java.sen.StringTagger;
import net.java.sen.dictionary.Token;
import org.mongojack.JacksonDBCollection;

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
    static JacksonDBCollection<WhoWrite,String> coll2;
    static JacksonDBCollection<InsertedTerms,String> coll3;//insert
    static JacksonDBCollection<DeletedTerms,String> coll4;//del&
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
        DB db = mongo.getDB("wikipediaDB_kondou");
        DBCollection dbCollection = db.getCollection("wikitext_Test");
        JacksonDBCollection<Wikitext, String> coll = JacksonDBCollection.wrap(dbCollection, Wikitext.class, String.class);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        DBCollection dbCollection2=db.getCollection("Editor_Term_Test");
        DBCollection dbCollection3=db.getCollection("InsertedTerms_Test");
        DBCollection dbCollection4=db.getCollection("DeletedTerms_Test");
        coll2 = JacksonDBCollection.wrap(dbCollection2, WhoWrite.class,String.class);
        coll3 = JacksonDBCollection.wrap(dbCollection3, InsertedTerms.class,String.class);
        coll4 = JacksonDBCollection.wrap(dbCollection4, DeletedTerms.class,String.class);

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
                        //System.out.println(reader.getElementText());
                        title = reader.getElementText();//2回getElementText()やっちゃだめ
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
                            List<Token> tokens = new ArrayList<Token>();
                            try {
                                tagger.analyze(text, tokens);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            List<String> current_text = new ArrayList<String>(tokens.size()+1);

                            for(Token token:tokens){
                                String regex = "^[ -/:-@\\[-\\`\\{-\\~]+$";
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
                            for(int ccc=0;ccc<last;ccc++){//リバート検知
                                int index=(head+ccc)%20;
                                if(now.compare(resultsarray[index])){
                                    //System.out.println(now.version+":"+resultsarray[index].version);
                                    int dd=0;
                                    int ad=0;
                                    for(String type:diff){
                                        if(type.equals("+")){
                                            System.out.println(now.getInsertedTerms().getTerms().get(dd));
                                            now.getWhoWritever().getWhowritelist().get(ad).setEditor(resultsarray[index].getDellist().get(dd));
                                            //now.whoWrite.getEditors().set(ad,resultsarray[ccc].dellist.get(dd));
                                            dd++;
                                            ad++;
                                        }
                                        else if(type.equals("|")){
                                            ad++;
                                        }
                                    }
                                    //now=whowrite(current_editor,prevdata,text,prevtext,delta,offset+ver+1)
                                    break;
                                }
                                if(now.comparehash(resultsarray[ccc].getText())){//完全に戻していた場合
                                    int indext=0;
                                    for(WhoWrite who:now.getWhoWritever().getWhowritelist()){
                                        who.setEditor(resultsarray[ccc].getWhoWritever().getWhowritelist().get(indext).getEditor());
                                        indext++;
                                    }
                                    break;
                                }
                            }
                            //coll.insert(new Wikitext(title, date, name, text, id, comment, version));なくても問題ないけどあると確認しやすいやつ
                            resultsarray[tail%20]=now;
                            tail++;
                            coll2.insert(now.getWhoWritever().getWhowritelist());//ここは20140423現在使う
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
        coll3.insert(whowrite.getInsertedTerms());
        for (DeletedTerms de : whowrite.getDeletedTerms().values()){
            coll4.insert(de);
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
                System.out.println(line);
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


