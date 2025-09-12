package com.stock.survive.repository;

import com.stock.survive.entity.RealtimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RealtimeRepository extends JpaRepository<RealtimeEntity,Long> {

}
