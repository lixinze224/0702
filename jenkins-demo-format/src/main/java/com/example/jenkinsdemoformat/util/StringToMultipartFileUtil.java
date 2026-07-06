package com.example.jenkinsdemoformat.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class StringToMultipartFileUtil {

    public static MultipartFile toMultipartFile(String content, String filename) {
        byte[] bytes = content.getBytes();
        return new ByteArrayMultipartFile(bytes, filename);
    }

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;

        public ByteArrayMultipartFile(byte[] content, String filename) {
            this.content = content;
            this.filename = filename;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            FileOutputStream fos = new FileOutputStream(dest);
            fos.write(content);
            fos.flush();
            fos.close();
        }
    }
}