package com.tabookey.logs;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class cmd {
    public static final void main(String args[]) throws IOException {
        String cmd = args[0];
        String file = args[1];
        if (cmd.equals("write")) {
            PrintStream out = EncStream.createEncLog(file);
            InputStream in;
            if (args[2].equals('-'))
                in = System.in;
            else
                in = new FileInputStream(args[2]);

            byte[] b = new byte[100000];
            int c = in.read(b);
            System.out.println("reading: " + c + " bytes");
            out.write(b, 0, c);
            out.flush();
            out.close();
        } else if (cmd.equals("read")) {
            InputStream r = EncStream.openEncLog(file);
            String s = readString(r);
            System.out.println(s);
        } else if (cmd.equals("zip")) {
            ZipFile f = new ZipFile(file);
            for (ZipEntry e : Collections.list(f.entries())) {
                System.out.println("=== " + e.getName());
                if ( !e.getName().endsWith("log")) {
                    //dump as plain text the appinfo
                    System.out.println(readString(f.getInputStream(e)));
                    continue;
                }
                if ( e.getSize()<10 )
                    continue;
                System.out.println(readString(EncStream.openEncLog(f.getInputStream(e))));
            }
        }
    }

    @NotNull
    private static String readString(InputStream r) throws IOException {
        StringBuilder s = new StringBuilder();
        byte[] b = new byte[4096];
        int c;
        try {
            while ((c = r.read(b)) > 0) {
                s.append(new String(b, 0, c));
            }
        } catch (IOException bpe) {
            //ignore bad padding: the last block is might be not fully written
            if (bpe.toString().indexOf("BadPadding") == -1)
                throw bpe; //its another error.
        }
        return s.toString();
    }
}
