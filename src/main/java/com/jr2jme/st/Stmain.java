package com.jr2jme.st;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.mongojack.JacksonDBCollection;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class Stmain {


    public static void main(String[] args) {

        Set<String> AimingArticle = new HashSet<String>(350);
        fileRead("イスラム.txt", AimingArticle);
        //System.out.println(args[0]);
        MongoClient mongo = null;
        try {
            mongo = new MongoClient("dragons.db.ss.is.nagoya-u.ac.jp", 27017);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        assert mongo != null;
        DB db = mongo.getDB("wikipediaDB_kondou");
        DBCollection dbCollection = db.getCollection("wikitext_Islam");
        JacksonDBCollection<Wikitext, String> coll = JacksonDBCollection.wrap(dbCollection, Wikitext.class, String.class);
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
            while (reader.hasNext()) {
                // 4.1 次のイベントを取得
                int eventType = reader.next();
                // 4.2 イベントが要素の開始であれば、名前を出力する
                if (eventType == XMLStreamReader.START_ELEMENT) {
                    if ("title".equals(reader.getName().getLocalPart())) {
                        //System.out.println(reader.getElementText());
                        version = 0;
                        title = reader.getElementText();
                        if (AimingArticle.contains(title)) {
                            isAimingArticle = true;
                        } else {
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
                            //System.out.println(title+date+name+text+id+comment);
                            coll.insert(new Wikitext(title, date, name, text, id, comment, version));
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

    public static void fileRead(String filePath, Set<String> aiming) {
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(filePath);
            br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                aiming.add(line);
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
    }
}
