import api from './axios.js';

export const loginEmployee = async (email, password) => {
    const response = await api.post(
        '/api/v1/common/emp/login',
        {
            email,
            password
        }
    );

    const data = response.data;
    const token = data.token;

    if (!token) {
        throw new Error('Login response did not contain a token.');
    }

    localStorage.setItem('token', token);

    // Save login response for displaying the logged-in user's details.
    localStorage.setItem('loggedInUser', JSON.stringify(data));

    return data;
};

export const logoutEmployee = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('loggedInUser');

    window.location.href = '/';
};