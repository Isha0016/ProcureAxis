import axios from 'axios';

const api = axios.create({
    baseURL: 'https://procureaxis-production.up.railway.app',
    headers: {
        'Content-Type': 'application/json'
    }
});

// Automatically attach JWT token to authenticated requests
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');

        if (token) {
            config.headers.token = token;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

export default api;