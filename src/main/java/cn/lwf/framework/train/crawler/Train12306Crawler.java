package cn.lwf.framework.train.crawler;

import cn.lwf.framework.constant.Constants;
import cn.lwf.framework.model.Train;
import cn.lwf.framework.model.TrainFahrplan;
import cn.lwf.framework.train.dao.TrainDao;
import cn.lwf.framework.train.mapper.TrainMapper;
import cn.lwf.framework.util.HttpClientUtils;
import cn.lwf.framework.vo.Node;
import cn.lwf.framework.vo.TrainInfoNew;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 12306 官网获取车次时刻表信息
 */
@Slf4j
@Component
public class Train12306Crawler {

    @Autowired
    TrainDao trainDao;

    public void get12306TrainInfo(TrainFahrplan fahrplan, String localTrainCode, String trainNo, String fromcode, String tocode, String date){
        JSONArray dataJsonArray = new JSONArray();
        try{
            StringBuffer uri = new StringBuffer(Constants.TRAIN_12306_QUERY_TRAIN_FAHRPLAN_BY_TRAINNO);
            uri.append("?train_no=").append(trainNo);
            uri.append("&from_station_telecode=").append(fromcode);
            uri.append("&to_station_telecode=").append(tocode);
            uri.append("&depart_date=").append(date);
            log.info("12306 官网时刻表获取请求地址：{}", uri.toString());
            //根据车次编码获取时刻表信息
            String resultString = HttpClientUtils.httpGet(uri.toString());
            if(StringUtils.isNotBlank(resultString)){
                JSONObject result = JSON.parseObject(resultString);
                if(!result.isEmpty() && (result.getIntValue("httpstatus") == 200 && result.containsKey("data"))){
                    JSONObject data = result.getJSONObject("data");
                    if(!data.isEmpty() && data.containsKey("data")){
                        dataJsonArray = data.getJSONArray("data");
                        int size = dataJsonArray.size();
                        //infoNew
                        TrainInfoNew infoNew = new TrainInfoNew();
                        ArrayList<Node> nodeList = new ArrayList<Node>();
                        infoNew.setItems(nodeList);
                        infoNew.setTrainLength(String.valueOf(size));
                        //info
                        Map<String,String> infomap = new HashMap<String,String>();
                        for(int index = 1;index <= size; index++){
                            JSONObject json = dataJsonArray.getJSONObject(index-1);
                            Node node = new Node();
                            String startTime = json.getString("start_time");
                            String arriveTime = json.getString("arrive_time");
                            String overTime = json.getString("stopover_time");
                            String stationName = json.getString("station_name");
//	                    String stationNo = json.getString("station_no");
                            if(index == 1){
                                String trainClassName = json.getString("train_class_name");
                                String trainCode = json.getString("station_train_code");
                                String startStationName = json.getString("start_station_name");
                                String endStationName = json.getString("end_station_name");

                                infoNew.setTrainCode(trainCode);
                                infoNew.setStartStationName(startStationName);
                                infoNew.setEndStationName(endStationName);
                                node.setArriveTime("----");
                                node.setOverTime("----");
                                node.setStartTime(startTime);
                                //info
                                infomap.put("trainClassName", trainClassName);
                                infomap.put("beginStation", stationName);
                                infomap.put("beginTime", startTime);
                                infomap.put("trainCode", trainCode);
                                infomap.put("endStation", endStationName);
                            }else{
                                //处理arriveTime
                                node.setArriveTime(arriveTime);
                                node.setStartTime(startTime);
                                node.setOverTime(overTime);
                            }
                            node.setStationName(stationName);
                            if(index < 10){
                                node.setStationNo("0"+index);
                            }else{
                                node.setStationNo(String.valueOf(index));
                            }
                            nodeList.add(node);
                            if(index == size){
                                node.setOverTime("----");
                                node.setStartTime(arriveTime);
                            }
                            infomap.put("no_"+String.valueOf(index-1)+"_stationName", stationName);
                            if(index < 10){
                                infomap.put("no_"+String.valueOf(index-1)+"_station_no", "0"+index);
                            }else{
                                infomap.put("no_"+String.valueOf(index-1)+"_station_no", index+"");
                            }
                            if(index == 1){
                                infomap.put("no_"+String.valueOf(index-1)+"_aTime", "----");
                                infomap.put("no_"+String.valueOf(index-1)+"_lTime", "----");
                                infomap.put("no_"+String.valueOf(index-1)+"_sTime", startTime);
                            }else{
                                infomap.put("no_"+String.valueOf(index-1)+"_aTime", arriveTime);
                                infomap.put("no_"+String.valueOf(index-1)+"_lTime", overTime);
                                infomap.put("no_"+String.valueOf(index-1)+"_sTime", startTime);
                            }
                            if(index == size){
                                infomap.put("no_"+String.valueOf(index-1)+"_lTime", "----");
                                infomap.put("no_"+String.valueOf(index-1)+"_sTime", arriveTime);
                            }
                        }
                        fahrplan.setTrainInfoNew(JSONObject.toJSONString(infoNew));
                        fahrplan.setTrainInfo(JSONObject.toJSONString(infomap));
                        log.info("12306 - 根据车次编码获取时刻表信息 --> infoNew：{}",JSONObject.toJSONString(infoNew));
                        log.info("12306 - 根据车次编码获取时刻表信息 --> info：{}",JSONObject.toJSONString(infomap));
                    }
                }
            }
        }catch(Exception ex){
            log.error("解析错误或无法爬取到数据");
            ex.printStackTrace();
        }
        //避免以上无法获取情况，补偿通过官方时刻表js方式先获取最新的trainNo再次查询
        if(!dataJsonArray.isEmpty() || dataJsonArray.size() == 0){
            //调用官网上爬取数据
            log.debug("12306 - 再次进行官网数据获取数据:" + localTrainCode);
            get12306TrainInfo(fahrplan, localTrainCode, DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
        }
    }

    /**
     *  采用通过官网爬取，官网地址：https://kyfw.12306.cn/otn/queryTrainInfo/init（原始12306接口数据2019年12月30之后未更新，去哪儿也查不到数据）
     * @param fahrplan
     * @param localTrainCode
     * @param date
     */
    public void get12306TrainInfo(TrainFahrplan fahrplan, String localTrainCode, String date){

        try{
            //步骤1、新增或更新车次列表信息
            StringBuffer train_uri = new StringBuffer(Constants.TRAIN_12306_QUERY_TRAINNO);
            train_uri.append("?keyword=").append(localTrainCode);
            train_uri.append("&date=").append(date.replaceAll("-", ""));
            log.info("12306 官网获取车次编码请求地址：{}", train_uri.toString());
            //根据车次编码获取时刻表信息
            String trainResultString = HttpClientUtils.httpGet(train_uri.toString());
            if(StringUtils.isBlank(trainResultString)) {
                return;
            }
            JSONObject trainResult = JSON.parseObject(trainResultString);
            if (trainResult.isEmpty()
                    || !trainResult.containsKey("data")
                    || trainResult.getIntValue("httpstatus") != 200) {
                return;
            }
            JSONArray trainArray = trainResult.getJSONArray("data");
            if(trainArray == null || trainArray.isEmpty()){
                return;
            }
            //该请求是模糊查询，则取第一个值即可
            //{"date":"20200708","from_station":"九江","station_train_code":"G1581","to_station":"常州北","total_num":"14","train_no":"57000G15810C"}
            JSONObject trainDateJson = trainArray.getJSONObject(0);
            Train train = new Train();
            train.setTrainCode(trainDateJson.getString("station_train_code"));
            train.setStartStationName(trainDateJson.getString("from_station"));
            train.setEndStationName(trainDateJson.getString("to_station"));
            train.setTrainNo(trainDateJson.getString("train_no"));
            String dateStr = trainDateJson.getString("date");
            if (StringUtils.isNotBlank(dateStr)) {
                dateStr = DateFormatUtils.format(DateUtils.parseDate(dateStr, "yyyyMMdd"), "yyyy-MM-dd");
            }
            train.setRunDate(dateStr);//运行时间
            //更新获取车次
            Train trainTemp = trainDao.findTrainByCode(train.getTrainCode());
            if(trainTemp == null){
                trainDao.addTrain(train);
                log.info("12306 - 新增车次 code：{},startStation：{},endStation：{}", train.getTrainCode(), train.getStartStationName(),train.getEndStationName() );
            }else{
                trainTemp.setStartStationName(train.getStartStationName());
                trainTemp.setEndStationName(train.getEndStationName());
                trainTemp.setTrainNo(train.getTrainNo());
                trainTemp.setRunDate(train.getRunDate());//运行时间
                trainDao.updateTrain(trainTemp);
            }

            //步骤2、根据获取到的车次编码读取车次时刻表信息
            StringBuffer fahrplan_uri = new StringBuffer(Constants.TRAIN_12306_QUERY_TRAIN_FAHRPLAN);
            fahrplan_uri.append("?leftTicketDTO.train_no=").append(train.getTrainNo());
            fahrplan_uri.append("&leftTicketDTO.train_date=").append(date);
            fahrplan_uri.append("&rand_code=");
            log.info("12306 官网获取时刻表信息请求地址：{}", fahrplan_uri.toString());
            //根据车次编码获取时刻表信息
            String fahrplanResultString = HttpClientUtils.httpGet(fahrplan_uri.toString());
            if(StringUtils.isBlank(fahrplanResultString)) {
                return;
            }
            JSONObject fahrplanResult = JSON.parseObject(fahrplanResultString);
            if (fahrplanResult.isEmpty()
                    || !fahrplanResult.containsKey("data")) {
                return;
            }

            JSONObject fahrplanData = fahrplanResult.getJSONObject("data");
            if (fahrplanData.isEmpty()
                    || !fahrplanData.containsKey("data")) {
                return;
            }
            JSONArray fahrplanArray = fahrplanData.getJSONArray("data");
            if(fahrplanArray == null || fahrplanArray.isEmpty()){
                return;
            }
            int size = fahrplanArray.size();
            //步骤3、解析车次时刻信息
            //infoNew
            TrainInfoNew infoNew = new TrainInfoNew();
            ArrayList<Node> nodeList = new ArrayList<Node>();
            infoNew.setItems(nodeList);
            infoNew.setTrainLength(String.valueOf(size));
            //info
            Map<String,String> infomap = new HashMap<String,String>();
            for(int index = 1; index <= size; index++){
                JSONObject json = fahrplanArray.getJSONObject(index-1);
                Node node = new Node();
                String startTime = json.getString("start_time");
                String arriveTime = json.getString("arrive_time");
                String overTime = json.getString("running_time");
                String stationName = json.getString("station_name");
//                String stationNo = json.getString"station_no");
                if(index==1){
                    String trainClassName = json.getString("train_class_name");
                    String trainCode = json.getString("station_train_code");
                    String startStationName = json.getString("start_station_name");
                    String endStationName = json.getString("end_station_name");

                    infoNew.setTrainCode(trainCode);
                    infoNew.setStartStationName(startStationName);
                    infoNew.setEndStationName(endStationName);
                    node.setArriveTime("----");
                    node.setOverTime("----");
                    node.setStartTime(startTime);
                    //info
                    infomap.put("trainClassName", trainClassName);
                    infomap.put("beginStation", stationName);
                    infomap.put("beginTime", startTime);
                    infomap.put("trainCode", trainCode);
                    infomap.put("endStation", endStationName);
                }else{
                    //处理arriveTime
                    node.setArriveTime(arriveTime);
                    node.setStartTime(startTime);
                    node.setOverTime(overTime);
                }
                node.setStationName(stationName);
                if(index<10){
                    node.setStationNo("0"+index);
                }else{
                    node.setStationNo(String.valueOf(index));
                }
                nodeList.add(node);
                if(index == size){
                    node.setOverTime("----");
                    node.setStartTime(arriveTime);
                }
                infomap.put("no_"+String.valueOf(index-1)+"_stationName", stationName);
                if(index<10){
                    infomap.put("no_"+String.valueOf(index-1)+"_station_no", "0"+index);
                }else{
                    infomap.put("no_"+String.valueOf(index-1)+"_station_no", index+"");
                }
                if(index==1){
                    infomap.put("no_"+String.valueOf(index-1)+"_aTime", "----");
                    infomap.put("no_"+String.valueOf(index-1)+"_lTime", "----");
                    infomap.put("no_"+String.valueOf(index-1)+"_sTime", startTime);
                }else{
                    infomap.put("no_"+String.valueOf(index-1)+"_aTime", arriveTime);
                    infomap.put("no_"+String.valueOf(index-1)+"_lTime", overTime);
                    infomap.put("no_"+String.valueOf(index-1)+"_sTime", startTime);
                }

                if(index == size){
                    infomap.put("no_"+String.valueOf(index-1)+"_lTime", "----");
                    infomap.put("no_"+String.valueOf(index-1)+"_sTime", arriveTime);
                }
            }
            fahrplan.setTrainInfoNew(JSONObject.toJSONString(infoNew));
            fahrplan.setTrainInfo(JSONObject.toJSONString(infomap));
            log.info("12306(补偿) - 根据车次编码获取时刻表信息 --> infoNew：{}",JSONObject.toJSONString(infoNew));
            log.info("12306(补偿)  - 根据车次编码获取时刻表信息 --> info：{}",JSONObject.toJSONString(infomap));
        }catch(Exception ex){
            log.error("解析错误或无法爬取到数据");
            ex.printStackTrace();
        }
    }
}
