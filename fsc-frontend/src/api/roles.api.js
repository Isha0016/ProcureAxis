import api from './axios.js';

export async function createRole(roleData) {
    const response = await api.post(
        '/api/v1/common/role/create',
        roleData
    );
    return response.data;
}