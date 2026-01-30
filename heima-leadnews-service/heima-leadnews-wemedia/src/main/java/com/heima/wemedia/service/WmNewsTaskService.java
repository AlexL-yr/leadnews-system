package com.heima.wemedia.service;

import java.util.Date;

public interface WmNewsTaskService {

    /**
     * add task into the delay queue
     * @param id article id
     * @param publishTime as the execution time of the task
     */
    public void addNewsToTask(Integer id, Date publishTime);

    public void scanNewsByTask();
}
