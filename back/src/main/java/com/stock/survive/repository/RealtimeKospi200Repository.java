package com.stock.survive.repository;

import com.stock.survive.entity.RealtimeKospi200Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RealtimeKospi200Repository extends JpaRepository<RealtimeKospi200Entity,Long> {

}
