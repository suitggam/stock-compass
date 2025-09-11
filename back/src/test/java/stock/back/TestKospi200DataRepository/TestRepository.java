package stock.back.TestKospi200DataRepository;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import stock.back.entity.Kospi200DataEntity;
import stock.back.repository.Kospi200DataRepository;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@Log4j2
public class TestRepository {

    @Autowired
    Kospi200DataRepository kospi200DataRepository;

    @Test
    public void repoTest(){
        Assertions.assertNotNull(kospi200DataRepository);
        log.info(kospi200DataRepository.getClass().getName());
    }

    @Test
    public void getTest(){
        List<Kospi200DataEntity> list=kospi200DataRepository.findAll();
        log.info(list);
    }
    @Test
    public void getOneTest(){
        Long id=1L;
        Optional<Kospi200DataEntity> list=kospi200DataRepository.findById(id);

        Kospi200DataEntity kospi=list.orElseThrow();
        log.info(kospi.getCompanyName());
    }
}
