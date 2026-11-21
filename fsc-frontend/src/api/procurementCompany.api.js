import spoApi from './spoAxios.js';

const BASE_PATH = '/api/procurement-companies';

// Get all procurement companies (paginated)
export async function getAllProcurementCompanies(page = 0, size = 10) {
    const response = await spoApi.get(BASE_PATH, {
        params: {
            page,
            size
        }
    });

    return response.data;
}

// Update procurement company details
export async function updateProcurementCompany(companyId, data) {
    const response = await spoApi.patch(
        `${BASE_PATH}/${encodeURIComponent(companyId)}`,
        data
    );

    return response.data;
}

// Approve procurement company
export async function approveProcurementCompany(companyId) {
    const response = await spoApi.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/review`,
        {
            decision: 'APPROVE'
        }
    );

    return response.data;
}

// Reject procurement company
export async function rejectProcurementCompany(companyId, rejectionReason) {
    const response = await spoApi.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/review`,
        {
            decision: 'REJECT',
            rejectionReason
        }
    );

    return response.data;
}

// Suspend procurement company
export async function suspendProcurementCompany(companyId) {
    const response = await spoApi.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/suspend`
    );

    return response.data;
}

// Reinstate procurement company
export async function reinstateProcurementCompany(companyId) {
    const response = await spoApi.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/reinstate`
    );

    return response.data;
}

// Register a new procurement company
export async function registerProcurementCompany(registrationData) {
    const response = await spoApi.post(
        `${BASE_PATH}/register`,
        registrationData
    );

    return response.data;
}

