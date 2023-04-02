package com.productionpilot.db;

import com.productionpilot.Application;
import com.productionpilot.db.timescale.service.BatchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, properties = "spring.jpa.show-sql=true")
@Slf4j
public class DBTest {
    @Autowired
    private BatchService batchService;

    @Test
    public void test() {
        for(var batch : batchService.findAll()) {
            log.info("{}", batch);
            for(var parent : batchService.getFullPath(batch)) {
                log.info("  {}", parent);
            }
        }
    }
}
