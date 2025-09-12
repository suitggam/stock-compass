package com.stock.survive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.stock.survive.entity.Kospi200DataEntity;

@Repository
public interface Kospi200DataRepository extends JpaRepository<Kospi200DataEntity,Long> {

}
