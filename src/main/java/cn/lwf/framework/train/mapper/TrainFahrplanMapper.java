package cn.lwf.framework.train.mapper;

import cn.lwf.framework.model.TrainFahrplan;

import java.util.List;

public interface TrainFahrplanMapper {

    List<TrainFahrplan> findFahrplanList();

    TrainFahrplan findFahrplanByCode(String trainCode);

    int insert(TrainFahrplan trainFahrplan);

    int update(TrainFahrplan trainFahrplan);
}
