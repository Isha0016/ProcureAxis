import api from './axios.js';
import spoApi from './spoAxios.js';

const BASE_PATH = '/api/v1/common/emp';

// Invite a new employee
export async function inviteEmployee(employeeData) {
    const response = await api.post(
        `${BASE_PATH}/invite-employee`,
        employeeData
    );

    return response.data;
}

// Assign or update an employee's manager
export async function assignEmployeeManager(employeeSysId, managerSysId) {
    const response = await api.patch(
        `${BASE_PATH}/${encodeURIComponent(employeeSysId)}/manager`,
        { managerSysId }
    );

    return response.data;
}

// Activate an employee account
// This endpoint is normally called from the employee activation flow.
export async function activateEmployee(token, newPassword) {
    const response = await api.post(
        `${BASE_PATH}/activate`,
        {
            token,
            newPassword
        }
    );

    return response.data;
}
// Procurement activation

export async function activateProcurementEmployee(token, newPassword) {

    const response = await spoApi.post(

        `${BASE_PATH}/activate`,

        { token, newPassword }

    );

    return response.data;

}