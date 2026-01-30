package com.heima.utils.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.*;

/**
 * 字符串压缩
 */
public class ZipUtils {

    /**
     * 使用 gzip 进行压缩
     */
    public static String gzip(String primStr) {
        if (primStr == null || primStr.length() == 0) {
            return primStr;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {

            gzip.write(primStr.getBytes(StandardCharsets.UTF_8));
            gzip.finish();

            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 使用 gzip 进行解压缩
     */
    public static String gunzip(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }

        byte[] compressed = Base64.getDecoder().decode(compressedStr);

        try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
             GZIPInputStream ginzip = new GZIPInputStream(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int offset;
            while ((offset = ginzip.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            return out.toString(StandardCharsets.UTF_8.name());

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 使用 zip 进行压缩
     */
    public static String zip(String str) {
        if (str == null) {
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zout = new ZipOutputStream(out)) {

            zout.putNextEntry(new ZipEntry("0"));
            zout.write(str.getBytes(StandardCharsets.UTF_8));
            zout.closeEntry();

            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 使用 zip 进行解压缩
     */
    public static String unzip(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }

        byte[] compressed = Base64.getDecoder().decode(compressedStr);

        try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
             ZipInputStream zin = new ZipInputStream(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            zin.getNextEntry();
            byte[] buffer = new byte[1024];
            int offset;
            while ((offset = zin.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            return out.toString(StandardCharsets.UTF_8.name());

        } catch (IOException e) {
            return null;
        }
    }
}
