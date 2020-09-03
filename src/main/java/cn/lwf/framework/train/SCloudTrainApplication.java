package cn.lwf.framework.train;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAutoConfiguration //自动读取配置
@SpringBootApplication //springboot应用注解
@Slf4j
public class SCloudTrainApplication {

    public static void main(String[] args) {

        SpringApplication.run(SCloudTrainApplication.class, args);
        log.info("启动Spring boot服务成功");
    }

}
