CREATE TABLE account_activations (
                                     sys_id UUID PRIMARY KEY,
                                     employee_sys_id UUID NOT NULL,
                                     token_hash VARCHAR(64) NOT NULL UNIQUE,
                                     expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     consumed_at TIMESTAMP WITH TIME ZONE,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                     CONSTRAINT fk_account_activation_employee
                                         FOREIGN KEY (employee_sys_id)
                                             REFERENCES employees("sysId")
);

CREATE INDEX idx_account_activation_employee
    ON account_activations(employee_sys_id);