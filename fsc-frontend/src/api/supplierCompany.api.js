import api from './axios.js';

const BASE_PATH = '/api/v1/slo/company';

export async function getSupplierCompany(companyId) {
    const response = await api.get(
        `${BASE_PATH}/${encodeURIComponent(companyId)}`
    );
    return response.data;
}

export async function startSupplierCompanyReview(companyId) {
    const response = await api.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/review`
    );
    return response.data;
}

export async function approveSupplierCompany(companyId) {
    const response = await api.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/approve`
    );
    return response.data;
}

export async function rejectSupplierCompany(companyId, rejectionReason) {
    const response = await api.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/reject`,
        { rejectionReason }
    );
    return response.data;
}

export async function suspendSupplierCompany(companyId) {
    const response = await api.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/suspend`
    );
    return response.data;
}

export async function deactivateSupplierCompany(companyId) {
    const response = await api.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/deactivate`
    );
    return response.data;
}

export async function reinstateSupplierCompany(companyId) {
    const response = await api.post(
        `${BASE_PATH}/${encodeURIComponent(companyId)}/reinstate`
    );
    return response.data;
}

// Register a new supplier company
export async function registerSupplierCompany(registrationData) {
    const response = await api.post(
        `${BASE_PATH}/register`,
        registrationData
    );

    return response.data;
}