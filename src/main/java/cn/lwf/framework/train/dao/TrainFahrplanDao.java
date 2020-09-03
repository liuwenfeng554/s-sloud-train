package cn.lwf.framework.train.dao;

import cn.lwf.framework.model.TrainFahrplan;
import cn.lwf.framework.train.mapper.TrainFahrplanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainFahrplanDao {

    @Autowired
    TrainFahrplanMapper trainFahrplanMapper;

    public List<TrainFahrplan> findFahrplanList() {
        return trainFahrplanMapper.findFahrplanList();
    }

    public TrainFahrplan findFahrplanByCode(String trainCode) {
        return trainFahrplanMapper.findFahrplanByCode(trainCode);
    }

    public int addFahrplan(TrainFahrplan trainFahrplan) {
        return trainFahrplanMapper.insert(trainFahrplan);
    }

    public int updateFahrplan(TrainFahrplan trainFahrplan) {
        return trainFahrplanMapper.update(trainFahrplan);
    }

}
