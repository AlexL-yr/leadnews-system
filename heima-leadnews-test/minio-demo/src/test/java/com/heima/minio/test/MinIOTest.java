package com.heima.minio.test;

import com.heima.file.service.FileStorageService;
import com.heima.minio.MinIOApplication;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest(classes = MinIOApplication.class)
@RunWith(SpringRunner.class)
public class MinIOTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    public void test() throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("E:\\list.html");
        String path = fileStorageService.uploadHtmlFile("", "list.html", fileInputStream);
        System.out.println(path);
    }
    public static void main(String[] args) {

        try {
            FileInputStream fileInputStream = new FileInputStream("E:\\BaiduNetdiskDownload\\templates\\plugins\\css\\index.css");
            MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123")
                    .endpoint("http://192.168.200.130:9000").build();

            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object("plugins/css/index.css")
                    .contentType("text/css")
                    .bucket("leadnews")
                    .stream(fileInputStream, fileInputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);
//            System.out.println("http://192.168.200.130:9000/leadnews/list.html");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
