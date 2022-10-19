package com.bpi.batch.jobs;

import java.util.Date;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import com.bpi.batch.tasklet.*;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableBatchProcessing
@Slf4j
public class ProcessJob {

	private static final String ISOLATION_REPEATABLE_READ = "ISOLATION_REPEATABLE_READ";

	@Autowired
	private DataSource dataSource;
	@Autowired
	private PlatformTransactionManager platformTransactionManager;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private SimpleJobLauncher jobLauncher;

	@Bean
	public SimpleJobOperator jobOperator(JobExplorer jobExplorer, JobRepository jobRepository,
			JobRegistry jobRegistry) {

		SimpleJobOperator jobOperator = new SimpleJobOperator();

		jobOperator.setJobExplorer(jobExplorer);
		jobOperator.setJobRepository(jobRepository);
		jobOperator.setJobRegistry(jobRegistry);
		jobOperator.setJobLauncher(jobLauncher);

		return jobOperator;
	}

	@Bean
	public JobRepository jobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(platformTransactionManager);
		factory.setValidateTransactionState(true);
		factory.setIsolationLevelForCreate(ISOLATION_REPEATABLE_READ);
		factory.setIncrementerFactory(customIncrementerFactory());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	private DataFieldMaxValueIncrementerFactory customIncrementerFactory() {
		return new CustomDataFieldMaxValueIncrementerFactory(dataSource);
	}

	private class CustomDataFieldMaxValueIncrementerFactory extends DefaultDataFieldMaxValueIncrementerFactory {

		CustomDataFieldMaxValueIncrementerFactory(DataSource dataSource) {
			super(dataSource);
		}

		@Override
		public DataFieldMaxValueIncrementer getIncrementer(String incrementerType, String incrementerName) {
			DataFieldMaxValueIncrementer incrementer = super.getIncrementer(incrementerType, incrementerName);
			if (incrementer instanceof H2SequenceMaxValueIncrementer) {
				((H2SequenceMaxValueIncrementer) incrementer).setPaddingLength(20); // .setCacheSize(20);
			}
			return incrementer;
		}
	}

	// @Scheduled(fixedDelay =10000)
	// @Scheduled(fixedRate=10000)
	@Scheduled(cron = "*/10 * * * * ?")
	// @Scheduled(fixedDelayString = "${spring.batch.fixedDelay}")
	// @Scheduled(fixedRateString = "${spring.batch.fixedRate}")
	public void perform() throws Exception {

		log.info("===================================================================");
		log.info("===================================================================");
		log.info("=============== Job Started at :" + new Date() + " ===============");
		log.info("===================================================================");
		log.info("");
		JobParameters param = new JobParametersBuilder().addString("JobID", String.valueOf(System.currentTimeMillis()))
				.toJobParameters();

		JobExecution execution = jobLauncher.run(alertProcess(), param);

		log.info("");
		log.info("===================================================================");
		log.info("=============== Job finished with status :" + execution.getStatus() + " ===============");
		log.info("===================================================================");
		log.info("===================================================================");
	}

	@Bean
	public Job alertProcess() {
		return jobBuilderFactory.get("AlertProcess").incrementer(new RunIdIncrementer()).flow(step1()).next(step2())
				.next(step3()).end().build();
	}

	// Steps Builder
	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").tasklet(Step_One_Tasklet()).build();
	}

	@Bean
	public Step step2() {
		return stepBuilderFactory.get("step2").tasklet(Step_Two_Tasklet()).build();
	}

	@Bean
	public Step step3() {
		return stepBuilderFactory.get("step3").tasklet(Step_Three_Tasklet()).build();
	}

	// Tasklets Instances
	@Bean
	public Step_One_Tasklet Step_One_Tasklet() {
		Step_One_Tasklet taskletOne = new Step_One_Tasklet();

		return taskletOne;
	}

	@Bean
	public Step_Two_Tasklet Step_Two_Tasklet() {
		Step_Two_Tasklet taskletTwo = new Step_Two_Tasklet();

		return taskletTwo;
	}

	@Bean
	public Step_Three_Tasklet Step_Three_Tasklet() {
		Step_Three_Tasklet taskletThree = new Step_Three_Tasklet();

		return taskletThree;
	}

	@Bean
	public SimpleJobLauncher jobLauncher(JobRepository jobRepository) {
		SimpleJobLauncher launcher = new SimpleJobLauncher();
		launcher.setJobRepository(jobRepository);
		return launcher;
	}
}
