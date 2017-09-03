package com.utopia.cloudmusicspider.repository;

/**
 * Created by utopia on 2017/8/30.
 * Copyright © 2017 utopia. All rights reserved.
 */

import com.utopia.cloudmusicspider.model.SongModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface SongModelRepository extends JpaRepository<SongModel, String>{
    List<SongModel> findByCommentCountGreaterThan(Long commentCount);
}
