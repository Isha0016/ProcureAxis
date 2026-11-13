package com.slo.supplier_lifecycle_operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan({
		"com.slo.supplier_lifecycle_operations",
		"com.fsc.supply_chain_common_operations"
})
@EntityScan(basePackages = {
		"com.slo.supplier_lifecycle_operations",
		"com.fsc.supply_chain_common_operations"
})
@EnableJpaRepositories(basePackages = {
		"com.slo.supplier_lifecycle_operations",
		"com.fsc.supply_chain_common_operations"
})
@EnableScheduling
public class SupplierLifecycleOperationsApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				SupplierLifecycleOperationsApplication.class,
				args
		);
	}
}
