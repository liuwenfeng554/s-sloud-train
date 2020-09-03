package cn.lwf.framework.train;

import cn.lwf.framework.train.service.TrainFahrplanService;
import cn.lwf.framework.train.service.TrainService;
import cn.lwf.framework.train.service.TrainStationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SCloudTrainApplicationTests {

    @Autowired
    TrainService trainService;

    @Autowired
    TrainStationService trainStationService;

    @Autowired
    TrainFahrplanService trainFahrplanService;

    @Test
    void contextLoads() {
        //trainService.analysisTrainList();
        //trainStationService.analysisStationInfo();
        //trainService.syncTrainListByStation();
        trainFahrplanService.autoSyncTrainFahrplan();
    }

}
