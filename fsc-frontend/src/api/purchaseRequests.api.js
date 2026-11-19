import spoApi from './spoAxios.js';


export const createPurchaseRequest = async (data) => {
    const response = await spoApi.post('/api/v1/spo/purchase-requests', data);
    return response.data;
};

export const getPurchaseRequest = async (id) => {
    const response = await spoApi.get(`/api/v1/spo/purchase-requests/${id}`);
    return response.data;
};

export const updatePurchaseRequest = async (id, data) => {
    const response = await spoApi.patch(`/api/v1/spo/purchase-requests/${id}`, data);
    return response.data;
};

export const submitPurchaseRequest = async (id) => {
    const response = await spoApi.post(`/api/v1/spo/purchase-requests/${id}/submit`);
    return response.data;
};

export const getPurchaseRequestHistory = async (id) => {
    const response = await spoApi.get(`/api/v1/spo/purchase-requests/${id}/history`);
    return response.data;
};