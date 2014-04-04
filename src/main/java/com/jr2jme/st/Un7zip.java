package com.jr2jme.st;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.IOException;

/**
 * Created by Hirotaka on 2014/04/04.
 */
public class Un7zip {
    public static void Un7zip() {
        SevenZFile sevenZFile = null;
        SevenZArchiveEntry entry = null;
        try {
            sevenZFile = new SevenZFile(new File("archive.7z"));
            entry = sevenZFile.getNextEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*byte[] content = new byte[entry.getSize()];
        while (entry.) {
            sevenZFile.read(content, offset, content.length - offset);
        }*/
    }
}
