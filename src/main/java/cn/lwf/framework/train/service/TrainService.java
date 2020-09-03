package cn.lwf.framework.train.service;

import cn.lwf.framework.constant.Constants;
import cn.lwf.framework.model.Train;
import cn.lwf.framework.model.TrainStation;
import cn.lwf.framework.model.TrainFahrplan;
import cn.lwf.framework.train.dao.TrainDao;
import cn.lwf.framework.train.dao.TrainStationDao;
import cn.lwf.framework.train.dao.TrainFahrplanDao;
import cn.lwf.framework.util.HttpClientUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class TrainService {

    @Autowired
    TrainFahrplanDao trainFahrplanDao;

    @Autowired
    TrainStationDao trainStationDao;

    @Autowired
    TrainDao trainDao;

    /**
     *  version：1.0 2019之前版本
     *  通过js解析全路客运车站车次
     */
    @Transactional
    public void analysisTrainList() {
        try{
            //获取全路客运车站车次
            String result = HttpClientUtils.httpGet(Constants.TRAIN_12306_LIST_JS);
            if(StringUtils.isBlank(result)){
                log.error("获取全路客运车站车次失败");
                return;
            }
            String jsonStr = result.substring("var train_list =".length());
            JSONObject json = JSONObject.parseObject(jsonStr);
            int index=1;
            Map<String,Train> trainMap = new HashMap<>();
            Set keySet = json.keySet();
            log.info("train_list遍历总数：" + keySet.size());
            Iterator itt = keySet.iterator();
            while(itt.hasNext()){
                String date = (String)itt.next();
                //某一天的数据
                JSONObject dateJson = json.getJSONObject(date);
                Set keyset = dateJson.keySet();
                Iterator it = keyset.iterator();
                while(it.hasNext()){
                    String key=(String)it.next();
                    JSONArray jsonArray = dateJson.getJSONArray(key);
                    for(int i=0;i<jsonArray.size();i++){
                        //{"station_train_code":"D1(北京-沈阳)","train_no":"24000000D10V"}
                        JSONObject trainJson = jsonArray.getJSONObject(i);
                        String trainCode = trainJson.getString("station_train_code");
                        String trainNo = trainJson.getString("train_no");
                        //D1(北京-沈阳)
                        String code = trainCode.substring(0, trainCode.indexOf("("));
                        String startStation = trainCode.substring(trainCode.indexOf("(") + 1,trainCode.indexOf("-"));
                        String endStation = trainCode.substring(trainCode.indexOf("-") + 1,trainCode.indexOf(")"));

                        Train train = new Train();
                        train.setTrainCode(code);
                        train.setStartStationName(startStation);
                        train.setEndStationName(endStation);
                        train.setTrainNo(trainNo);
                        train.setRunDate(date);//运行时间

                        if(!trainMap.containsKey(code)){
                            trainMap.put(code, train);
                        }

                    }
                }
            }

            Iterator it = trainMap.keySet().iterator();
            while(it.hasNext()){
                Train train = trainMap.get(it.next());
                Train temp = trainDao.findTrainByCode(train.getTrainCode());
                if(temp==null){
                    int passed = trainDao.addTrain(train);
                    if(passed > 0){
                        log.info("新增全路客运车站车次({})成功",train.getTrainCode());
                        //插入时刻表
                        saveOrUpdateFahraplan(train);
                    }else{
                        log.error("新增全路客运车站车次({})失败：{}",train.getTrainCode(), JSON.toJSONString(train));
                    }
                }else{
                    temp.setStartStationName(train.getStartStationName());
                    temp.setEndStationName(train.getEndStationName());
                    temp.setTrainNo(train.getTrainNo());
                    temp.setRunDate(train.getRunDate());
                    int passed = trainDao.updateTrain(temp);
                    if(passed > 0){
                        log.info("更新全路客运车站车次({})成功",train.getTrainCode());
                    }else{
                        log.error("更新全路客运车站车次({})失败：{}",train.getTrainCode(), JSON.toJSONString(temp));
                    }
                }
            }
        }catch (Exception e){
            log.error("根据车站获取所有在该站办客的车次异常：{}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     *  version：1.0 2020版本
     *  根据车站获取所有在该站办客的车次
     */
    @Transactional
    public void syncTrainListByStation() {
        try{
            String runDate = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
            List<TrainStation> list = trainStationDao.findStationList();
            for (TrainStation station: list) {
                StringBuffer uri = new StringBuffer(Constants.TRAIN_12306_QUERY_LIST);
                uri.append("?train_start_date=").append(runDate);//当前时间
                uri.append("&train_station_code=").append(station.getTelegraphCode());
                log.info("根据车站获取所有在该站办客的车次请求地址：{}", uri.toString());
                //根据车站获取所有在该站办客的车次
                String resultString = HttpClientUtils.httpGet(uri.toString());
                if(StringUtils.isBlank(resultString)){
                    log.error("获取车站办客的车次响应失败：车站 - {}，电报码 - {}", station.getStationName(), station.getTelegraphCode());
                    continue;
                }
                JSONObject result = JSON.parseObject(resultString);
                if(result.getIntValue("httpstatus") != 200){
                    log.error("获取车站办客的车次数据失败：车站 - {}，电报码 - {}", station.getStationName(), station.getTelegraphCode());
                    continue;
                }
                JSONObject pdata = result.getJSONObject("data");
                if(pdata.isEmpty()){
                    log.error("获取车站办客的车次数据失败：车站 - {}，电报码 - {}", station.getStationName(), station.getTelegraphCode());
                    continue;
                }
                JSONArray items = pdata.getJSONArray("data");
                if(!pdata.getBoolean("flag") || items.size() <= 0){
                    log.error("获取车站办客的车次无数据，请核查电报码是否正确：车站 - {}，电报码 - {}", station.getStationName(), station.getTelegraphCode());
                    continue;
                }
                for (Object item: items) {
                    JSONObject data = (JSONObject) item;
                    //以下data参数可以根据实际需要的字段取值
                    String trainCode = data.getString("station_train_code");
                    String startStationName = data.getString("start_station_name");
                    String endStationName = data.getString("end_station_name");
                    String trainNo = data.getString("train_no");
                    Train train = trainDao.findTrainByCode(trainCode);
                    if(train == null){
                        train = new Train();
                        train.setTrainCode(trainCode); //车次',
                        train.setStartStationName(startStationName); //始发站',
                        train.setEndStationName(endStationName); //终点站',
                        train.setTrainNo(trainNo); //车次编码',
                        train.setRunDate(runDate); //运行时间',
                        log.info("新增车站办客的车次：{}", JSON.toJSONString(train));
                        int passed = trainDao.addTrain(train);
                        if(passed > 0){
                            log.info("新增车站办客的车次({})成功",train.getTrainCode());
                            //插入时刻表
                            saveOrUpdateFahraplan(train);
                        }else{
                            log.error("新增车站办客的车次({})失败：{}",train.getTrainCode(), JSON.toJSONString(train));
                        }
                    }else{
                        train.setStartStationName(startStationName); //始发站',
                        train.setEndStationName(endStationName); //终点站',
                        train.setTrainNo(trainNo); //车次编码',
                        train.setRunDate(runDate); //运行时间',
                        log.info("更新车站办客的车次新车站办客的车次：{}", JSON.toJSONString(train));
                        int passed = trainDao.updateTrain(train);
                        if(passed > 0){
                            log.info("更新车站办客的车次({})成功",train.getTrainCode());
                        }else{
                            log.error("更新车站办客的车次({})失败：{}",train.getTrainCode(), JSON.toJSONString(train));
                        }
                    }
                }
            }
            log.info("获取车站办客的车次完成");
        }catch (Exception e){
            log.error("获取车站办客的车次异常：{}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void saveOrUpdateFahraplan(Train train){
        TrainFahrplan trainFahrplan = trainFahrplanDao.findFahrplanByCode(train.getTrainCode());
        if(trainFahrplan == null){
            //新增时刻表
            trainFahrplan = new TrainFahrplan();
            trainFahrplan.setTrainCode(train.getTrainCode());
            trainFahrplan.setTrainNo(train.getTrainNo());
            trainFahrplanDao.addFahrplan(trainFahrplan);
        }else{
            //更新时刻表
            trainFahrplan.setTrainCode(train.getTrainCode());
            trainFahrplan.setTrainNo(train.getTrainNo());
            trainFahrplanDao.updateFahrplan(trainFahrplan);
        }
    }


}
