package com.infotienda.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        relaxShippingAddressNullability();
    }

    private void relaxShippingAddressNullability() {
        String isNullable = jdbcTemplate.queryForObject("""
                select is_nullable
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'orders'
                  and column_name = 'shipping_address_id'
                """, String.class);

        if (!"NO".equalsIgnoreCase(isNullable)) {
            return;
        }

        log.info("Updating orders.shipping_address_id to allow null values for pickup orders");
        jdbcTemplate.execute("alter table orders modify column shipping_address_id bigint null");
    }
}
