package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用控制器
 */
@RestController
@Slf4j
@RequestMapping("/admin/common")
@ApiOperation("通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 上传文件
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public Result<String> upload(MultipartFile file){
        log.info("上传文件:{}",file);
        try {
            //获取上传文件的原始文件名
            String originalFilename = file.getOriginalFilename();
            //截取原始文件名的后缀png/jpg等
            String extension =  originalFilename.substring(originalFilename.lastIndexOf("."));
            //生成随机文件名
            String objectName = UUID.randomUUID().toString() + extension;
            //文件的请求路径
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("上传文件失败:{}",e);
        }
        return Result .error(MessageConstant.UPLOAD_FAILED);

    }
}
