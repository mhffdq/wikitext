package com.jr2jme.st;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.*;

/**
 * Created by Hirotaka on 2014/04/04.
 */
public class UnBzip2 {
    public static BZip2CompressorInputStream unbzip2() throws IOException {
        FileInputStream in = new FileInputStream("jawiki-latest-pages-meta-history3.xml.bz2");
        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
        return bzIn;
    }
}
