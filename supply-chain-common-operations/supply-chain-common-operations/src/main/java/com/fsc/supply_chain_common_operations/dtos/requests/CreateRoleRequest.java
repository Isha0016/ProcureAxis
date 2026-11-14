package com.fsc.supply_chain_common_operations.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    private UUID companySysId;
    private String roleName;
    private List<String> operationName;
}
