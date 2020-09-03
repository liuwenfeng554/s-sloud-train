package cn.lwf.framework.train.dao;

import cn.lwf.framework.model.Train;
import cn.lwf.framework.train.mapper.TrainMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainDao {

    @Autowired
    TrainMapper trainMapper;

    public List<Train> findTrainList() {
        return trainMapper.findTrainList();
    }

    public Train findTrainByCode(String trainCode) {
        return trainMapper.findTrainByCode(trainCode);
    }

    public int addTrain(Train train) {
        return trainMapper.insert(train);
    }

    public int updateTrain(Train train) {
        return trainMapper.update(train);
    }

}
