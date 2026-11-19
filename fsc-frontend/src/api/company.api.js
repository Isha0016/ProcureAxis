import api from './axios.js';

// Extract the supplier company ID from the logged-in user's JWT
const getCompanyId = () => {
    const token = localStorage.getItem('token');

    if (!token) {
        throw new Error('Authentication token not found. Please log in again.');
    }

    const payload = JSON.parse(
        atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
    );

    const companyId = payload.companyId;

    if (!companyId) {
        throw new Error('Company ID not found in authentication token.');
    }

    return companyId;
};

// Fetch logged-in supplier company profile
export const getCompanyProfile = async () => {
    const companyId = getCompanyId();

    const response = await api.get(
        `/api/v1/slo/company/${companyId}`
    );

    return response.data;
};

// Update logged-in supplier company profile
export const updateCompanyProfile = async (profileData) => {
    const companyId = getCompanyId();

    const response = await api.patch(
        `/api/v1/slo/company/${companyId}`,
        profileData
    );

    return response.data;
};