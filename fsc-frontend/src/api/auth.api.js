import api from './axios.js';

export const loginEmployee = async (email, password) => {
    const response = await api.post(
        '/api/v1/common/emp/login',
        {
            email,
            password
        }
    );

    const token = response.data.token;

    if (!token) {
        throw new Error('Login response did not contain a token.');
    }

    localStorage.setItem('token', token);

    return response.data;
};