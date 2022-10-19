package com.bpi.batch.tasklet;

import org.springframework.batch.core.StepContribution;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class Step_Two_Tasklet implements Tasklet, InitializingBean {

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		log.info(
				"=============== BEFORE " + chunkContext.getStepContext().getJobName() + " " + chunkContext.getStepContext().getStepName() + " ===================");

		
		log.info(
				"               execution " + chunkContext.getStepContext().getStepExecution().getJobExecutionId() + "                ");


		log.info(
				"=============== AFTER " + chunkContext.getStepContext().getJobName() + " " + chunkContext.getStepContext().getStepName() + " ===================");

		return RepeatStatus.FINISHED;
	}

}
