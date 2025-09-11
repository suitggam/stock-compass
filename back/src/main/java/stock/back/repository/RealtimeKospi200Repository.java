package stock.back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.back.entity.Kospi200DataEntity;
import stock.back.entity.RealtimeKospi200Entity;

@Repository
public interface RealtimeKospi200Repository extends JpaRepository<RealtimeKospi200Entity,Long> {

}
