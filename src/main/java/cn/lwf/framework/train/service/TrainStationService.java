package cn.lwf.framework.train.service;

import cn.lwf.framework.constant.Constants;
import cn.lwf.framework.model.TrainStation;
import cn.lwf.framework.train.dao.TrainDao;
import cn.lwf.framework.train.dao.TrainStationDao;
import cn.lwf.framework.util.HttpClientUtils;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class TrainStationService {

    @Autowired
    TrainStationDao trainStationDao;

    @Autowired
    TrainDao trainDao;

    /**
     *  version：1.0 2020版本
     *  通过js解析全路客运车站信息
     */
    @Transactional
    public void analysisStationInfo() {
        try{
            //获取全路客运车站信息
            String result = HttpClientUtils.httpGet(Constants.TRAIN_12306_STATION_JS);
            if(StringUtils.isBlank(result)){
                log.error("获取全路客运车站信息失败");
                return;
            }
            if(result.indexOf(Constants.TRAIN_STATION_JS_PREFIX_CHARACTER) == -1){
                log.error("解析失败，请确认是否官网改版");
                return;
            }
            // result.length() -1 去掉后面分号
            result = result.trim().replaceAll(Constants.TRAIN_STATION_JS_REGEX_TRIM,"");
            String str = result.substring(Constants.TRAIN_STATION_JS_PREFIX_CHARACTER.length(), result.length() - 1);
            String[] stations = str.split(Constants.TRAIN_STATION_JS_REGEX_SPLIT);
            log.info("全路客运车站信息总记录数：{}", stations.length - 1);
            List<TrainStation> list = new ArrayList<TrainStation>();
            for (String station: stations) {
                //过滤站点切割为空的数据
                if(StringUtils.isNotBlank(station)){
                    //根据格式解析以下存储：bjb|北京北|VAP|beijingbei|bjb|0
                    String[] array = station.split("\\|");
                    if(array.length < 6){
                        //检测格式和长度不对，直接终止
                        log.error("全路客运车站信息解析格式长度错误：{}" + station);
                        break;
                    }
                    TrainStation trainStation = new TrainStation();
                    trainStation.setId(Long.valueOf(array[5])); //ID
                    trainStation.setTelegraphCode(array[2]); //电报码
                    trainStation.setStationName(array[1]); //站名
                    trainStation.setSpell(array[3]); //拼音
                    trainStation.setInitial(array[0]); //首字母
                    trainStation.setPinyinCode(array[4]); //拼音码

                    list.add(trainStation);
                    log.info("正在添加站点 - {}", trainStation.getStationName());
                }
            }
            log.info("全路客运车站信息解析记录数：{}", list.size());
            //遍历插入或更新车站信息
            if(list.size() >0){
                for (TrainStation trainStation : list) {
                    //通过name字段检测出现重复站点名称不会插入，避免数据不完整插入改由id字段检测
                    //TrainStation station = trainStationDao.findStationByName(trainStation.getStationName());
                    TrainStation station = trainStationDao.findStationById(trainStation.getId());
                    if(station == null){
                        log.info("新增全路客运车站信息：{}", JSON.toJSONString(trainStation));
                        int passed = trainStationDao.addStation(trainStation);
                        if(passed > 0){
                            log.info("新增全路客运车站({})信息成功", trainStation.getStationName());
                        }else{
                            log.error("新增全路客运车站({})信息失败：{}", trainStation.getStationName(), JSON.toJSONString(trainStation));
                        }
                    }else{
                        log.info("更新全路客运车站信息：{}", JSON.toJSONString(trainStation));
                        int passed = trainStationDao.updateStation(trainStation);
                        if(passed > 0){
                            log.info("更新全路客运车站({})信息成功", trainStation.getStationName());
                        }else{
                            log.error("更新全路客运车站({})信息失败：{}", trainStation.getStationName(), JSON.toJSONString(trainStation));
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error("解析全路客运车站信息异常：{}", e.getMessage());
            e.printStackTrace();
        }
    }



}
