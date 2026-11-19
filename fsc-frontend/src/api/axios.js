import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8083',
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