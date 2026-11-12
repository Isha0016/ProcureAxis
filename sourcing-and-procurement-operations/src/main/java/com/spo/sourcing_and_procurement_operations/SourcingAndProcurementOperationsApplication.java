package com.spo.sourcing_and_procurement_operations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(
		scanBasePackages = {
				"com.spo.sourcing_and_procurement_operations",
				"com.fsc.supply_chain_common_operations"
		}
)
@EntityScan(basePackages = {
		"com.spo.sourcing_and_procurement_operations",
		"com.fsc.supply_chain_common_operations"
})
@EnableJpaRepositories(basePackages = {
		"com.spo.sourcing_and_procurement_operations.repositories",
		"com.fsc.supply_chain_common_operations.repositories"
})
public class SourcingAndProcurementOperationsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SourcingAndProcurementOperationsApplication.class, args);
	}

}
