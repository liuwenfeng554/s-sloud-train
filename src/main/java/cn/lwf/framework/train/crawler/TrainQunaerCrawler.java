package cn.lwf.framework.train.crawler;

import cn.lwf.framework.constant.Constants;
import cn.lwf.framework.model.TrainFahrplan;
import cn.lwf.framework.util.HttpClientUtils;
import cn.lwf.framework.vo.Node;
import cn.lwf.framework.vo.TrainInfoNew;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  去哪儿官网获取车次时刻表信息
 */
@Slf4j
@Component
public class TrainQunaerCrawler {

    public  TrainFahrplan getQunarTrainInfo(TrainFahrplan trainFahrplan, String trainNum, String departure, String arrive, String date){
        try{
            StringBuffer uri = new StringBuffer(Constants.TRAIN_QUNAR_QUERY_TSEATDETAIL);
            uri.append("?dptStation=").append(departure);
            uri.append("&arrStation=").append(arrive);
            uri.append("&date=").append(date);
            uri.append("&trainNo=").append(trainNum);
            uri.append("&user=").append("neibu");
            uri.append("&source=").append("www");
            uri.append("&needTimeDetail=").append("true");
            log.info("去哪儿官网获取时刻表请求地址：{}", uri.toString());
            String jsonStr = "{}";
            //根据车次编码获取时刻表信息
            String resultString = HttpClientUtils.httpPost(uri.toString(),jsonStr);
            if(StringUtils.isNotBlank(resultString)) {
                JSONObject result = JSON.parseObject(resultString);
                if (!result.isEmpty() && (result.getIntValue("httpstatus") == 200 && result.containsKey("data"))) {
                    JSONObject dataJsonObject = result.getJSONObject("data");
                    if(dataJsonObject.containsKey("stationItemList")){
                        String typeName = dataJsonObject.getString("typeName");
                        JSONArray array = dataJsonObject.getJSONArray("stationItemList");
                        int size = array.size();
                        TrainInfoNew infoNew = new TrainInfoNew();
                        infoNew.setTrainCode(trainNum);
                        infoNew.setTrainLength(String.valueOf(size));
                        ArrayList<Node> nodeList = new ArrayList<Node>();
                        infoNew.setItems(nodeList);
                        String stationName = "";
                        Map<String,String> infomap = new HashMap<String,String>();
                        infomap.put("trainClassName", typeName);
                        int index=1;
                        Iterator iterator = array.iterator();
                        while (iterator.hasNext()) {
                            JSONObject jsonObject = (JSONObject) iterator.next();
                            stationName = jsonObject.getString("stationName");
                            String startTime = jsonObject.getString("startTime");
                            String arriveTime = jsonObject.getString("arriveTime");
                            Integer overTime = jsonObject.getInteger("overTime");
                            Node node = new Node();
                            if(index == 1){
                                infoNew.setStartStationName(stationName);
                                node.setArriveTime("----");
                                node.setOverTime("----");
                                node.setStartTime(startTime);
                                infomap.put("beginStation", stationName);
                                infomap.put("beginTime", startTime);
                                infomap.put("trainCode", trainNum);
                            }else{
                                //处理arriveTime
                                node.setArriveTime(arriveTime);
                                node.setStartTime(startTime);
                                node.setOverTime(overTime+"分钟");
                            }
                            if(index < 10){
                                node.setStationNo("0"+index);
                            }else{
                                node.setStationNo(String.valueOf(index));
                            }
                            node.setStationName(stationName);
                            nodeList.add(node);
                            if(index == size){
                                node.setOverTime("----");
                                node.setStartTime(arriveTime);
                                infoNew.setEndStationName(stationName);
                                infomap.put("endStation", stationName);
                            }
                            infomap.put("no_"+String.valueOf(index-1)+"_stationName", stationName);
                            if(index<10){
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
                                infomap.put("no_"+String.valueOf(index-1)+"_lTime", overTime+"分钟");
                                infomap.put("no_"+String.valueOf(index-1)+"_sTime", startTime);
                            }
                            if(index == size){
                                infomap.put("no_"+String.valueOf(index-1)+"_lTime", "----");
                                infomap.put("no_"+String.valueOf(index-1)+"_sTime", arriveTime);
                            }
                            index++;
                        }
                        trainFahrplan.setTrainInfoNew(JSONObject.toJSONString(infoNew));
                        trainFahrplan.setTrainInfo(JSONObject.toJSONString(infomap));

                        log.info("qunaer - 根据车次编码获取时刻表信息 --> infoNew：{}",JSONObject.toJSONString(infoNew));
                        log.info("qunaer - 根据车次编码获取时刻表信息 --> info：{}",JSONObject.toJSONString(infomap));
                    }
                }
            }
        }catch(Exception ex){
            log.error("解析错误或无法爬取到数据");
            ex.printStackTrace();
        }

        return trainFahrplan;
    }
}
