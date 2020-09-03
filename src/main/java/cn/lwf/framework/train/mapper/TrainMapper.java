package cn.lwf.framework.train.mapper;

import cn.lwf.framework.model.Train;
import cn.lwf.framework.model.TrainStation;

import java.util.List;

public interface TrainMapper {

    List<Train> findTrainList();

    Train findTrainByCode(String trainCode);

    int insert(Train train);

    int update(Train train);
}
