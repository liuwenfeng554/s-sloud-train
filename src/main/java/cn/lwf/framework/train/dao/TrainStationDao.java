package cn.lwf.framework.train.dao;

import cn.lwf.framework.model.TrainStation;
import cn.lwf.framework.train.mapper.TrainStationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainStationDao {

    @Autowired
    TrainStationMapper trainStationMapper;

    public List<TrainStation> findStationList() {
        return trainStationMapper.findStationList();
    }

    public TrainStation findStationByName(String stationName) {
        return trainStationMapper.findStationByName(stationName);
    }

    public TrainStation findStationById(Long id) {
        return trainStationMapper.findStationById(id);
    }

    public int addStation(TrainStation station) {
        return trainStationMapper.insert(station);
    }

    public int updateStation(TrainStation station) {
        return trainStationMapper.update(station);
    }

}
