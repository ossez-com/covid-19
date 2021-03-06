package com.ossez.covid19.service.batch.tasklet;

import com.google.inject.internal.cglib.core.$ProcessArrayCallback;
import com.ossez.covid19.common.Factory;
import com.ossez.covid19.common.dao.factories.Covid19Factory;
import com.ossez.covid19.common.models.Covid19Current;
import com.ossez.covid19.common.models.Covid19DailyUS;
import com.ossez.covid19.common.models.Covid19States;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 *
 */
public class Covid19Tasklet implements Tasklet {

    private final Logger logger = LoggerFactory.getLogger(Covid19Tasklet.class);
    private Resource directory;

    public Covid19Tasklet(Resource directory) {
        this.directory = directory;
    }


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        logger.info(">>>>>>>>");
        RestTemplate restTemplate = new RestTemplate();

        // GET CURRENT
        ResponseEntity<Covid19Current[]> responseEntity = restTemplate.exchange("https://covidtracking.com/api/v1/states/current.json",
                HttpMethod.GET,
                null,
                Covid19Current[].class);
        Covid19Current[] covid19Currents = responseEntity.getBody();

        logger.info("Current Data Size {}", covid19Currents.length);
//        Covid19Factory.get(1L);

        logger.info(">>>>>>>> {}", covid19Currents[1].getPositive());
        for (Covid19Current covid19Current : covid19Currents) {
            Covid19Current covid19CurrentDB = Covid19Factory.getByState(covid19Current);

            // NULL CHECK TO GET ID SET
            if (ObjectUtils.isNotEmpty(covid19CurrentDB)) {
                covid19Current.setId(covid19CurrentDB.getId());
            }

            Covid19Factory.save(covid19Current);
        }


        //GET STATES
        restTemplate = new RestTemplate();
        Covid19States[] Covid19StatesList = restTemplate.exchange("https://covidtracking.com/api/v1/states/daily.json",
                HttpMethod.GET,
                null,
                Covid19States[].class).getBody();

        for (Covid19States covid19States : Covid19StatesList) {
            Covid19States covid19StatesDB = Covid19Factory.getCovid19StatesByHash(covid19States.getHash());

            // NULL CHECK TO GET ID SET
            if (ObjectUtils.isEmpty(covid19StatesDB)) {
                covid19States.setUuid(UUID.randomUUID().toString());
                Covid19Factory.save(covid19States);
            }


        }
        processDailyUS("https://covidtracking.com/api/v1/us/daily.json");

        return RepeatStatus.FINISHED;
    }

    /**
     * @param apiURL
     */
    private void processDailyUS(String apiURL) {
        //GET STATES
        RestTemplate restTemplate = new RestTemplate();
        Covid19DailyUS[] Covid19DailyUSList = restTemplate.exchange(apiURL, HttpMethod.GET, null, Covid19DailyUS[].class).getBody();

        for (Covid19DailyUS covid19DailyUS : Covid19DailyUSList) {
            Covid19DailyUS covid19DailyUSDB = Covid19Factory.getCovid19DailyUSByHash(covid19DailyUS.getHash());

            // NULL CHECK TO GET ID SET
            if (ObjectUtils.isEmpty(covid19DailyUSDB)) {
                covid19DailyUS.setUuid(UUID.randomUUID().toString());
                Covid19Factory.save(covid19DailyUS);
            }


        }


    }
}
