package cn.lwf.framework.train.task;

import cn.lwf.framework.train.service.TrainFahrplanService;
import cn.lwf.framework.train.service.TrainService;
import cn.lwf.framework.train.service.TrainStationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@Component
@Configurable
@EnableScheduling
@Slf4j
public class TrainInitTask{

	@Autowired
	TrainService trainService;

    @Autowired
	TrainStationService trainStationService;

	@Autowired
	TrainFahrplanService trainFahrplanService;

	/**
	 *  version：1.0版本,2019.12
	 *  TO:项目启动执行一次，每次js执行只拉取30天数据（获取旧车次列表信息）
	 */
	@PostConstruct
	public void initTrainList() {
	    log.info("执行车次列表数据 - start");
		trainService.analysisTrainList();
        log.info("执行车次列表数据 - end");
	}

    /**
     *  version：2.0版本,2020.08
     *  TO:项目启动执行一次（获取全路客运车站）
     */
	@PostConstruct
    public void initTrainStation() {
        log.info("执行全路客运车站信息数据 - start");
        trainStationService.analysisStationInfo();
        log.info("执行全路客运车站信息数据 - end");
    }

	/**
	 *  version：2.0版本,2020.08
	 *  TO:每天凌晨0点执行（自动同步车次和车次时刻信息）
     *  建议：可采用手动获取，自动频繁获取可能会12306被拉黑
	 */
	 //@Scheduled(cron = "0 0 0 * * *")
	 @Scheduled(cron = "0 0 */3 * * ?")
	 public void syncEverydayTrainList() {
         log.info("每天执行车次列表数据更新 - start");
	 	 //1、同步今日车次
		 trainService.syncTrainListByStation();
         //2、更新完，同步车次时刻信息
		 trainFahrplanService.autoSyncTrainFahrplan();
         log.info("每天执行车次列表数据更新 - start");
	 }

}
