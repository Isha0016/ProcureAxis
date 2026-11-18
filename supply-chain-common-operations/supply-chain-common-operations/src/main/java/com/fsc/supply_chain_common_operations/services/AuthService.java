package com.fsc.supply_chain_common_operations.services;

import com.fsc.supply_chain_common_operations.dtos.requests.EmployeeLoginRequest;
import com.fsc.supply_chain_common_operations.dtos.responses.EmployeeLoginResponse;
import com.fsc.supply_chain_common_operations.exceptions.InvalidCredentialsException;
import com.fsc.supply_chain_common_operations.exceptions.UnAuthorizedException;
import com.fsc.supply_chain_common_operations.models.Employee;
import com.fsc.supply_chain_common_operations.models.Operation;
import com.fsc.supply_chain_common_operations.models.Role;
import com.fsc.supply_chain_common_operations.utilities.JwtUtility;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.UserStatus;
import com.fsc.supply_chain_common_operations.models.Company;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.jsonwebtoken.Claims;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final EmployeeService employeeService;
    private final JwtUtility jwtUtility;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(EmployeeService employeeService,
                       JwtUtility jwtUtility,
                       RoleService roleService,
                       PasswordEncoder passwordEncoder){
        this.employeeService = employeeService;
        this.jwtUtility = jwtUtility;
        this.roleService = roleService;
        this.passwordEncoder=passwordEncoder;
    }

    public EmployeeLoginResponse authenticateUser(EmployeeLoginRequest employeeLoginRequest){
        String email = employeeLoginRequest.getEmail();
        String password = employeeLoginRequest.getPassword();
        Employee employee = employeeService.getEmployeeByEmail(email);
        if (employee == null ||
                !passwordEncoder.matches(password, employee.getPasswordHash())) {
            throw new InvalidCredentialsException(
                    "Wrong email or password entered"
            );
        }
        if (!employee.isEmailVerified()

                || employee.getUserStatus() != UserStatus.ACTIVE) {

            throw new InvalidCredentialsException(

                    "Please activate your account before logging in"

            );

        }
        employee.setLastLoginAt(Instant.now());
        employeeService.saveOrUpdate(employee);
        String token = jwtUtility.generateJwtToken(employee);
        return new EmployeeLoginResponse(token);
    }

    public Employee isUserHavingAccess(
            String userToken,
            String operationName
    ) {
        Claims claims = jwtUtility.extractAllClaims(userToken);

        String userEmail = claims.get("email", String.class);

        Employee employee = employeeService.getEmployeeByEmail(userEmail);

        if (employee == null) {
            throw new UnAuthorizedException("Employee does not exist.");
        }

        // Check employee account status
        if (employee.getUserStatus() != UserStatus.ACTIVE) {
            throw new UnAuthorizedException("Employee account is not active.");
        }

        // Check company existence
        Company company = employee.getCompany();

        if (company == null) {
            throw new UnAuthorizedException("Employee is not associated with a company.");
        }

        // Check company type
        if (company.getCompanyType() != CompanyType.INTERNAL) {
            throw new UnAuthorizedException(
                    "Only internal employees can perform this operation."
            );
        }

        // Check company status
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new UnAuthorizedException("Internal company is not active.");
        }

        // Check assigned role permissions
        for (Role role : employee.getRoles()) {

            if (role == null) {
                continue;
            }

            if (isRoleCanPerformOperation(role, operationName)) {
                return employee;
            }
        }

        throw new UnAuthorizedException(
                "User does not have permission to perform this operation."
        );
    }

    private boolean isRoleCanPerformOperation(Role role, String operationName){
        List<Operation> operations = role.getOperations();
        for(Operation operation : operations){
            if(operation.getOperationName().equals(operationName)){
                return true;
            }
        }
        return false;
    }

}
