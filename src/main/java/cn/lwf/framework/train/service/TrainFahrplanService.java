package cn.lwf.framework.train.service;

import cn.lwf.framework.constant.Constants;
import cn.lwf.framework.model.Train;
import cn.lwf.framework.model.TrainFahrplan;
import cn.lwf.framework.model.TrainStation;
import cn.lwf.framework.train.dao.TrainDao;
import cn.lwf.framework.train.dao.TrainFahrplanDao;
import cn.lwf.framework.train.dao.TrainStationDao;
import cn.lwf.framework.train.gateway.TrainSyncGateway;
import cn.lwf.framework.util.HttpClientUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class TrainFahrplanService {

    @Autowired
    TrainFahrplanDao trainFahrplanDao;

    @Autowired
    TrainStationDao trainStationDao;

    @Autowired
    TrainDao trainDao;

    @Autowired
    TrainSyncGateway trainSyncGateway;

    /**
     * 自动同步车次时刻<由定时任务调取>
     */
    public void autoSyncTrainFahrplan() {
        try{
            //所有车次数据遍历
            /**
            List<Train> list =  trainDao.findTrainList();
            for (Train train : list) {
                trainSyncGateway.syncTrainFahrplan(train.getTrainCode());
            }
            **/
            //所有车次数据遍历（过滤冻结的车次）
            List<TrainFahrplan> list =  trainFahrplanDao.findFahrplanList();
            for (TrainFahrplan fahrplan : list) {
                trainSyncGateway.syncTrainFahrplan(fahrplan.getTrainCode());
            }
        }catch (Exception e){
            log.error("自动同步车次时刻信息数据异常：{}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 手动同步车次时刻<可通过前端请求接口到方法>
     */
    public void manualSyncTrainFahrplan(String trainCode) {
        try{
            trainSyncGateway.syncTrainFahrplan(trainCode);
        }catch (Exception e){
            log.error("自动同步车次时刻信息数据异常：{}", e.getMessage());
            e.printStackTrace();
        }
    }
}
