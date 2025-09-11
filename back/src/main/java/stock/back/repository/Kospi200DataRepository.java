package stock.back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.back.entity.Kospi200DataEntity;

import java.util.List;

@Repository
public interface Kospi200DataRepository extends JpaRepository<Kospi200DataEntity,Long> {

}
