package com.joker.reggie.controller;

import com.joker.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.path}")
    private String BASE_PATH;

    /**
     * 文件上传
     * 这里注意参数名必须为file或者使用@RequestParam注解才能使用其他名称
     * 且该文件是一个临时文件，需要转存到指定位置，否则本次请求完成之后该文件就会被删除
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        log.info("执行文件上传操作。。。");

        // 1、获取原文件名称
        String originalFilename = file.getOriginalFilename();

        // 2、截取原文件名的后缀名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 3、防止文件名重复，使用UUID生成文件名
        String fileName = UUID.randomUUID().toString();

        // 4、生成最终文件名
        String finalFileName = fileName + suffix;

        // 5、判断当前目录下是否有该目录
        File dir = new File(BASE_PATH);
        if (!dir.exists()){
            // 不存在，则创建该目录
            dir.mkdirs();
        }

        // 转储到指定目录下
        try {
            file.transferTo(new File(BASE_PATH + finalFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return R.success(finalFileName);
    }

    /**
     * 文件下载并回显到浏览器
     * @param name
     * @return
     */
    @GetMapping("/download")
    public R<String> download(String name, HttpServletResponse response){
        log.info("执行文件下载操作。。。");
        FileInputStream fis = null;
        try {
            // 1、创建输入流对象，通过输入流读取文件内容
            fis = new FileInputStream(new File(BASE_PATH + name));

            // 2、创建输出流对象，通过输出流将文件写回浏览器，在浏览器中展示图片
            ServletOutputStream outputStream = response.getOutputStream();
            // 3、响应的文件类型是图片文件 固定格式
            response.setContentType("image/jpeg");

            int len = 0;
            byte[] bytes = new byte[1024];
            while((len = fis.read(bytes)) != -1){
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }

            // 4、关闭资源
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
