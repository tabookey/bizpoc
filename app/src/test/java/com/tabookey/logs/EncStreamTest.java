package com.tabookey.logs;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class EncStreamTest {

    @Test
    public void testLogs() {
        Log.initLogger("/tmp", true);
        Log.d("tag", "this is a log line");
        Log.e("errtag", "err", new Throwable("here"));
        Log.v("some-verbose", "asdasd");
        //new filename
        Log.initLogger("/tmp", true);
        Log.e("errtag", "err", new Throwable("here"));

        Log.initLogger(null,true);


    }
    @Test
    public void test_log_10() throws IOException {
        checkLogSize(10);
    }

    @Test
    public void test_log_10000() throws IOException {
        checkLogSize(10000);
    }
    @Test
    public void test_log_60000() throws IOException {
        checkLogSize(60000);
    }

    void checkLogSize(int size) throws IOException {

        String test;
        {

            StringBuilder buf = new StringBuilder();
            for (int i = 0; buf.length()<size; i++)
                buf.append("log," + i);

            test = buf.toString().substring(0,size);
        }
        String tmpfile = "/tmp/testlog";
        PrintStream out = EncStream.createEncLog(tmpfile);

        out.print(test);
        out.close();

        long fsize = new File(tmpfile).length();
        System.out.println( "full size: "+size+" compressed: "+ fsize+" ratio: "+(fsize*100/size)+"%");

        String str ="";
        InputStream in = EncStream.openEncLog(tmpfile);
        byte[] buf = new byte[1000];
        int c;
        while ((c= in.read(buf))>0 ) {
            str=str+new String(buf,0,c);
        }

        in.close();
//        new File(tmpfile).delete();

        assertEquals(test.length(),str.length());
        assertEquals(test,str);
    }
}